package com.juancala.courtbooking.booking;

import com.juancala.courtbooking.block.CourtBlock;
import com.juancala.courtbooking.block.CourtBlockRepository;
import com.juancala.courtbooking.booking.dto.AvailabilityResponse;
import com.juancala.courtbooking.booking.dto.BookingResponse;
import com.juancala.courtbooking.booking.dto.CreateBookingRequest;
import com.juancala.courtbooking.booking.dto.SlotOption;
import com.juancala.courtbooking.booking.dto.SlotResponse;
import com.juancala.courtbooking.common.BadRequestException;
import com.juancala.courtbooking.common.ConflictException;
import com.juancala.courtbooking.common.NotFoundException;
import com.juancala.courtbooking.court.Court;
import com.juancala.courtbooking.court.CourtRepository;
import com.juancala.courtbooking.pricing.PriceRule;
import com.juancala.courtbooking.pricing.PricingService;
import com.juancala.courtbooking.user.UserRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookingService {

    private static final String SLOT_TAKEN = "Ese horario ya está reservado";

    private final BookingRepository bookingRepository;
    private final CourtRepository courtRepository;
    private final CourtBlockRepository courtBlockRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;
    private final BookingRules rules;

    public BookingService(BookingRepository bookingRepository,
                          CourtRepository courtRepository,
                          CourtBlockRepository courtBlockRepository,
                          UserRepository userRepository,
                          PricingService pricingService,
                          BookingRules rules) {
        this.bookingRepository = bookingRepository;
        this.courtRepository = courtRepository;
        this.courtBlockRepository = courtBlockRepository;
        this.userRepository = userRepository;
        this.pricingService = pricingService;
        this.rules = rules;
    }

    @Transactional
    public BookingResponse create(Long userId, CreateBookingRequest request) {
        Court court = getActiveCourt(request.courtId());
        validateSlot(court, request.startTime(), request.durationMinutes());

        Instant now = Instant.now();
        Instant start = toInstant(request.date(), request.startTime());
        Instant end = start.plus(Duration.ofMinutes(request.durationMinutes()));

        if (!start.isAfter(now)) {
            throw new BadRequestException("No se puede reservar en el pasado");
        }
        LocalDate lastBookableDay = LocalDate.now(rules.getZone()).plusDays(rules.getMaxDaysInAdvance());
        if (request.date().isAfter(lastBookableDay)) {
            throw new BadRequestException(
                    "Solo se puede reservar con " + rules.getMaxDaysInAdvance() + " días de antelación como máximo");
        }

        long activeBookings = bookingRepository
                .countByUserIdAndStatusAndEndTimeAfter(userId, BookingStatus.CONFIRMED, now);
        if (activeBookings >= rules.getMaxActiveBookings()) {
            throw new ConflictException(
                    "Has alcanzado el máximo de " + rules.getMaxActiveBookings() + " reservas activas");
        }

        List<CourtBlock> blocks = courtBlockRepository.findOverlapping(court.getId(), start, end);
        if (!blocks.isEmpty()) {
            throw new ConflictException("La pista está bloqueada en ese horario: " + blocks.get(0).getReason());
        }

        // Comprobación previa para dar un error claro en el caso normal...
        if (!bookingRepository.findOverlapping(court.getId(), BookingStatus.CONFIRMED, start, end).isEmpty()) {
            throw new ConflictException(SLOT_TAKEN);
        }

        BigDecimal price = pricingService.calculate(
                court, request.date(), request.startTime(), request.durationMinutes());

        // getReferenceById no consulta la base de datos: basta el id para la clave foránea
        Booking booking = new Booking(userRepository.getReferenceById(userId), court, start, end, price);
        try {
            // ...pero si dos peticiones llegan a la vez, ambas pasan esa comprobación.
            // Ahí quien decide es la restricción de exclusión de PostgreSQL.
            bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(SLOT_TAKEN);
        }
        return BookingResponse.from(booking, rules.getZone());
    }

    public List<BookingResponse> findByUser(Long userId) {
        return bookingRepository.findByUserIdOrderByStartTimeDesc(userId).stream()
                .map(booking -> BookingResponse.from(booking, rules.getZone()))
                .toList();
    }

    @Transactional
    public void cancel(Long bookingId, Long userId, boolean isAdmin) {
        // Si la reserva es de otro usuario se responde 404, para no revelar que existe
        Booking booking = bookingRepository.findById(bookingId)
                .filter(b -> isAdmin || b.getUser().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("No existe la reserva con id " + bookingId));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException("La reserva ya está cancelada");
        }
        Instant now = Instant.now();
        if (!booking.getStartTime().isAfter(now)) {
            throw new BadRequestException("No se puede cancelar una reserva que ya ha empezado");
        }
        Instant cancellationDeadline = booking.getStartTime().minus(Duration.ofHours(rules.getCancellationHours()));
        if (!isAdmin && now.isAfter(cancellationDeadline)) {
            throw new BadRequestException(
                    "Solo se puede cancelar con al menos " + rules.getCancellationHours() + " horas de antelación");
        }
        booking.cancel();
    }

    /** Horas de inicio libres de una pista en un día, con las duraciones que caben y su precio. */
    public AvailabilityResponse getAvailability(Long courtId, LocalDate date) {
        Court court = getActiveCourt(courtId);
        List<SlotResponse> slots = new ArrayList<>();

        LocalDate today = LocalDate.now(rules.getZone());
        boolean bookableDay = !date.isBefore(today) && !date.isAfter(today.plusDays(rules.getMaxDaysInAdvance()));
        if (bookableDay) {
            Instant now = Instant.now();
            Instant dayStart = date.atStartOfDay(rules.getZone()).toInstant();
            Instant dayEnd = date.plusDays(1).atStartOfDay(rules.getZone()).toInstant();

            // Todo lo que ocupa la pista ese día: reservas confirmadas y bloqueos
            List<Interval> busy = new ArrayList<>();
            bookingRepository.findOverlapping(courtId, BookingStatus.CONFIRMED, dayStart, dayEnd)
                    .forEach(b -> busy.add(new Interval(b.getStartTime(), b.getEndTime())));
            courtBlockRepository.findOverlapping(courtId, dayStart, dayEnd)
                    .forEach(cb -> busy.add(new Interval(cb.getStartTime(), cb.getEndTime())));

            // Las tarifas se cargan una sola vez para todo el día
            List<PriceRule> priceRules = pricingService.loadRules(court, date);

            int openingMinute = minuteOfDay(rules.getOpeningTime());
            int lastMinute = lastBookableMinute(court);
            for (int minute = openingMinute; minute < lastMinute; minute += BookingRules.SLOT_STEP_MINUTES) {
                LocalTime slotTime = LocalTime.ofSecondOfDay(minute * 60L);
                Instant slotStart = toInstant(date, slotTime);
                if (!slotStart.isAfter(now)) {
                    continue;
                }
                List<SlotOption> options = new ArrayList<>();
                for (int duration : BookingRules.ALLOWED_DURATIONS) {
                    if (minute + duration > lastMinute) {
                        continue;
                    }
                    Instant slotEnd = slotStart.plus(Duration.ofMinutes(duration));
                    boolean free = busy.stream().noneMatch(interval -> interval.overlaps(slotStart, slotEnd));
                    if (free) {
                        options.add(new SlotOption(duration,
                                pricingService.calculate(priceRules, court, slotTime, duration)));
                    }
                }
                if (!options.isEmpty()) {
                    slots.add(new SlotResponse(slotTime, options));
                }
            }
        }
        return new AvailabilityResponse(court.getId(), court.getName(), date, slots);
    }

    private void validateSlot(Court court, LocalTime startTime, int durationMinutes) {
        if (!BookingRules.ALLOWED_DURATIONS.contains(durationMinutes)) {
            throw new BadRequestException("La duración debe ser de 60 o 90 minutos");
        }
        boolean onTheGrid = startTime.getMinute() % BookingRules.SLOT_STEP_MINUTES == 0
                && startTime.getSecond() == 0
                && startTime.getNano() == 0;
        if (!onTheGrid) {
            throw new BadRequestException("La reserva debe empezar en punto o a y media");
        }
        int startMinute = minuteOfDay(startTime);
        int endMinute = startMinute + durationMinutes;
        boolean withinOpeningHours = startMinute >= minuteOfDay(rules.getOpeningTime())
                && endMinute <= minuteOfDay(rules.getClosingTime());
        if (!withinOpeningHours) {
            throw new BadRequestException("El horario del club es de "
                    + rules.getOpeningTime() + " a " + rules.getClosingTime());
        }
        if (endMinute > lastBookableMinute(court)) {
            throw new BadRequestException("Esta pista no tiene iluminación: solo se puede reservar hasta las "
                    + rules.getLightingFrom());
        }
    }

    /** Las pistas sin luz dejan de poder usarse cuando empieza el horario con iluminación. */
    private int lastBookableMinute(Court court) {
        int closingMinute = minuteOfDay(rules.getClosingTime());
        return court.isHasLighting()
                ? closingMinute
                : Math.min(closingMinute, minuteOfDay(rules.getLightingFrom()));
    }

    private Court getActiveCourt(Long courtId) {
        return courtRepository.findById(courtId)
                .filter(Court::isActive)
                .orElseThrow(() -> new NotFoundException("No existe la pista con id " + courtId));
    }

    private Instant toInstant(LocalDate date, LocalTime time) {
        return ZonedDateTime.of(date, time, rules.getZone()).toInstant();
    }

    private int minuteOfDay(LocalTime time) {
        return time.toSecondOfDay() / 60;
    }

    private record Interval(Instant start, Instant end) {

        boolean overlaps(Instant otherStart, Instant otherEnd) {
            return start.isBefore(otherEnd) && end.isAfter(otherStart);
        }
    }
}
