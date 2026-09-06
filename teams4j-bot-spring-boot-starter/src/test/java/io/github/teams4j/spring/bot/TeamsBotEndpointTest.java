package io.github.teams4j.spring.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.security.KeyPair;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import io.github.teams4j.bot.Activity;
import io.github.teams4j.bot.ActivityHandler;
import io.github.teams4j.bot.BotTokenVerifier;
import io.github.teams4j.bot.FakeTransport;
import io.github.teams4j.bot.InvokeResponse;
import io.github.teams4j.bot.TestTokens;
import io.github.teams4j.http.HttpTransport;

/**
 * The endpoint through Spring MVC, with real tokens: the verifier fetches its keys through the
 * {@code HttpTransport} bean, which here is a table.
 */
class TeamsBotEndpointTest {

    private static final String APP_ID = "app-id";
    private static final String SERVICE_URL = "https://smba.trafficmanager.net/apac/";
    private static final KeyPair KEY = TestTokens.rsa();

    private static final String MESSAGE = "{\"type\":\"message\",\"id\":\"m1\",\"text\":\"hi\",\"serviceUrl\":\""
            + SERVICE_URL + "\",\"conversation\":{\"id\":\"a:1\"}}";
    private static final String INVOKE = "{\"type\":\"invoke\",\"name\":\"adaptiveCard/action\",\"serviceUrl\":\""
            + SERVICE_URL + "\",\"conversation\":{\"id\":\"a:1\"}}";

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TeamsBotAutoConfiguration.class))
            .withUserConfiguration(Application.class)
            .withPropertyValues("teams4j.bot.app-id=" + APP_ID, "teams4j.bot.app-secret=s3cret");

    private static String token() {
        return TestTokens.token("k1", KEY, APP_ID, SERVICE_URL, Instant.now());
    }

    private static MockMvc mvc(WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void aVerifiedMessageReachesTheHandlerAndIsAnsweredWithAnEmpty200() {
        runner.run(context -> {
            mvc(context)
                    .perform(post("/api/messages")
                            .header("Authorization", token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(MESSAGE))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));
            assertThat(context.getBean(Application.class).seen.get())
                    .isNotNull()
                    .extracting(Activity::text)
                    .isEqualTo("hi");
        });
    }

    @Test
    void anInvokeIsAnsweredWithTheHandlersJson() {
        runner.run(
                context -> mvc(context)
                        .perform(post("/api/messages")
                                .header("Authorization", token())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(INVOKE))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(
                                content()
                                        .json(
                                                "{\"statusCode\":200,\"type\":\"application/vnd.microsoft.activity.message\",\"value\":\"ok\"}")));
    }

    @Test
    void aRequestWithoutAValidTokenIs401AndNeverReachesTheHandler() {
        runner.run(context -> {
            mvc(context)
                    .perform(post("/api/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(MESSAGE))
                    .andExpect(status().isUnauthorized());
            mvc(context)
                    .perform(post("/api/messages")
                            .header("Authorization", "Bearer forged")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(MESSAGE))
                    .andExpect(status().isUnauthorized());
            assertThat(context.getBean(Application.class).seen.get()).isNull();
        });
    }

    @Test
    void thePathIsConfigurable() {
        runner.withPropertyValues("teams4j.bot.path=/teams/inbound").run(context -> {
            mvc(context)
                    .perform(post("/teams/inbound")
                            .header("Authorization", token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(MESSAGE))
                    .andExpect(status().isOk());
            mvc(context)
                    .perform(post("/api/messages")
                            .header("Authorization", token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(MESSAGE))
                    .andExpect(status().isNotFound());
        });
    }

    /** MVC, a handler, and the key set served from a table through the transport bean. */
    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class Application {
        final AtomicReference<@Nullable Activity> seen = new AtomicReference<>();

        @Bean
        HttpTransport httpTransport() {
            return new FakeTransport()
                    .on(
                            URI.create("https://login.example/openid"),
                            () -> FakeTransport.json(200, "{\"jwks_uri\":\"https://login.example/keys\"}"))
                    .on(
                            URI.create("https://login.example/keys"),
                            () -> FakeTransport.json(200, TestTokens.jwks(TestTokens.jwk("k1", KEY))));
        }

        @Bean
        BotTokenVerifier botTokenVerifier(HttpTransport transport) {
            // The metadata URL is not a property -- production has one value -- so a test that
            // needs a stub declares the verifier bean, which the auto-configuration then backs off.
            return BotTokenVerifier.builder(APP_ID)
                    .transport(transport)
                    .openIdMetadata(URI.create("https://login.example/openid"))
                    .build();
        }

        @Bean
        ActivityHandler activityHandler() {
            return activity -> {
                seen.set(activity);
                return activity.isInvoke() ? InvokeResponse.message("ok") : null;
            };
        }
    }
}
