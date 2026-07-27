/*
 * File purpose: Implements the media feature behavior.
 */
import {
  Component,
  computed,
  inject,
  OnDestroy,
  OnInit,
  signal
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { MediaService } from '../../../core/services/media.service';
import { NotificationService } from '../../../core/services/notification.service';
import { MediaAsset, MediaPurpose } from '../../../models/media.model';

@Component({
  selector: 'app-media-manager',
  imports: [ReactiveFormsModule],
  templateUrl: './media-manager.html',
  styleUrl: './media-manager.scss'
})
export class MediaManager implements OnInit, OnDestroy {
  private readonly formBuilder = inject(FormBuilder);
  private readonly mediaService = inject(MediaService);
  private readonly notifications = inject(NotificationService);

  protected readonly assets = signal<MediaAsset[]>([]);
  protected readonly loading = signal(true);
  protected readonly uploading = signal(false);
  protected readonly selectedFile = signal<File | null>(null);
  protected readonly previewUrl = signal<string | null>(null);
  protected readonly totalBytes = computed(() =>
    this.assets().reduce((total, asset) => total + asset.size, 0)
  );
  protected readonly productAssetCount = computed(
    () => this.assets().filter((asset) => asset.purpose === 'PRODUCT_IMAGE').length
  );
  protected readonly form = this.formBuilder.nonNullable.group({
    productId: [''],
    purpose: ['PRODUCT_IMAGE' as MediaPurpose]
  });

  ngOnInit(): void {
    this.loadAssets();
  }

  ngOnDestroy(): void {
    this.revokePreview();
  }

  protected chooseFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) {
      return;
    }
    const validationMessage = this.mediaService.validationMessage(file);
    if (validationMessage) {
      this.notifications.error(validationMessage);
      return;
    }
    this.revokePreview();
    this.selectedFile.set(file);
    this.previewUrl.set(URL.createObjectURL(file));
  }

  protected upload(): void {
    const file = this.selectedFile();
    if (!file || this.uploading()) {
      this.notifications.error('Choose an image before uploading.');
      return;
    }
    this.uploading.set(true);
    const { productId, purpose } = this.form.getRawValue();
    this.mediaService
      .upload(file, productId.trim() || undefined, purpose)
      .pipe(finalize(() => this.uploading.set(false)))
      .subscribe((asset) => {
        this.notifications.success(`${asset.originalFilename} was uploaded.`);
        this.revokePreview();
        this.selectedFile.set(null);
        this.form.controls.productId.setValue('');
        this.loadAssets();
      });
  }

  protected delete(asset: MediaAsset): void {
    if (!window.confirm(`Delete “${asset.originalFilename}”?`)) {
      return;
    }
    this.mediaService.delete(asset.id).subscribe(() => {
      this.notifications.success(`${asset.originalFilename} was deleted.`);
      this.loadAssets();
    });
  }

  protected sizeLabel(bytes: number): string {
    return bytes < 1024 * 1024
      ? `${Math.ceil(bytes / 1024)} KB`
      : `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  }

  private loadAssets(): void {
    this.loading.set(true);
    this.mediaService
      .listMine()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((assets) => this.assets.set(assets));
  }

  private revokePreview(): void {
    const url = this.previewUrl();
    if (url) {
      URL.revokeObjectURL(url);
    }
    this.previewUrl.set(null);
  }
}
