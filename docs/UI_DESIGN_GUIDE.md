<!-- BUY-01 learning header
File purpose: Documents the project ui design guide.
Learning focus: Living documentation and traceability alongside implementation.
-->
# BUY-01 UI Design Guide

This guide explains the production UI as a set of small concepts you can study
independently. Read the commits in order and compare each one with its parent:

```powershell
git show <commit>
git diff <commit>^ <commit>
```

## 1. Visual tokens and the application shell

Commit: `ff92166 feat(ui): introduce the neon glass design system`

The global stylesheet defines semantic CSS custom properties instead of repeating
raw colors:

- `--void`, `--surface`, and `--line` create the dark depth system.
- `--brand`, `--accent`, and `--cyan` express hierarchy and state.
- `--radius-*`, `--shadow-*`, and `--ease-out` keep every component consistent.

The application shell demonstrates layered gradients, glass surfaces,
`backdrop-filter`, a sticky navigation bar, visible focus styles, and a responsive
footer. Global `prefers-reduced-motion` rules make animations safe for people who
request less motion.

## 2. Cinematic catalog composition

Commit: `98b66fc feat(storefront): create the cinematic animated catalog`

The catalog uses three layout ideas:

1. A split hero creates an immediate story/action hierarchy.
2. A code-native product visualization provides atmosphere without downloading a
   decorative image.
3. A responsive card grid keeps real product data dominant.

The CSS uses pseudo-elements for ambient light, transform-only animation for
smooth rendering, and staggered animation delays from Angular's `@for` index.
Product links remain semantic anchors, so the experience still works with a
keyboard and keeps native navigation behavior.

## 3. Immersive product detail

Commit: `6752abf feat(storefront): build an immersive product gallery`

The detail page separates visual exploration from purchase information. The main
gallery is sticky on wide screens, while the information column scrolls naturally.
Real images use meaningful alternative text; generated decoration is hidden from
assistive technology.

The page also shows how to:

- Keep a strong visual result when a product has no images.
- Turn repeated facts into compact assurance cards.
- Use `aspect-ratio` and `object-fit` to avoid layout shifts.
- Preserve a readable single-column flow on small screens.

## 4. Secure authentication as a story

Commit: `be07688 feat(auth-ui): animate the secure access experience`

Login and registration share one SCSS partial so their layout, focus behavior,
motion, and responsive rules cannot drift. Their Angular forms and validation
logic stay unchanged.

The role selector uses `:has(input:checked)` to style the selected radio card
without extra TypeScript. The moving light, status pulse, and entrance animations
use CSS only, and the global reduced-motion rule automatically neutralizes them.

## 5. Identity console

Commits:

- `4675f6e feat(profile-ui): create the animated identity console`
- `6b7af58 feat(profile): clarify and verify seller avatar replacement`

The profile page turns account state into a visual identity map while preserving
the working upload flow. Sellers see explicit Add avatar, Change avatar, and
Remove actions with a local preview before saving. Client and admin accounts see
the same polished profile surface without receiving seller-only upload actions.

## 6. Reactive seller metrics

Commit: `743f05c feat(seller-ui): transform inventory into a command center`

The seller dashboard introduces Angular `computed()` signals:

```ts
protected readonly totalUnits = computed(() =>
  this.products().reduce((total, product) => total + product.quantity, 0)
);
```

Computed signals derive the metric rail directly from the product signal. They
recalculate when the catalog changes and require no manual synchronization.
Catalog value, image count, and unit count use the same pattern.

The editor and inventory stay on one screen, but sticky positioning is disabled at
smaller breakpoints so the form never traps mobile users.

## 7. Reactive media vault

Commit: `c3f555b feat(media-ui): redesign uploads as a digital asset vault`

The media page applies the same computed-signal concept to total storage and
product imagery. The selected-file preview, loading state, empty state, uploaded
asset grid, and destructive action each have distinct visual feedback.

The browser's file filter is only convenience. The backend still checks size,
declared MIME type, real byte signature, ownership, and authorization.

## 8. Production budgets

Commit: `ebdcd90 build(ui): set intentional production style budgets`

Angular budgets are guardrails, not arbitrary defaults. The final limits are set
just above the measured production output:

- Initial JavaScript warns at 540 kB and fails at 600 kB.
- A component stylesheet warns at 18 kB and fails at 22 kB.

This gives the design room to exist while ensuring future changes cannot silently
grow without review.

## Animation rules used throughout

- Prefer `transform` and `opacity`; browsers can animate them efficiently.
- Keep ambient animation slow and interaction feedback fast.
- Never hide essential information inside animation.
- Give hover effects an equivalent keyboard focus state.
- Respect `prefers-reduced-motion`.
- Avoid layout-changing properties such as width, height, and margin in looping
  animations.

## Suggested practice

Try these exercises on a separate branch:

1. Change only the root color tokens to create a warm alternate theme.
2. Add a catalog card status derived from quantity with a `computed()` signal.
3. Remove one animation and confirm the page remains understandable.
4. Test the catalog at 360 px, 768 px, and 1440 px widths.
5. Lower the component budget by 1 kB and use the production build output to find
   the largest stylesheet.

## Current marketplace-native storefront

Commit: `93cbfc6 feat(storefront): build a marketplace-native animated landing page`

The final storefront deliberately removes the portfolio gallery, stock imagery,
and external GIFs. Its visual language now belongs to BUY-01:

- The hero uses CSS-built product parcels and orbiting category tags.
- Real seller product images appear only when returned by the product API.
- Products, About us, and How it works are real page sections.
- Every section button uses smooth scrolling and updates the URL fragment.
- Login, registration, seller tools, profile, and admin moderation remain normal
  application routes.
- Reduced-motion preferences disable smooth scrolling and looping animation.

This keeps the experience expressive without implying that a marketplace is a
technology demo or a creator portfolio.

## Moderation workspace

Commit: `701366d feat(moderation): complete the optional admin workflow`

The admin dashboard presents three human-readable collections: accounts,
products, and uploaded media. It avoids infrastructure language, uses clear
confirmation before deletion, and keeps account data read-only. Route guards
remove the workspace from non-admin navigation, while the services still enforce
the real authorization boundary.
