# Nexora redesign brief

Authored design decisions under the user's request to change the whole design
from A to Z. These are decisions, not invented interview answers.

User request: "i dont like the desgin i want image enmation and scroll effect like new3flex , change the whole desgin from a-z".
The existing local reference is Neo4flix, whose entrance rotates a cinematic
reel and moves the camera through its centre. The user previously supplied
ScrollCraft and the Estates interaction video. The earlier Nexora floating-card
refinement was rejected as insufficient. No new approval checkpoint is needed
to carry out the expressly requested redesign.

## Eight creative decisions

1. Vibe: tactile, photographic, bold, editorial. Neo4flix provides the spatial
   movement reference, not movie branding. Ivory paper, ink, and burnt orange.
2. Journey: image-led opening, category selection, immersive campaign window,
   real searchable collection, quiet member/shop invitation.
3. Energy: a composed opening grows into the image reveal; shopping stays calm.
4. Feelings: curiosity (layered objects), agency (real category shortcuts),
   delight (the photograph opens into a room), confidence (operable catalog),
   belonging (join the marketplace). The room reveal is the one peak.
5. Signature: a campaign photograph appears as a narrow framed aperture, opens
   across the viewport as a near product cutout slides past, then settles as a
   full photographic spread. Text and products live on separate motion planes.
6. Aesthetic: editorial retail with a graphic, oversized masthead; no glowing
   dashboard cards or gradients on buttons. Account and management surfaces
   use the same type, paper palette, rules, and image treatment.
7. Distinct scenes. The useful catalog remains directly reachable. No long
   flythrough, autoplay video, or continuously rendering WebGL on this laptop.
8. Existing assets: product API images and the prior artwork. New campaign
   images use the built-in image generator; no Kie API is needed. Generated
   campaign art is not represented as a specific listed product.

## Grammar and fingerprint

Custom retail lookbook: a masthead-integrated opening, real category index,
one photographic aperture spread, catalog, then a split membership invitation.
Navigation is a functional shop/account bar with a compact mobile menu. The
ending is a two-path invitation (shop or sell), not an empty pinned CTA.
Forbids: repeated story cards, pinned explanatory text, synthetic statistics,
unbounded ambient motion, and delaying direct catalog navigation.

The eight stock grammars were considered: filmic/continuous worlds obstruct
direct shopping; chaptered editorial forbids the requested image-led first
screen; live surface belongs to the dashboard; typographic poster demotes the
requested imagery; gallery repeats the rejected structure; split-stage implies
a comparison we do not have; cutlist forbids the short photographic reveal.
The chosen structure defines different navigation, information order, and end.

Against the existing Nexora refinement row: different grammar, navigation,
hero, sequence, ending, and signature (6/6). Its old row is retained.

## Feeling curve, peak, and score

Curiosity: oversized type behind a floating product and offset photo plane.
Agency: category shortcuts select the real catalog filter.
Delight: a room-sized photograph opens around a passing foreground object.
Confidence: a readable catalog with search/filter/sort and honest API states.
Belonging: a stable invitation with real registration destinations.

Peak: "The little picture opened into a whole room as I moved past the object."
Tell-someone sentence: "It's the shop where the photographs open around you."
No authored dead-scroll intervals; each pinned interval must visibly change.

| Beat | Device | Span |
|---|---|---|
| Opening | independent CSS depth/pointer with short pin | 1.7 screens |
| Categories | natural flow + image hover | content height |
| Campaign peak | reveal aperture + foreground parallax | 2.2 screens |
| Catalog | short staggered entrances, real controls | product count |
| Invitation | quiet split photograph, no pin | one short screen |

## Layer contract

The paper/type plane moves least. The hero photograph moves moderately and
the alpha headphone subject moves independently in front, with a small cropped
sneaker foreground providing near depth. The generated cutouts are explicitly
floating studio objects, with no ground-contact illusion to break. The campaign
room has no duplicated extracted object. Typography remains readable above
the room or on paper; no full-image dark scrim. All assets retain a stable
static layout when reduced motion is enabled. Phone layouts recompose images
and labels and eliminate extended pinning. Offscreen motion work is released.
