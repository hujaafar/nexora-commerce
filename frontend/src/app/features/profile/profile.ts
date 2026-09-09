/*
 * File purpose: Implements the profile feature behavior.
 */
import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize, of, switchMap } from 'rxjs';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { MediaService } from '../../core/services/media.service';
import { NotificationService } from '../../core/services/notification.service';
import { CommerceService } from '../../core/services/commerce.service';
import { CustomerAnalytics, SellerAnalytics } from '../../models/commerce.model';

@Component({
  selector: 'app-profile',
  imports: [CurrencyPipe, ReactiveFormsModule, RouterLink],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile implements OnInit, OnDestroy {
  private readonly formBuilder = inject(FormBuilder);
  private readonly mediaService = inject(MediaService);
  private readonly notifications = inject(NotificationService);
  private readonly commerce = inject(CommerceService);
  protected readonly authService = inject(AuthService);

  protected readonly saving = signal(false);
  protected readonly selectedAvatar = signal<File | null>(null);
  protected readonly avatarPreview = signal<string | null>(null);
  protected readonly avatarUrl = signal<string | null>(null);
  protected readonly customerAnalytics = signal<CustomerAnalytics | null>(null);
  protected readonly sellerAnalytics = signal<SellerAnalytics | null>(null);
  protected readonly maximumUnits = computed(() => {
    const metrics = this.authService.isSeller()
      ? (this.sellerAnalytics()?.bestSellingProducts ?? [])
      : (this.customerAnalytics()?.mostBoughtProducts ?? []);
    return Math.max(1, ...metrics.map((metric) => metric.units));
  });
  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
  });

  ngOnInit(): void {
    this.applyUser();
    this.authService.refreshProfile().subscribe(() => this.applyUser());
    if (this.authService.isSeller()) {
      this.commerce.sellerAnalytics().subscribe((analytics) => this.sellerAnalytics.set(analytics));
    } else if (this.authService.isClient()) {
      this.commerce
        .customerAnalytics()
        .subscribe((analytics) => this.customerAnalytics.set(analytics));
    }
  }

  ngOnDestroy(): void {
    this.revokePreview();
  }

  protected chooseAvatar(event: Event): void {
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
    this.selectedAvatar.set(file);
    this.avatarPreview.set(URL.createObjectURL(file));
  }

  protected removeAvatar(): void {
    this.revokePreview();
    this.selectedAvatar.set(null);
    this.avatarUrl.set(null);
  }

  protected submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.saving()) {
      return;
    }
    this.saving.set(true);
    const avatarUpload = this.selectedAvatar()
      ? this.mediaService
          .upload(this.selectedAvatar()!, undefined, 'AVATAR')
          .pipe(switchMap((asset) => of(asset.url)))
      : of(this.avatarUrl());

    avatarUpload
      .pipe(
        switchMap((avatarUrl) =>
          this.authService.updateProfile({
            name: this.form.getRawValue().name,
            avatarUrl,
          }),
        ),
        finalize(() => this.saving.set(false)),
      )
      .subscribe((profile) => {
        this.notifications.success('Your profile was updated.');
        this.revokePreview();
        this.selectedAvatar.set(null);
        this.avatarUrl.set(profile.avatarUrl);
      });
  }

  protected barWidth(units: number): number {
    return Math.max(8, (units / this.maximumUnits()) * 100);
  }

  private applyUser(): void {
    const user = this.authService.currentUser();
    if (user) {
      this.form.controls.name.setValue(user.name);
      this.avatarUrl.set(user.avatarUrl);
    }
  }

  private revokePreview(): void {
    const preview = this.avatarPreview();
    if (preview) {
      URL.revokeObjectURL(preview);
    }
    this.avatarPreview.set(null);
  }
}
