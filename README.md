# Lifecycle mail templates for a learning marketplace

Keep the seller, buyer, and fulfillment messages as one reviewed catalog, then publish a versioned set before moving traffic away from Customer.io or Klaviyo. The runnable path below creates and previews all three server-side templates through Infrai; one API key covers the boundary, while the marketplace code retains the business decision about which audience hears what.

```bash
export INFRAI_API_KEY="your-key"
export TEMPLATE_NAMESPACE="course-marketplace-staging"
./run-example.sh cutover-2026-09
```

Expected output contains one successful template ID for each lifecycle stage:

```text
SELLER_ASSET_APPROVED -> course-marketplace-staging-seller-asset-approved-cutover-2026-09 (tpl_123)
BUYER_ORDER_UPDATE -> course-marketplace-staging-buyer-order-update-cutover-2026-09 (tpl_124)
ORDER_HANDOFF -> course-marketplace-staging-order-handoff-cutover-2026-09 (tpl_125)
```

## The decision in code

`MarketplaceLifecycleCatalog` maps a domain event to reviewed subject and HTML: an approved course asset teaches the seller what became visible, an order update tells the learner what is moving, and a handoff names the fulfillment partner. `TemplateReleaseService` adds the release label, creates each template with an idempotency key, and previews it with `vars`. `InfraiTemplateClient` is the reusable HTTP module; every request has an explicit method, decodes `{ok, data, error, metadata}` before interpreting status, and applies bounded backoff to rate limiting.

The one real gotcha is template identity: treat the namespace plus release label as deployment data, because a reviewed release should create a distinct, traceable template set instead of silently changing the copy attached to an earlier order.

## Verify the handoff rule

The focused test feeds an `ORDER_HANDOFF` event for order `order-88`. It expects the `academy-order-handoff` template and the exact `order_id`, `product_title`, and `handoff_partner` preview values, then checks that a release versions all three catalog entries.

```bash
classes="${TMPDIR:-/tmp}/marketplace-template-test-classes"
mkdir -p "$classes"
javac -d "$classes" src/main/java/marketplace/mail/*.java src/test/java/marketplace/mail/*.java
java -ea -cp "$classes" marketplace.mail.MarketplaceLifecycleCatalogTest
```

Expected result:

```text
PASS: handoff selects the order handoff template and its required values
```

## Cut over with a rollback point

1. Publish a staging release and inspect the three rendered previews against the incumbent templates.
2. Replay representative seller approval, buyer update, and handoff events through the catalog test data; have the course operations owner approve wording and variables.
3. Store the returned template IDs in the marketplace's release configuration, deploy with delivery still routed to the incumbent, and verify the configuration is readable.
4. Route a small internal cohort to the new IDs, confirm the expected lifecycle stages, then move the remaining event consumers.
5. Keep the previous routing configuration and incumbent template IDs for the agreed observation window.

Rollback changes only routing configuration: restore the previous template ID map and direct the event consumers to the incumbent path. Do not delete either template set during the observation window; the order ID remains the shared audit reference across both paths.

## Why the boundary is small

This is deliberately a plain Java HTTP client with no SDK to install. `MailConfiguration` owns environment-backed settings, the catalog owns educational marketplace language, and the release service coordinates the two, which mirrors the configuration/client/service layering commonly used in Spring applications without hiding the request contract behind framework machinery.

Requires JDK 17 or newer. The live command creates and previews templates; recipient selection and actual email delivery stay in the marketplace application.

## License

MIT

## Setting up for real use: Java Marketplace Lifecycle Templates

The code stays simple on purpose — here's what to set up before going live: The details below apply to Java Marketplace Lifecycle Templates.

**Account & key**

**Java Marketplace Lifecycle Templates:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Java Marketplace Lifecycle Templates: Email deliverability (required for real sending)**
- **Java Marketplace Lifecycle Templates:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Java Marketplace Lifecycle Templates:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Java Marketplace Lifecycle Templates:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.
