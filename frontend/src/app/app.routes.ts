/*
 * File purpose: Defines lazy application routes and their guards.
 */
import { Routes } from '@angular/router';
import { adminGuard } from './core/guards/admin.guard';
import { authGuard } from './core/guards/auth.guard';
import { clientGuard } from './core/guards/client.guard';
import { sellerGuard } from './core/guards/seller.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'products',
  },
  {
    path: 'products',
    loadComponent: () =>
      import('./features/products/product-list/product-list').then((module) => module.ProductList),
  },
  {
    path: 'products/:id',
    loadComponent: () =>
      import('./features/products/product-detail/product-detail').then(
        (module) => module.ProductDetail,
      ),
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login').then((module) => module.Login),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register').then((module) => module.Register),
  },
  {
    path: 'seller',
    canActivate: [authGuard, sellerGuard],
    loadComponent: () =>
      import('./features/seller/dashboard/seller-dashboard').then(
        (module) => module.SellerDashboard,
      ),
  },
  {
    path: 'media',
    canActivate: [authGuard, sellerGuard],
    loadComponent: () =>
      import('./features/media/media-manager/media-manager').then((module) => module.MediaManager),
  },
  {
    path: 'admin',
    canActivate: [authGuard, adminGuard],
    loadComponent: () =>
      import('./features/admin/admin-dashboard').then((module) => module.AdminDashboard),
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () => import('./features/profile/profile').then((module) => module.Profile),
  },
  {
    path: 'cart',
    canActivate: [authGuard, clientGuard],
    loadComponent: () => import('./features/cart/cart-page').then((module) => module.CartPage),
  },
  {
    path: 'checkout',
    canActivate: [authGuard, clientGuard],
    loadComponent: () =>
      import('./features/checkout/checkout-page').then((module) => module.CheckoutPage),
  },
  {
    path: 'orders',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/orders/order-list/order-list').then((module) => module.OrderList),
  },
  {
    path: 'orders/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/orders/order-detail/order-detail').then((module) => module.OrderDetail),
  },
  {
    path: 'wishlist',
    canActivate: [authGuard, clientGuard],
    loadComponent: () =>
      import('./features/wishlist/wishlist-page').then((module) => module.WishlistPage),
  },
  {
    path: '**',
    redirectTo: 'products',
  },
];
