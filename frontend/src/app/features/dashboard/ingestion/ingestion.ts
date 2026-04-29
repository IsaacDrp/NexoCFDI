import { Component, NgZone, OnDestroy, computed, inject, signal } from '@angular/core';
import { DatePipe, LowerCasePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Subscription, interval, switchMap, takeWhile, tap } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { IngestionService } from '../../../core/services/ingestion.service';
import { IngestedEmailResponse, JobRunResponse } from '../../../core/models/ingestion.model';
import { KeywordConfigDialogComponent } from './keyword-config-dialog/keyword-config-dialog';

interface ApiError {
  message: string;
  code: string;
}

@Component({
  selector: 'app-ingestion',
  imports: [
    DatePipe,
    LowerCasePipe,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './ingestion.html',
  styleUrl: './ingestion.css',
})
export class IngestionComponent implements OnDestroy {
  private ingestionService = inject(IngestionService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  private ngZone = inject(NgZone);

  private pollSub: Subscription | null = null;

  readonly months = [
    { value: 1, label: 'Enero' },
    { value: 2, label: 'Febrero' },
    { value: 3, label: 'Marzo' },
    { value: 4, label: 'Abril' },
    { value: 5, label: 'Mayo' },
    { value: 6, label: 'Junio' },
    { value: 7, label: 'Julio' },
    { value: 8, label: 'Agosto' },
    { value: 9, label: 'Septiembre' },
    { value: 10, label: 'Octubre' },
    { value: 11, label: 'Noviembre' },
    { value: 12, label: 'Diciembre' },
  ];

  readonly currentYear = new Date().getFullYear();
  readonly yearOptions = Array.from({ length: 5 }, (_, i) => this.currentYear - i);

  selectedMonth = signal(new Date().getMonth() + 1);
  selectedYear = signal(this.currentYear);

  triggering = signal(false);
  jobRun = signal<JobRunResponse | null>(null);
  emails = signal<IngestedEmailResponse[]>([]);
  apiError = signal<ApiError | null>(null);

  readonly isRunning = computed(() => this.jobRun()?.status === 'RUNNING');

  displayedColumns = ['subject', 'fromAddress', 'receivedAt', 'types', 'matchReasons', 'attachments'];

  search() {
    this.stopPolling();
    this.jobRun.set(null);
    this.emails.set([]);
    this.apiError.set(null);
    this.triggering.set(true);

    this.ingestionService
      .triggerIngestion({ year: this.selectedYear(), month: this.selectedMonth() })
      .subscribe({
        next: (job) => {
          this.ngZone.run(() => {
            this.jobRun.set(job);
            this.triggering.set(false);
            if (job.status === 'RUNNING') {
              this.startPolling(job.id);
            } else if (job.status === 'SUCCESS') {
              this.loadEmails(job.targetYear, job.targetMonth);
            }
          });
        },
        error: (err: HttpErrorResponse) => {
          this.ngZone.run(() => {
            this.triggering.set(false);
            const body = err.error as Partial<ApiError> | null;
            if (body?.code && body?.message) {
              this.apiError.set({ code: body.code, message: body.message });
            } else {
              this.snackBar.open('Error al iniciar la búsqueda', 'Cerrar', { duration: 4000 });
            }
          });
        },
      });
  }

  private startPolling(jobId: string) {
    this.pollSub = interval(2500)
      .pipe(
        switchMap(() => this.ingestionService.getJobStatus(jobId)),
        tap((job) => this.ngZone.run(() => this.jobRun.set(job))),
        takeWhile((job) => job.status === 'RUNNING', true),
      )
      .subscribe({
        next: (job) => {
          if (job.status === 'SUCCESS') {
            this.ngZone.run(() => this.loadEmails(job.targetYear, job.targetMonth));
          } else if (job.status === 'FAILED') {
            this.ngZone.run(() =>
              this.snackBar.open(
                `Búsqueda fallida: ${job.errorMessage ?? 'Error desconocido'}`,
                'Cerrar',
                { duration: 6000 },
              ),
            );
          }
        },
        error: () => {
          this.ngZone.run(() =>
            this.snackBar.open('Error al consultar el estado del proceso', 'Cerrar', {
              duration: 4000,
            }),
          );
        },
      });
  }

  private loadEmails(year: number, month: number) {
    this.ingestionService.findEmails(year, month).subscribe({
      next: (emails) => this.ngZone.run(() => this.emails.set(emails)),
      error: () =>
        this.ngZone.run(() =>
          this.snackBar.open('Error al cargar los correos', 'Cerrar', { duration: 4000 }),
        ),
    });
  }

  private stopPolling() {
    this.pollSub?.unsubscribe();
    this.pollSub = null;
  }

  openKeywordConfig() {
    this.dialog.open(KeywordConfigDialogComponent, { width: '660px', maxHeight: '90vh' });
  }

  ngOnDestroy() {
    this.stopPolling();
  }

  monthLabel(month: number): string {
    return this.months.find((m) => m.value === month)?.label ?? '';
  }

  matchReasonLabel(reason: string): string {
    const labels: Record<string, string> = {
      HAS_XML_PDF: 'XML + PDF',
      HAS_ZIP: 'ZIP',
      HAS_XML_ONLY: 'Solo XML',
      HAS_PDF_ONLY: 'Solo PDF',
      KEYWORD_MATCH: 'Keyword',
      KNOWN_INVOICER: 'Emisor conocido',
    };
    return labels[reason] ?? reason;
  }
}
