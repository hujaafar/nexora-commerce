import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { asset, chooseFile, click, fill } from '../../../testing/marketplace-fixtures';
import { MediaManager } from './media-manager';

describe('Media library workflow', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [MediaManager],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    vi.stubGlobal(
      'confirm',
      vi.fn(() => true),
    );
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:library');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
  });
  afterEach(() => {
    http.verify();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });
  it('blocks invalid uploads, stores a linked photo and removes it after confirmation', () => {
    const fixture = TestBed.createComponent(MediaManager);
    fixture.detectChanges();
    http.expectOne('/api/media/images/mine').flush([]);
    fixture.detectChanges();
    chooseFile(fixture, [new File(['bad'], 'bad.txt', { type: 'text/plain' })]);
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
    chooseFile(fixture, [new File(['png'], 'lamp.png', { type: 'image/png' })]);
    fill(fixture, '#product-id', ' product-1 ');
    click(fixture, 'Add to library');
    const upload = http.expectOne('/api/media/images');
    expect(upload.request.body.get('productId')).toBe('product-1');
    expect(upload.request.body.get('purpose')).toBe('PRODUCT_IMAGE');
    upload.flush(asset);
    http.expectOne('/api/media/images/mine').flush([asset]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('1 KB');
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:library');
    fixture.nativeElement.querySelector('[aria-label="Delete image"]').click();
    const deletion = http.expectOne('/api/media/images/photo-1');
    expect(deletion.request.method).toBe('DELETE');
    deletion.flush(null);
    http.expectOne('/api/media/images/mine').flush([]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Your library is empty');
  });
});
