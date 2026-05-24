import { Component, NgZone, OnDestroy, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, LowerCasePipe, SlicePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Subscription, interval, switchMap, takeWhile, tap } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatDialog } from '@angular/material/dialog';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AppIconComponent } from '../../../core/ui/icon/icon.component';
import { AppBtnDirective } from '../../../core/ui/button/button.directive';
import { ToastService } from '../../../core/ui/toast/toast.service';
import { IngestionService } from '../../../core/services/ingestion.service';
import {
  EmailProcessingStatus,
  IngestedEmailResponse,
  JobRunResponse,
} from '../../../core/models/ingestion.model';
import { KeywordConfigDialogComponent } from './keyword-config-dialog/keyword-config-dialog';
import { ManualCfdiDialogComponent } from './manual-cfdi-dialog/manual-cfdi-dialog';
import { MonthlyReportDialogComponent } from './monthly-report-dialog/monthly-report-dialog';

interface ApiError { message: string; code: string; }

@Component({
  selector: 'app-ingestion',
  imports: [
    CurrencyPipe,
    DatePipe,
    LowerCasePipe,
    SlicePipe,
    MatCardModule,
    MatFormFieldModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
    AppIconComponent,
    AppBtnDirective,
  ],
  templateUrl: './ingestion.html',
  styleUrl: './ingestion.css',
})
export class IngestionComponent implements OnDestroy {
  private ingestionService = inject(IngestionService);
  private dialog  = inject(MatDialog);
  private toast   = inject(ToastService);
  private ngZone  = inject(NgZone);

  private pollSub: Subscription | null = null;

  readonly months = [
    { value: 1,  label: 'Enero'      },
    { value: 2,  label: 'Febrero'    },
    { value: 3,  label: 'Marzo'      },
    { value: 4,  label: 'Abril'      },
    { value: 5,  label: 'Mayo'       },
    { value: 6,  label: 'Junio'      },
    { value: 7,  label: 'Julio'      },
    { value: 8,  label: 'Agosto'     },
    { value: 9,  label: 'Septiembre' },
    { value: 10, label: 'Octubre'    },
    { value: 11, label: 'Noviembre'  },
    { value: 12, label: 'Diciembre'  },
  ];

  readonly currentYear  = new Date().getFullYear();
  readonly yearOptions  = Array.from({ length: 5 }, (_, i) => this.currentYear - i);

  selectedMonth = signal(new Date().getMonth() + 1);
  selectedYear  = signal(this.currentYear);
  triggering       = signal(false);
  loadingLatest    = signal(false);
  jobRun           = signal<JobRunResponse | null>(null);
  emails           = signal<IngestedEmailResponse[]>([]);
  apiError         = signal<ApiError | null>(null);
  expandedRow      = signal<IngestedEmailResponse | null>(null);

  readonly isRunning  = computed(() => this.jobRun()?.status === 'RUNNING');
  readonly isBusy     = computed(() => this.triggering() || this.isRunning() || this.loadingLatest());

  readonly displayedColumns = [
    'expand', 'processingStatus', 'subject', 'fromAddress',
    'receivedAt', 'cfdiUuid', 'types', 'matchReasons',
    'errorCause', 'attachments', 'actions',
  ];

  toggleExpand(row: IngestedEmailResponse): void {
    if (row.processingStatus !== 'STORED') return;
    this.expandedRow.set(this.expandedRow() === row ? null : row);
  }

