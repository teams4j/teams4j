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

    /** An activity is parsed before its token is checked, and BigDecimal(String) is quadratic in the length. */
    @Test
    @Timeout(5)
    fun aLongerNumberIsRefusedBeforeItIsConverted() {
        assertThatThrownBy { codec.read("""{"x":${"7".repeat(400_000)}}""") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
