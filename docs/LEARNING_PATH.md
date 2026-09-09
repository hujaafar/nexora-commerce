# Learning path

1. Follow the architecture in the root README and trace a request through
   `gateway-service` into `user-service`.
2. Study JWT claims, BCrypt, roles and ownership checks. Run the tests beside
   each service before changing its behavior.
3. Follow the public product search into MongoDB and inspect atomic stock
   reservation in the product service.
4. Trace cart → checkout → stock reservation → order → cancellation and its
   compensating stock release. Compare customer and seller order views.
5. Upload an image through the media API. Inspect signature validation,
   ownership, object storage and the immutable MediaDownload value.
6. Explore Angular signals, forms, route guards, interceptors and error states.
   Then inspect `storefront-motion.ts` and the ScrollCraft lifecycle adaptation.
7. Read `.github/workflows/ci.yml`, then `Jenkinsfile`, then the optional quality
   stack. Follow a failed test through the pipeline to see why deployment stops.
8. Run the real API integration test and read its ownership and inventory
   assertions. Use the source Git bundles to study the earlier educational
   milestones if they are available locally.

`source-provenance.json` records source heads; those historical commits are
preserved outside the new public repository rather than fabricated as new work.
