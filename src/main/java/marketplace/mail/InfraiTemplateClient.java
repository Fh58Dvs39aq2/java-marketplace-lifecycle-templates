package marketplace.mail;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiTemplateClient implements TemplateGateway {
    private static final Pattern OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*(true|false)");
    private static final Pattern ID = Pattern.compile("\\\"(?:id|template_id)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private final HttpClient http;
    private final MailConfiguration config;

    public InfraiTemplateClient(MailConfiguration config) {
        this(config, HttpClient.newBuilder().connectTimeout(config.timeout()).build());
    }

    InfraiTemplateClient(MailConfiguration config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    @Override
    public PublishedTemplate create(TemplateDefinition template, String requestKey) {
        String body = jsonObject(Map.of(
                "name", template.name(),
                "subject", template.subject(),
                "html", template.html()));
        String response = request("POST", "/v1/email/template/create", body, requestKey);
        Matcher id = ID.matcher(response);
        if (!id.find()) {
            throw new InfraiException(200, "Successful template response did not contain an id");
        }
        return new PublishedTemplate(template.name(), id.group(1));
    }

    @Override
    public String preview(String templateId, Map<String, String> templateVars) {
        String body = "{\"vars\":" + jsonObject(templateVars) + "}";
        return request("POST", "/v1/email/template/preview/" + templateId, body, null);
    }

    // Canonical capability names: infrai.email.template.create and infrai.email.template.preview.
    private String request(String method, String path, String body, String requestKey) {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(config.baseUri().resolve(path))
                    .timeout(config.timeout())
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json");
            if (requestKey != null) {
                builder.header("Idempotency-Key", requestKey);
            }
            HttpRequest request = builder.method(method, HttpRequest.BodyPublishers.ofString(body)).build();
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                Envelope envelope = decodeEnvelope(response.body(), response.statusCode());
                if (response.statusCode() == 429 && attempt < 3) {
                    sleep(retryDelay(response, attempt));
                    continue;
                }
                if (!envelope.ok()) {
                    throw new InfraiException(response.statusCode(), envelope.raw());
                }
                if (response.statusCode() >= 500) {
                    throw new InfraiException(response.statusCode(), "Transport request was not accepted");
                }
                return envelope.raw();
            } catch (IOException e) {
                throw new InfraiException(0, "Could not reach email service", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InfraiException(0, "Interrupted while calling email service", e);
            }
        }
        throw new InfraiException(429, "Retry budget exhausted");
    }

    private static Envelope decodeEnvelope(String raw, int status) {
        Matcher matcher = OK.matcher(raw);
        if (!matcher.find()) {
            throw new InfraiException(status, "Response was not an Infrai envelope");
        }
        return new Envelope(Boolean.parseBoolean(matcher.group(1)), raw);
    }

    private static Duration retryDelay(HttpResponse<?> response, int attempt) {
        String value = response.headers().firstValue("Retry-After").orElse("");
        try {
            return Duration.ofSeconds(Math.max(0, Long.parseLong(value)));
        } catch (NumberFormatException ignored) {
            return Duration.ofMillis(250L * (1L << attempt));
        }
    }

    private static void sleep(Duration delay) throws InterruptedException {
        Thread.sleep(delay.toMillis());
    }

    static String jsonObject(Map<String, String> values) {
        StringBuilder json = new StringBuilder("{");
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (json.length() > 1) json.append(',');
            json.append('"').append(escape(entry.getKey())).append("\":\"")
                    .append(escape(entry.getValue())).append('"');
        }
        return json.append('}').toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private record Envelope(boolean ok, String raw) {}
}

interface TemplateGateway {
    PublishedTemplate create(TemplateDefinition template, String requestKey);
    String preview(String templateId, Map<String, String> templateVars);
}

record PublishedTemplate(String name, String id) {}

final class InfraiException extends RuntimeException {
    private final int status;

    InfraiException(int status, String message) {
        super(message);
        this.status = status;
    }

    InfraiException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    int status() {
        return status;
    }
}
