package com.yuvraj.openchatai

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yuvraj.openchatai.data.network.OpenAIService
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies base-URL normalization on-device, including local/cleartext HTTP
 * endpoints and gateways that carry the API key in the URL path.
 */
@RunWith(AndroidJUnit4::class)
class BaseUrlTest {

    private val service = OpenAIService()

    @Test
    fun keepsCleartextHttpUrl() {
        assertEquals(
            "http://91.240.175.30:8000/v1/6e77d40a33a84705abb20af545164fad",
            service.normalizeBaseUrl("http://91.240.175.30:8000/v1/6e77d40a33a84705abb20af545164fad/"),
        )
    }

    @Test
    fun keepsLocalhostUrl() {
        assertEquals("http://10.0.2.2:11434/v1", service.normalizeBaseUrl("http://10.0.2.2:11434/v1"))
    }

    @Test
    fun prefixesHttpsWhenSchemeMissing() {
        assertEquals("https://api.openai.com/v1", service.normalizeBaseUrl("api.openai.com/v1"))
    }

    @Test
    fun keepsExistingHttpsScheme() {
        assertEquals("https://openrouter.ai/api/v1", service.normalizeBaseUrl("https://openrouter.ai/api/v1"))
    }

    @Test
    fun stripsTrailingSlashes() {
        assertEquals("https://api.deepseek.com/v1", service.normalizeBaseUrl("https://api.deepseek.com/v1///"))
    }
}
