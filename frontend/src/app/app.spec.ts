import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { of } from 'rxjs';

describe('App', () => {
  let mockMsalService: any;
  let mockMsalBroadcastService: any;

  beforeEach(async () => {
    mockMsalService = {
      handleRedirectObservable: () => of(null),
    };
    mockMsalBroadcastService = {};

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        { provide: MsalService, useValue: mockMsalService },
        { provide: MsalBroadcastService, useValue: mockMsalBroadcastService },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });
});
