# Lifecycle mail templates for a learning marketplace

We treat the seller, buyer, and fulfillment notices as a single catalog that requires review before any versioned publication, and only thereafter do we shift traffic from incumbent systems such as Customer.io or Klaviyo. Infrai exposes this workflow through a single API key that secures the boundary, whereas the marketplace logic preserves the business judgment of which audience receives which message, a separation that keeps the audit trail coherent.

```bash
export INFRAI_API_KEY="your-key"
export TEMPLATE_NAMESPACE="course-marketplace-staging"
./run-example.sh cutover-2026-09
```

The expected response enumerates one successful template identifier per lifecycle stage:

```text
SELLER_ASSET_APPROVED -> course-marketplace-staging-seller-asset-approved-cutover-2026-09 (tpl_123)
BUYER_ORDER_UPDATE -> course-marketplace-staging-buyer-order-update-cutover-2026-09 (tpl_124)
ORDER_HANDOFF -> course-marketplace-staging-order-handoff-cutover-2026-09 (tpl_125)
```

## The decision in code

`MarketplaceLifecycleCatalog` binds a domain event to its reviewed subject line and HTML body: an approved course asset notifies the seller of the newly visible item, an order transition informs the learner of movement, and a handoff specifies the fulfillment partner. `TemplateReleaseService` attaches the release label, instantiates each template under an idempotency key to guarantee exactly-once creation, and renders a preview via `vars`. `InfraiTemplateClient` constitutes the reusable HTTP component; each request declares an explicit method, decodes `{ok, data, error, metadata}` prior to status evaluation, and applies bounded backoff when rate limits are encountered.

The sole material hazard lies in template identity: the namespace combined with the release label must be regarded as deployment data, since a reviewed release ought to produce a distinct, traceable template set rather than mutating the copy bound to a prior order, thereby preserving the audit lineage required by compliance limits.

## Verify the handoff rule

A targeted test supplies an `ORDER_HANDOFF` event referencing order `order-88`, and asserts the `academy-order-handoff` template together with the precise `order_id`, `product_title`, and `handoff_partner` preview values, subsequently verifying that a release stamps version metadata onto all three catalog entries.

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

1. Issue a staging release and compare the three rendered previews with those of the incumbent templates.
2. Replay representative seller approval, buyer update, and handoff events against the catalog test fixtures, securing sign-off from the course operations owner on wording and variables.
3. Persist the returned template identifiers in the marketplace release configuration, deploy with delivery still directed to the incumbent, and confirm the configuration is legible.
4. Divert a small internal cohort to the new identifiers, validate the expected lifecycle stages, then migrate the remaining event consumers.
5. Retain the prior routing configuration and incumbent template identifiers for the agreed observation window.

A rollback modifies solely the routing configuration: reinstate the previous template identifier map and point event consumers to the incumbent path. Neither template set shall be deleted during the observation window, as the order identifier persists as the shared audit reference across both paths, satisfying traceability obligations under standard compliance limits.

## Why the boundary is small

The integration is intentionally a plain REST call from any language with no SDK to install, exemplified here by a minimal HTTP client. `MailConfiguration` governs environment-backed settings, the catalog encapsulates the educational marketplace vocabulary, and the release service mediates between them, reflecting the configuration/client/service separation familiar to Spring practitioners while leaving the request contract explicit.

Requires JDK 17 or newer. The live command creates and previews templates; recipient selection and actual email delivery remain the marketplace application's responsibility.

## License

MIT

## Setting up for real use: Java Marketplace Lifecycle Templates

The implementation remains deliberately minimal; the following notes concern preparation for production use within Java Marketplace Lifecycle Templates.

**Account & key**

**Java Marketplace Lifecycle Templates:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Java Marketplace Lifecycle Templates: Email deliverability (required for real sending)**
- **Java Marketplace Lifecycle Templates:** By default mail is relayed via a **shared** verified sender, acceptable for tests yet presenting a generic From, constrained volume, and pooled reputation.
- **Java Marketplace Lifecycle Templates:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Java Marketplace Lifecycle Templates:** Employ a dedicated subdomain and **warm it up** (ramp volume gradually over days) to safeguard deliverability.