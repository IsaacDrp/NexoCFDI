import { Component, NgZone, OnInit, inject, signal } from '@angular/core';
import { DatePipe, LowerCasePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MailAccountService } from '../../../core/services/mail-account.service';
import { MailAccount } from '../../../core/models/mail-account.model';
import { LinkMailDialogComponent } from './link-mail-dialog/link-mail-dialog';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-mail-accounts',
  imports: [
    DatePipe,
    LowerCasePipe,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './mail-accounts.html',
  styleUrl: './mail-accounts.css',
})
export class MailAccountsComponent implements OnInit {
  private mailService = inject(MailAccountService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  private ngZone = inject(NgZone);

  loading = signal(true);
  accounts = signal<MailAccount[]>([]);
  displayedColumns = ['emailAddress', 'displayName', 'provider', 'status', 'lastSyncAt', 'actions'];

  ngOnInit() {
    this.loadAccounts();
  }

  loadAccounts() {
    this.loading.set(true);
    this.mailService.getAccounts().subscribe({
      next: (accounts) => {
        this.ngZone.run(() => {
          this.accounts.set(accounts);
          this.loading.set(false);
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.snackBar.open('Error al cargar las cuentas de correo', 'Cerrar', { duration: 4000 });
          this.loading.set(false);
        });
      },
    });
  }

  openLinkDialog() {
    const dialogRef = this.dialog.open(LinkMailDialogComponent, { width: '440px' });

    dialogRef.afterClosed().subscribe((displayName: string | undefined) => {
      if (displayName) this.startOAuthFlow(displayName);
    });
  }

  private startOAuthFlow(displayName: string) {
    this.mailService.getAuthUrl('MICROSOFT').subscribe({
      next: ({ authorizationUrl }) => {
        const popup = window.open(
          authorizationUrl,
          'nexo-oauth',
          'width=600,height=700,scrollbars=yes,resizable=yes,noopener=no',
        );

        const handler = (event: MessageEvent) => {
          if (event.origin !== window.location.origin) return;
          const { code, error } = event.data ?? {};
          window.removeEventListener('message', handler);
          clearInterval(popupWatcher);

          if (error || !code) {
            this.snackBar.open('Autorización cancelada o fallida', 'Cerrar', { duration: 4000 });
            return;
          }

          this.mailService
            .linkAccount({
              authorizationCode: code,
              redirectUri: environment.oauthRedirectUri,
              displayName,
              provider: 'MICROSOFT',
            })
            .subscribe({
              next: () => {
                this.ngZone.run(() => {
                  this.snackBar.open('Cuenta vinculada correctamente', 'Cerrar', { duration: 3000 });
                  this.loadAccounts();
                });
              },
              error: () =>
                this.ngZone.run(() =>
                  this.snackBar.open('Error al vincular la cuenta', 'Cerrar', { duration: 4000 }),
                ),
            });
        };

        window.addEventListener('message', handler);

        const popupWatcher = setInterval(() => {
          if (popup?.closed) {
            clearInterval(popupWatcher);
            window.removeEventListener('message', handler);
          }
        }, 800);
      },
      error: () =>
        this.snackBar.open('Error al obtener URL de autorización', 'Cerrar', { duration: 4000 }),
    });
  }

  unlink(account: MailAccount) {
    if (!confirm(`¿Desvincular la cuenta "${account.displayName}" (${account.emailAddress})?`))
      return;

    this.mailService.unlinkAccount(account.id).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.snackBar.open('Cuenta desvinculada', 'Cerrar', { duration: 3000 });
          this.loadAccounts();
        });
      },
      error: () =>
        this.ngZone.run(() =>
          this.snackBar.open('Error al desvincular la cuenta', 'Cerrar', { duration: 4000 }),
        ),
    });
  }

  statusColor(status: string): string {
    return { ACTIVE: 'primary', PAUSED: 'accent', ERROR: 'warn', REVOKED: '' }[status] ?? '';
  }
}
