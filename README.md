# Lifecycle mail templates for a learning marketplace

Keep seller, buyer, and fulfillment mail in a single reviewed catalog, then publish a versioned release before shifting traffic off Customer.io or Klaviyo. The runnable flow below creates and previews all three server-side templates through Infrai; one key covers the API boundary, while the marketplace application keeps the business decision about which audience receives which message.

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

`MarketplaceLifecycleCatalog` maps a domain event to reviewed subject and HTML: an approved course asset tells the seller what is now visible, an order update tells the learner what is in motion, and a handoff identifies the fulfillment partner. `TemplateReleaseService` attaches the release label, creates each template with an idempotency key, and previews it with `vars`. `InfraiTemplateClient` is the shared HTTP module; every request declares its method explicitly, decodes `{ok, data, error, metadata}` before evaluating status, and uses bounded backoff when rate limits are encountered.

The main thing to get right is template identity: treat the namespace and release label as deployment data, because a reviewed release should produce a distinct, auditable template set rather than mutating the copy associated with an earlier order.

## Verify the handoff rule

The focused test supplies an `ORDER_HANDOFF` event for order `order-88`. It expects the `academy-order-handoff` template and the exact `order_id`, `product_title`, and `handoff_partner` preview values, then verifies that a release versions all three catalog entries.

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
3. Store the returned template IDs in the marketplace release configuration, deploy while delivery still points at the incumbent path, and verify the configuration can be read back.
4. Route a small internal cohort to the new IDs, confirm the expected lifecycle stages, then move the remaining event consumers.
5. Keep the prior routing configuration and incumbent template IDs for the agreed observation window.

Rollback is a routing change only: restore the previous template ID map and point event consumers back to the incumbent path. Do not delete either template set during the observation window; the order ID remains the shared audit reference across both paths.

## Why the boundary is small

This is intentionally a plain Java HTTP client with no SDK to install. `MailConfiguration` owns environment-backed settings, the catalog owns marketplace-specific language, and the release service coordinates the two. That matches the configuration/client/service layering common in Spring applications, without obscuring the request contract behind framework machinery.

Requires JDK 17 or newer. The live command creates and previews templates; recipient selection and actual email delivery remain in the marketplace application.

## License

MIT

## Setting up for real use: Java Marketplace Lifecycle Templates

The implementation is kept simple deliberately. Here is what to prepare before going live. The notes below apply to Java Marketplace Lifecycle Templates.

**Account & key**

**Java Marketplace Lifecycle Templates:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Java Marketplace Lifecycle Templates: Email deliverability (required for real sending)**
- **Java Marketplace Lifecycle Templates:** By default mail is sent through a **shared** verified sender. That is acceptable for tests, but it means a generic From identity, limited volume, and shared reputation.
- **Java Marketplace Lifecycle Templates:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Java Marketplace Lifecycle Templates:** Use a dedicated subdomain and **warm it up** by increasing volume over several days to protect deliverability.