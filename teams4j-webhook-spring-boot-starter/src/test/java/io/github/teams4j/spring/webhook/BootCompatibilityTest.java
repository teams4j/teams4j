package io.github.teams4j.spring.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Verifies that one starter artifact supports both Boot 3.x and 4.x.
 *
 * <p>The starter uses only a handful of annotations plus {@code AutoConfiguration.imports}
 * registration, and those sit at the same coordinates in 3.5 and 4.1. Run in the CI matrix
 * ({@code -PbootTestVersion}); a failure is the signal to split the artifact per Boot major.
 */
class BootCompatibilityTest {

    @Test
    void runsAgainstTheBootVersionTheMatrixSelected() {
        String expected = System.getProperty("teams4j.test.bootVersion");
        assertThat(expected).as("the target Boot version injected by the build").isNotBlank();
        assertThat(SpringBootVersion.getVersion())
                .as("the Boot version actually on the classpath")
                .startsWith(majorMinor(expected));
    }

    @Test
    void starterApiSurfaceIsPresent() {
        // These three are the entirety of the starter's dependency on Boot.
        assertThat(AutoConfiguration.class.getName())
                .isEqualTo("org.springframework.boot.autoconfigure.AutoConfiguration");
        assertThat(ConditionalOnMissingBean.class.getName())
                .isEqualTo("org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean");
        assertThat(ConfigurationProperties.class.getName())
                .isEqualTo("org.springframework.boot.context.properties.ConfigurationProperties");
    }

    private static String majorMinor(String version) {
        int first = version.indexOf('.');
        int second = version.indexOf('.', first + 1);
        return second < 0 ? version : version.substring(0, second);
    }
}
