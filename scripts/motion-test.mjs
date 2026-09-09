import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { JSDOM } from '../frontend/node_modules/jsdom/lib/api.js';

// A unit test of the vendor runtime's route lifetime; no real browser required.
const dom = new JSDOM('<style>.sc-stage { position: sticky; top: 0; }</style><main><section data-sc-act="pin" data-sc-span="2"><div class="sc-stage">Collection</div></section></main>', { runScripts: 'outside-only', pretendToBeVisual: true });
const { window } = dom;
window.matchMedia = () => ({ matches: false, addEventListener() {}, removeEventListener() {} });
const frames = new Map();
let nextFrame = 1;
window.requestAnimationFrame = (fn) => { const id = nextFrame++; frames.set(id, fn); return id; };
window.cancelAnimationFrame = (id) => frames.delete(id);
window.IntersectionObserver = class { observe() {} disconnect() {} };
window.eval(readFileSync(new URL('../frontend/public/motion/scrollcraft.js', import.meta.url), 'utf8'));
const root = window.document.querySelector('main');
for (let cycle = 0; cycle < 3; cycle++) {
  const instance = window.ScrollCraft.mount(root);
  assert.equal(window.ScrollCraft.instances.length, 1);
  assert.ok(frames.size > 0, 'The mounted runtime schedules animation frames');
  instance.destroy();
  assert.equal(frames.size, 0, 'Route disposal cancels all outstanding frames');
  assert.equal(window.ScrollCraft.instances.length, 0);
  assert.equal(root.querySelector('.sc-stage').getAttribute('style'), null);
  assert.equal(root.querySelector('section').getAttribute('style'), null);
  instance.destroy();
}
window.close();
console.log('PASS: three ScrollCraft mount/dispose cycles restore styles and leave no animation frames or instances.');
