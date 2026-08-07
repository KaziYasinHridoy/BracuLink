package com.braculink;

import com.braculink.service.CourseSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves {@code /v3/api-docs} — the spec Swagger UI renders — actually generates, rather than
 * assuming the {@code @Tag}/{@code @Operation} annotations are wired correctly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiGenerationTest {

    @MockitoBean
    private CourseSyncService courseSyncService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void theFullSpecGeneratesAndCoversEveryController() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                // Public: no bearer token required.
                .andExpect(jsonPath("$.paths./api/auth/login.post.security").isEmpty())
                // Protected: falls back to the global bearerAuth requirement.
                .andExpect(jsonPath("$.paths./api/notifications.get").exists())
                .andExpect(jsonPath("$.paths./api/swap-requests/{id}/suggestions.get").exists())
                .andExpect(jsonPath("$.paths./api/swap-groups/propose.post").exists())
                .andExpect(jsonPath("$.paths./api/friends/request.post").exists())
                .andExpect(jsonPath("$.paths./api/notifications/{id}/read.post").exists());
    }
}
