import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/welcome/welcome').then((m) => m.WelcomeComponent),
  },
  {
    path: 'oauth/callback',
    loadComponent: () =>
      import('./features/oauth-callback/oauth-callback').then((m) => m.OAuthCallbackComponent),
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard').then((m) => m.DashboardComponent),
    canActivate: [MsalGuard],
    children: [
      { path: '', redirectTo: 'profile', pathMatch: 'full' },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/dashboard/profile/profile').then((m) => m.ProfileComponent),
      },
      {
        path: 'mail-accounts',
        loadComponent: () =>
          import('./features/dashboard/mail-accounts/mail-accounts').then(
            (m) => m.MailAccountsComponent,
          ),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