  search() {
    this.stopPolling();
    this.expandedRow.set(null);
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
            if (job.status === 'RUNNING')  this.startPolling(job.id);
            else if (job.status === 'SUCCESS') this.loadEmails(job.targetYear, job.targetMonth);
          });
        },
        error: (err: HttpErrorResponse) => {
          this.ngZone.run(() => {
            this.triggering.set(false);
            const body = err.error as Partial<ApiError> | null;
            if (body?.code && body?.message) this.apiError.set({ code: body.code, message: body.message });
            else this.toast.error('Error al iniciar la búsqueda');
          });
        },
      });
  }

  viewLatest() {
    this.stopPolling();
    this.expandedRow.set(null);
    this.jobRun.set(null);
    this.emails.set([]);
    this.apiError.set(null);
    this.loadingLatest.set(true);

    this.ingestionService
      .getLatestSuccessfulJob(this.selectedYear(), this.selectedMonth())
      .subscribe({
        next: (job) => {
          this.ngZone.run(() => {
            this.loadingLatest.set(false);
            if (job) {
              this.jobRun.set(job);
              this.loadEmails(job.targetYear, job.targetMonth);
            } else {
              this.toast.warning('No hay ingestas exitosas previas para este período.');
            }
          });
        },
        error: () => {
          this.ngZone.run(() => {
            this.loadingLatest.set(false);
            this.toast.error('Error al consultar la última ingesta');
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
              this.toast.error(`Búsqueda fallida: ${job.errorMessage ?? 'Error desconocido'}`, 6000)
            );
          }
        },
        error: () => this.ngZone.run(() => this.toast.error('Error al consultar el estado del proceso')),
      });
  }

  private loadEmails(year: number, month: number) {
    this.ingestionService.findEmails(year, month).subscribe({
      next: (emails) => this.ngZone.run(() => this.emails.set(emails)),
      error: () => this.ngZone.run(() => this.toast.error('Error al cargar los correos')),
    });
  }

  private stopPolling() {
    this.pollSub?.unsubscribe();
    this.pollSub = null;
  }

  openKeywordConfig() {
    this.dialog.open(KeywordConfigDialogComponent, {
      width: '680px',
      maxHeight: '90vh',
      panelClass: ['nx-dialog'],
      backdropClass: 'nx-backdrop',
    });
  }

  openManualEntry() {
    const ref = this.dialog.open(ManualCfdiDialogComponent, {
      width: '600px',
      maxHeight: '90vh',
      panelClass: ['nx-dialog'],
      backdropClass: 'nx-backdrop',
      data: { year: this.selectedYear(), month: this.selectedMonth() },
    });
    ref.afterClosed().subscribe((result: IngestedEmailResponse | null | undefined) => {
      if (result) {
        this.emails.update((list) => [result, ...list]);
        this.toast.success('CFDI manual agregado correctamente');
      }
    });
  }

  openEditEntry(row: IngestedEmailResponse) {
    const ref = this.dialog.open(ManualCfdiDialogComponent, {
      width: '600px',
      maxHeight: '90vh',
      panelClass: ['nx-dialog'],
      backdropClass: 'nx-backdrop',
      data: { year: this.selectedYear(), month: this.selectedMonth(), editMode: true, email: row },
    });
    ref.afterClosed().subscribe((result: IngestedEmailResponse | null | undefined) => {
      if (result) {
        this.emails.update((list) => list.map((e) => (e.id === result.id ? result : e)));
        this.toast.success('CFDI actualizado correctamente');
      }
    });
  }

  hasStoredAttachment(row: IngestedEmailResponse, type: 'pdf' | 'xml'): boolean {
    return row.attachments.some((a) => a.extension.toLowerCase() === type && a.storageKey != null);
  }

  previewFile(row: IngestedEmailResponse, type: 'pdf' | 'xml') {
    const attachment = row.attachments.find(
      (a) => a.extension.toLowerCase() === type && a.storageKey != null
    );
    if (!attachment) {
      this.toast.warning(`No hay archivo ${type.toUpperCase()} disponible.`);
      return;
    }
    this.ingestionService.getAttachmentPreviewUrl(row.id, attachment.id).subscribe({
      next: (res) => window.open(res.url, '_blank'),
      error: () => this.toast.error('Error al generar la vista previa.'),
    });
  }

  openMonthlyReport() {
    const ref = this.dialog.open(MonthlyReportDialogComponent, {
      width: '560px',
      maxHeight: '90vh',
      panelClass: ['nx-dialog'],
      backdropClass: 'nx-backdrop',
      data: { year: this.selectedYear(), month: this.selectedMonth() },
    });
    ref.afterClosed().subscribe((success: boolean | undefined) => {
      if (success) this.toast.success('Reporte enviado correctamente');
    });
  }

  ngOnDestroy() { this.stopPolling(); }

  monthLabel(month: number): string {
    return this.months.find((m) => m.value === month)?.label ?? '';
  }

  matchReasonLabel(reason: string): string {
    const labels: Record<string, string> = {
      HAS_XML_PDF:    'XML + PDF',
      HAS_ZIP:        'ZIP',
      HAS_XML_ONLY:   'Solo XML',
      HAS_PDF_ONLY:   'Solo PDF',
      KEYWORD_MATCH:  'Keyword',
      KNOWN_INVOICER: 'Emisor',
      SENDER_MATCH:   'Remitente',
    };
    return labels[reason] ?? reason;
  }

  statusLabel(status: EmailProcessingStatus): string {
    const labels: Record<EmailProcessingStatus, string> = {
      STORED: 'Almacenado', ERROR: 'Error', PENDING: 'Pendiente',
    };
    return labels[status] ?? status;
  }

  storedCount():  number { return this.emails().filter((e) => e.processingStatus === 'STORED').length; }
  errorCount():   number { return this.emails().filter((e) => e.processingStatus === 'ERROR').length; }
  storedAttachments(row: IngestedEmailResponse): number {
    return row.attachments.filter((a) => a.storageKey != null).length;
  }
}
