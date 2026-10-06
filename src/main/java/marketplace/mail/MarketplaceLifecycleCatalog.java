package marketplace.mail;

import java.util.List;
import java.util.Map;

public final class MarketplaceLifecycleCatalog {
    public enum Stage { SELLER_ASSET_APPROVED, BUYER_ORDER_UPDATE, ORDER_HANDOFF }

    public record LifecycleEvent(Stage stage, String sellerName, String buyerName,
                                 String productTitle, String orderId, String handoffPartner) {}

    public TemplateDefinition templateFor(String namespace, LifecycleEvent event) {
        return switch (event.stage()) {
            case SELLER_ASSET_APPROVED -> new TemplateDefinition(
                    namespace + "-seller-asset-approved",
                    "{{product_title}} is ready for learners",
                    "<h1>Approved, {{seller_name}}</h1><p>{{product_title}} is ready for learners.</p>");
            case BUYER_ORDER_UPDATE -> new TemplateDefinition(
                    namespace + "-buyer-order-update",
                    "Your learning order {{order_id}} is moving",
                    "<h1>Hello {{buyer_name}}</h1><p>Your order {{order_id}} for {{product_title}} is moving.</p>");
            case ORDER_HANDOFF -> new TemplateDefinition(
                    namespace + "-order-handoff",
                    "Order {{order_id}} has reached {{handoff_partner}}",
                    "<h1>Handoff complete</h1><p>{{product_title}} is now with {{handoff_partner}}.</p>");
        };
    }

    public Map<String, String> templateVars(LifecycleEvent event) {
        return switch (event.stage()) {
            case SELLER_ASSET_APPROVED -> Map.of(
                    "seller_name", event.sellerName(), "product_title", event.productTitle());
            case BUYER_ORDER_UPDATE -> Map.of(
                    "buyer_name", event.buyerName(), "order_id", event.orderId(),
                    "product_title", event.productTitle());
            case ORDER_HANDOFF -> Map.of(
                    "order_id", event.orderId(), "product_title", event.productTitle(),
                    "handoff_partner", event.handoffPartner());
        };
    }

    public List<LifecycleEvent> examples() {
        return List.of(
                new LifecycleEvent(Stage.SELLER_ASSET_APPROVED, "Dr. Rivera", "Mina",
                        "Practical Geometry", "order-1042", "Campus Dispatch"),
                new LifecycleEvent(Stage.BUYER_ORDER_UPDATE, "Dr. Rivera", "Mina",
                        "Practical Geometry", "order-1042", "Campus Dispatch"),
                new LifecycleEvent(Stage.ORDER_HANDOFF, "Dr. Rivera", "Mina",
                        "Practical Geometry", "order-1042", "Campus Dispatch"));
    }
}

record TemplateDefinition(String name, String subject, String html) {}
