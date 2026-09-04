package com.herdcommand.api.i18n;

import com.herdcommand.api.api.error.ErrorCodes;
import com.herdcommand.api.support.TestJwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
class MessageResolutionTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void englishAcceptLanguageReturnsEnglishMessage() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "en"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED))
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void arabicAcceptLanguageReturnsArabicMessage() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "ar"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED))
                .andExpect(jsonPath("$.message").value("يلزم تسجيل الدخول للمتابعة."));
    }

    @Test
    void unsupportedLanguageFallsBackToArabic() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "fr"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("يلزم تسجيل الدخول للمتابعة."));
    }
}
