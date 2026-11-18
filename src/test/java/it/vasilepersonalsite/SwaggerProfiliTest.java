package it.vasilepersonalsite;

import it.vasilepersonalsite.service.EmailService;
import it.vasilepersonalsite.service.GoogleMeetService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Springdoc: spento con la configurazione di produzione, acceso con il profilo dev.
 * test.properties non tocca le proprietà springdoc, quindi vale ciò che dicono
 * application.properties / application-dev.properties.
 */
class SwaggerProfiliTest {

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @TestPropertySource(locations = "classpath:test.properties")
    class Produzione {
        @Autowired TestRestTemplate http;
        @MockBean EmailService emailService;
        @MockBean GoogleMeetService googleMeetService;

        @Test
        void apiDocsESwaggerNonDisponibili() {
            assertNotEquals(200, http.getForEntity("/v3/api-docs", String.class).getStatusCode().value());
            assertNotEquals(200, http.getForEntity("/swagger-ui/index.html", String.class).getStatusCode().value());
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("dev")
    @TestPropertySource(locations = "classpath:test.properties", properties = "server.address=127.0.0.1")
    class Sviluppo {
        @Autowired TestRestTemplate http;
        @MockBean EmailService emailService;
        @MockBean GoogleMeetService googleMeetService;

        @Test
        void apiDocsDisponibiliConSchemaBearer() {
            var r = http.getForEntity("/v3/api-docs", String.class);
            assertEquals(200, r.getStatusCode().value());
            assertEquals(true, r.getBody().contains("bearerAuth"));
            assertEquals(200, http.getForEntity("/swagger-ui/index.html", String.class).getStatusCode().value());
        }
    }
}
