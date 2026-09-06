package io.github.teams4j.spring.bot;

import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import io.github.teams4j.bot.ActivityEndpoint;
import io.github.teams4j.bot.ActivityHandler;

/**
 * The messaging endpoint: {@code POST teams4j.bot.path}, header and body handed to
 * {@link ActivityEndpoint}, its answer written back. Registered by the auto-configuration when the
 * application has Spring MVC and an {@link ActivityHandler} bean; a controller of the application's
 * own is these few lines again.
 */
@RestController
public class TeamsBotController {

    private final ActivityEndpoint endpoint;
    private final ActivityHandler handler;

    public TeamsBotController(ActivityEndpoint endpoint, ActivityHandler handler) {
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.handler = Objects.requireNonNull(handler, "handler");
    }

    @PostMapping(path = "${teams4j.bot.path:/api/messages}")
    public ResponseEntity<String> receive(
            @RequestHeader(name = "Authorization", required = false) @Nullable String authorization,
            @RequestBody String body) {
        ActivityEndpoint.Response answer = endpoint.handle(authorization, body, handler);
        ResponseEntity.BodyBuilder response = ResponseEntity.status(answer.status());
        String json = answer.body();
        return json == null
                ? response.build()
                : response.contentType(MediaType.APPLICATION_JSON).body(json);
    }
}
