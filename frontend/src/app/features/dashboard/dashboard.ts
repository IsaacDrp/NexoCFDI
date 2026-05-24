import { Component, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MsalService } from '@azure/msal-angular';
import { AppIconComponent } from '../../core/ui/icon/icon.component';
import { AppBtnDirective } from '../../core/ui/button/button.directive';

@Component({
  selector: 'app-dashboard',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    AppIconComponent,
    AppBtnDirective,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class DashboardComponent implements OnInit {
  private msalService = inject(MsalService);

  userName = '';
  userEmail = '';

  ngOnInit() {
    const account =
      this.msalService.instance.getActiveAccount() ??
      this.msalService.instance.getAllAccounts()[0];
    if (account) {
      this.userName  = account.name ?? '';
      this.userEmail = account.username ?? '';
    }
  }

  logout() {
    this.msalService.logoutRedirect({ postLogoutRedirectUri: '/' });
  }
}
