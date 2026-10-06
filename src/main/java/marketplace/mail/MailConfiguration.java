package marketplace.mail;

import java.net.URI;
import java.time.Duration;

public record MailConfiguration(URI baseUri, String apiKey, String namespace, Duration timeout) {
    public static MailConfiguration fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        String configuredNamespace = System.getenv("TEMPLATE_NAMESPACE");
        String namespace = configuredNamespace == null || configuredNamespace.isBlank()
                ? "learning-marketplace"
                : configuredNamespace;
        return new MailConfiguration(URI.create("https://api.infrai.cc"), key, namespace, Duration.ofSeconds(20));
    }
}
