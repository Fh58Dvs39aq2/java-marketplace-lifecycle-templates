package marketplace.mail;

import java.util.ArrayList;
import java.util.List;

public final class TemplateReleaseService {
    public record ReleaseResult(String stage, String templateName, String templateId, String preview) {}

    private final TemplateGateway gateway;
    private final MarketplaceLifecycleCatalog catalog;
    private final String namespace;

    public TemplateReleaseService(TemplateGateway gateway, MarketplaceLifecycleCatalog catalog, String namespace) {
        this.gateway = gateway;
        this.catalog = catalog;
        this.namespace = namespace;
    }

    public List<ReleaseResult> publishExamples(String release) {
        List<ReleaseResult> results = new ArrayList<>();
        for (MarketplaceLifecycleCatalog.LifecycleEvent event : catalog.examples()) {
            TemplateDefinition base = catalog.templateFor(namespace, event);
            TemplateDefinition versioned = new TemplateDefinition(
                    base.name() + "-" + release, base.subject(), base.html());
            String requestKey = "template:" + versioned.name();
            PublishedTemplate published = gateway.create(versioned, requestKey);
            String preview = gateway.preview(published.id(), catalog.templateVars(event));
            results.add(new ReleaseResult(event.stage().name(), published.name(), published.id(), preview));
        }
        return List.copyOf(results);
    }
}
