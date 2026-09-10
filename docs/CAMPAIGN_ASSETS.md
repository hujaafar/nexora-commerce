# Campaign image provenance

Generated for the Nexora editorial redesign on 10 September 2026 using the built-in `image_gen.imagegen` tool. No external image API, stock-photo license, or branded product photograph was used. These are illustrative campaign assets; live listing images still come from sellers through the product API.

The original PNGs were inspected and encoded to WebP with FFmpeg/libwebp at quality 86. The headphones and sneaker preserve alpha transparency. Encoding changed the file format, not the creative composition. All headline text is real HTML.

| Asset | Size | Use |
|---|---:|---|
| `frontend/public/assets/editorial-headphones.webp` | 176,804 bytes | Foreground opening and campaign object |
| `frontend/public/assets/editorial-room.webp` | 244,170 bytes | Room reveal, category image, sign-in and registration |
| `frontend/public/assets/editorial-sneaker.webp` | 202,384 bytes | Opening, category, membership composition |

Total: **623,358 bytes**. UI regression screenshots use explicitly isolated visual fixtures; they do not establish that these campaign objects are for sale.

## Final generation prompts

### Headphones

Use case: product-mockup. Asset type: floating foreground cutout for an editorial e-commerce website. Create one exceptionally photorealistic pair of premium over-ear headphones, warm ivory cushions, brushed champagne aluminum ear cups and a soft burnt-orange headband detail, three-quarter view, complete object tilted slightly diagonally as if suspended in a studio. Tactile magazine product photography, warm directional daylight from upper left, crisp material detail, naturally rich color, no plastic CGI look. Genuine transparent alpha background, no ground, no cast floor shadow, no backdrop or checkerboard. Keep the entire headphone silhouette comfortably inside the frame with 12 percent clear margins. No logos, text, watermarks, hands or extra objects. This is illustrative campaign artwork, not a representation of a branded product. High resolution square image.

### Room

Use case: photorealistic-natural. Asset type: full-width editorial campaign photograph for an e-commerce lookbook and its sign-in page. Tactile magazine product photography, warm directional daylight from upper left, crisp material detail, naturally rich color, no plastic CGI look. Photograph a beautiful lived-in modernist corner of a Mediterranean apartment: a sculptural burnt-orange upholstered lounge chair on the right, a low travertine table with a simple cream ceramic vase and one leafy branch, warm ivory plaster walls, a softly wrinkled linen curtain on the left, an olive green fabric bag on the chair. Late-afternoon architectural shadows, honest material texture, quietly stylish and welcoming. Wide cinematic landscape framing, eye-level camera, strong foreground-to-background depth. The left half should be mostly the ivory curtain and warmly lit wall, usable as an image crop. No people, no text, no logo, no watermark. One realistic photograph, no collage or graphic design.

### Sneaker

Use case: product-mockup. Asset type: foreground floating cutout for an editorial e-commerce lookbook. Tactile magazine product photography, warm directional daylight from upper left, crisp material detail, naturally rich color, no plastic CGI look. One stylish unbranded low-top sneaker in ivory suede and olive-green canvas, gum sole, cream laces, high-end functional everyday design. Complete shoe in dynamic three-quarter side view, toe points right and slightly toward camera, floating diagonally with no ground. Genuine transparent alpha background. Keep the entire silhouette and laces inside frame with generous 12 percent margins. No floor shadow, no checkerboard, no backdrop, no text, no logos, no people, no extra objects. Campaign artwork, not a depiction of a specific branded listing. Square high resolution.
