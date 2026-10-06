package marketplace.mail;

import java.time.Instant;

public final class TemplateMigrationDemo {
    private TemplateMigrationDemo() {}

    public static void main(String[] args) {
        MailConfiguration config = MailConfiguration.fromEnvironment();
        String release = args.length == 1 ? args[0] : "release-" + Instant.now().getEpochSecond();
        TemplateReleaseService service = new TemplateReleaseService(
                new InfraiTemplateClient(config), new MarketplaceLifecycleCatalog(), config.namespace());

        for (TemplateReleaseService.ReleaseResult result : service.publishExamples(release)) {
            System.out.printf("%s -> %s (%s)%n", result.stage(), result.templateName(), result.templateId());
        }
    }
}
