package com.videsol.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "pilot.api.username=test_user",
        "pilot.api.password=test_pass"
})
class VidesolBackendApplicationTests {

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring se cargue sin errores
    }
}
