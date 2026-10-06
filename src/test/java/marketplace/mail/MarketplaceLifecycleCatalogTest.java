package marketplace.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MarketplaceLifecycleCatalogTest {
    public static void main(String[] args) {
        MarketplaceLifecycleCatalog catalog = new MarketplaceLifecycleCatalog();
        MarketplaceLifecycleCatalog.LifecycleEvent handoff = new MarketplaceLifecycleCatalog.LifecycleEvent(
                MarketplaceLifecycleCatalog.Stage.ORDER_HANDOFF,
                "Teacher Lin", "Ari", "Algebra Lab", "order-88", "North Hub");

        TemplateDefinition selected = catalog.templateFor("academy", handoff);
        assert selected.name().equals("academy-order-handoff");
        assert catalog.templateVars(handoff).equals(Map.of(
                "order_id", "order-88", "product_title", "Algebra Lab", "handoff_partner", "North Hub"));

        FakeGateway gateway = new FakeGateway();
        List<TemplateReleaseService.ReleaseResult> results = new TemplateReleaseService(
                gateway, catalog, "academy").publishExamples("cutover-7");
        assert results.size() == 3;
        assert gateway.names.stream().allMatch(name -> name.endsWith("-cutover-7"));
        assert gateway.previewVars.get(2).get("handoff_partner").equals("Campus Dispatch");
        System.out.println("PASS: handoff selects the order handoff template and its required values");
    }

    private static final class FakeGateway implements TemplateGateway {
        private final List<String> names = new ArrayList<>();
        private final List<Map<String, String>> previewVars = new ArrayList<>();

        @Override
        public PublishedTemplate create(TemplateDefinition template, String requestKey) {
            names.add(template.name());
            return new PublishedTemplate(template.name(), "tpl-" + names.size());
        }

        @Override
        public String preview(String templateId, Map<String, String> templateVars) {
            previewVars.add(templateVars);
            return "{\"ok\":true,\"data\":{\"html\":\"rendered\"},\"error\":null,\"metadata\":{}}";
        }
    }
}
