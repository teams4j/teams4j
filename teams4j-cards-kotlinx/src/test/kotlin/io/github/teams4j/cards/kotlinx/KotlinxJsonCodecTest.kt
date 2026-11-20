package io.github.teams4j.cards.kotlinx

import io.github.teams4j.cards.CardValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.math.BigDecimal

class KotlinxJsonCodecTest {
    private val codec = KotlinxJsonCodec()

    @Test
    fun aNumberAtTheLimitIsRead() {
        val digits = "7".repeat(1000)

        assertThat(codec.read("""{"x":$digits}"""))
            .isEqualTo(CardValue.`object`(mapOf("x" to CardValue.of(BigDecimal(digits)))))
    }

    /** kotlinx.serialization before 1.13.0 read these as [1,2] (Kotlin/kotlinx.serialization#3289). */
    @Test
    fun aValueAfterAnArrayIsRefused() {
        for (json in listOf("[1]2]", """{"a":[1]2]}""", """["x"]"y"]""")) {
            assertThatThrownBy { codec.read(json) }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    /** An activity is parsed before its token is checked, and BigDecimal(String) is quadratic in the length. */
    @Test
    @Timeout(5)
    fun aLongerNumberIsRefusedBeforeItIsConverted() {
        assertThatThrownBy { codec.read("""{"x":${"7".repeat(400_000)}}""") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
