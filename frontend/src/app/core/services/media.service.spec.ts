/*
 * File purpose: Verifies media.service.spec behavior.
 */
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ALLOWED_IMAGE_TYPES, MAX_IMAGE_BYTES, MediaService } from './media.service';

describe('MediaService client validation', () => {
  let service: MediaService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient()],
    });
    service = TestBed.inject(MediaService);
  });

  it('accepts the same image allowlist as the backend', () => {
    for (const contentType of ALLOWED_IMAGE_TYPES) {
      const file = new File(['image'], 'image.bin', { type: contentType });
      expect(service.validationMessage(file)).toBeNull();
    }
  });

  it('rejects files larger than 2 MB before making an API call', () => {
    const content = new Uint8Array(MAX_IMAGE_BYTES + 1);
    const file = new File([content], 'large.png', { type: 'image/png' });
    expect(service.validationMessage(file)).toContain('2 MB');
  });

  it('rejects non-image content types', () => {
    const file = new File(['hello'], 'notes.txt', { type: 'text/plain' });
    expect(service.validationMessage(file)).toContain('JPEG');
  });
});
