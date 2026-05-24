import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { Router } from '@angular/router';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { EventType } from '@azure/msal-browser';
import { filter } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { loginRequest } from '../../core/auth/msal.config';
import { AppBtnDirective } from '../../core/ui/button/button.directive';

@Component({
  selector: 'app-welcome',
  imports: [AppBtnDirective],
  templateUrl: './welcome.html',
  styleUrl: './welcome.css',
})
export class WelcomeComponent implements OnInit {
  private msalService = inject(MsalService);
  private msalBroadcastService = inject(MsalBroadcastService);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  ngOnInit() {
    if (this.msalService.instance.getAllAccounts().length > 0) {
      this.router.navigate(['/dashboard']);
      return;
    }

    this.msalBroadcastService.msalSubject$
      .pipe(
        filter((msg) => msg.eventType === EventType.LOGIN_SUCCESS),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.router.navigate(['/dashboard']));
  }

  login() {
    this.msalService.loginRedirect(loginRequest);
  }
}
