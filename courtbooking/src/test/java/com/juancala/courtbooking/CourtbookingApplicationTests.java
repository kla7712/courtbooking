package com.juancala.courtbooking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CourtbookingApplicationTests {

    /** Comprueba que la aplicación arranca: migraciones, entidades y configuración. */
    @Test
    void contextLoads() {
    }
}
