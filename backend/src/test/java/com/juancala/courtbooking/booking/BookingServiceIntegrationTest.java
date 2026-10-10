package com.juancala.courtbooking.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.juancala.courtbooking.TestcontainersConfiguration;
import com.juancala.courtbooking.booking.dto.BookingResponse;
import com.juancala.courtbooking.booking.dto.CreateBookingRequest;
import com.juancala.courtbooking.common.BadRequestException;
import com.juancala.courtbooking.common.ConflictException;
import com.juancala.courtbooking.court.Court;
import com.juancala.courtbooking.court.CourtRepository;
import com.juancala.courtbooking.pricing.PricingService;
import com.juancala.courtbooking.user.Role;
import com.juancala.courtbooking.user.User;
import com.juancala.courtbooking.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Tests de integración contra un PostgreSQL real (Testcontainers).
 * Usan las pistas y tarifas que crean las migraciones:
 * Pista 1 = tierra batida con luz, Pista 2 = tierra batida sin luz.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BookingServiceIntegrationTest {

    @Autowired
    private BookingService bookingService;
    @Autowired
    private PricingService pricingService;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private CourtRepository courtRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookingRules rules;

    private Court litClayCourt;
    private Court unlitClayCourt;
    private LocalDate bookingDate;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        litClayCourt = findCourt("Pista 1");
        unlitClayCourt = findCourt("Pista 2");
        // Dentro del plazo de reserva y siempre en el futuro
        bookingDate = LocalDate.now(rules.getZone()).plusDays(3);
    }

    @Test
    void onlyOneOfManySimultaneousBookingsForTheSameSlotSucceeds() throws Exception {
        int attempts = 10;
        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            userIds.add(createUser());
        }
        CreateBookingRequest request =
                new CreateBookingRequest(litClayCourt.getId(), bookingDate, LocalTime.of(10, 0), 60);

        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch allReady = new CountDownLatch(attempts);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (Long userId : userIds) {
                results.add(executor.submit(() -> {
                    allReady.countDown();
                    go.await();   // todos los hilos esperan aquí y salen a la vez
                    try {
                        bookingService.create(userId, request);
                        return true;
                    } catch (ConflictException ex) {
                        return false;
                    }
                }));
            }
            allReady.await();
            go.countDown();

            int successes = 0;
            for (Future<Boolean> result : results) {
                if (result.get(30, TimeUnit.SECONDS)) {
                    successes++;
                }
            }

            assertThat(successes).isEqualTo(1);
            assertThat(bookingRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void overlappingBookingIsRejected() {
        bookingService.create(createUser(),
                new CreateBookingRequest(litClayCourt.getId(), bookingDate, LocalTime.of(10, 0), 90));

        CreateBookingRequest overlapping =
                new CreateBookingRequest(litClayCourt.getId(), bookingDate, LocalTime.of(11, 0), 60);

        assertThatThrownBy(() -> bookingService.create(createUser(), overlapping))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void bookingCanStartExactlyWhenThePreviousOneEnds() {
        bookingService.create(createUser(),
                new CreateBookingRequest(litClayCourt.getId(), bookingDate, LocalTime.of(10, 0), 60));

        BookingResponse next = bookingService.create(createUser(),
                new CreateBookingRequest(litClayCourt.getId(), bookingDate, LocalTime.of(11, 0), 60));

        assertThat(next.startTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(bookingRepository.count()).isEqualTo(2);
    }

    @Test
    void cancelledBookingFreesTheSlot() {
        Long owner = createUser();
        CreateBookingRequest request =
                new CreateBookingRequest(litClayCourt.getId(), bookingDate, LocalTime.of(10, 0), 60);
        BookingResponse first = bookingService.create(owner, request);

        bookingService.cancel(first.id(), owner, false);
        BookingResponse second = bookingService.create(createUser(), request);

        assertThat(second.status()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void courtWithoutLightingCannotBeBookedAfterDark() {
        // Acabaría a las 19:30, pasado el inicio del horario con luz (19:00)
        CreateBookingRequest request =
                new CreateBookingRequest(unlitClayCourt.getId(), bookingDate, LocalTime.of(18, 30), 60);

        assertThatThrownBy(() -> bookingService.create(createUser(), request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void userCannotExceedTheMaximumNumberOfActiveBookings() {
        Long userId = createUser();
        for (int i = 0; i < rules.getMaxActiveBookings(); i++) {
            bookingService.create(userId, new CreateBookingRequest(
                    litClayCourt.getId(), bookingDate, LocalTime.of(9 + i, 0), 60));
        }

        CreateBookingRequest oneTooMany =
                new CreateBookingRequest(litClayCourt.getId(), bookingDate, LocalTime.of(15, 0), 60);

        assertThatThrownBy(() -> bookingService.create(userId, oneTooMany))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void priceCombinesPeakRateAndLightingSupplement() {
        LocalDate friday = LocalDate.of(2026, 10, 9);

        // 18:00-19:00 en punta (16 €/h) + 19:00-19:30 en punta con luz (16 + 3 €/h, media hora)
        BigDecimal price = pricingService.calculate(litClayCourt, friday, LocalTime.of(18, 0), 90);

        assertThat(price).isEqualByComparingTo("25.50");
    }

    @Test
    void priceUsesOffPeakRateOnWeekdayMornings() {
        LocalDate friday = LocalDate.of(2026, 10, 9);

        BigDecimal price = pricingService.calculate(litClayCourt, friday, LocalTime.of(10, 0), 60);

        assertThat(price).isEqualByComparingTo("12.00");
    }

    @Test
    void priceUsesWeekendRate() {
        LocalDate saturday = LocalDate.of(2026, 10, 10);

        BigDecimal price = pricingService.calculate(litClayCourt, saturday, LocalTime.of(10, 0), 60);

        assertThat(price).isEqualByComparingTo("16.00");
    }

    @Test
    void priceRuleBoundariesAreNotShiftedByTimeZone() {
        LocalDate friday = LocalDate.of(2026, 10, 9);

        // Primera hora del día (valle) y primera hora de punta: detectan cualquier desfase horario
        assertThat(pricingService.calculate(litClayCourt, friday, LocalTime.of(8, 0), 60))
                .isEqualByComparingTo("12.00");
        assertThat(pricingService.calculate(litClayCourt, friday, LocalTime.of(17, 0), 60))
                .isEqualByComparingTo("16.00");
    }

    @Test
    void bookingTimesSurviveARoundTripThroughTheDatabase() {
        Long userId = createUser();
        bookingService.create(userId,
                new CreateBookingRequest(litClayCourt.getId(), bookingDate, LocalTime.of(10, 0), 60));

        // findByUser vuelve a leer la reserva de la base de datos
        BookingResponse stored = bookingService.findByUser(userId).get(0);

        assertThat(stored.startTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(stored.endTime()).isEqualTo(LocalTime.of(11, 0));
    }

    private Court findCourt(String name) {
        return courtRepository.findAll().stream()
                .filter(court -> court.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Falta la pista de ejemplo: " + name));
    }

    /** Crea un socio con email único y devuelve su id. */
    private Long createUser() {
        String email = "test-" + UUID.randomUUID() + "@example.com";
        return userRepository.save(new User("Test", email, "sin-uso", Role.MEMBER)).getId();
    }
}