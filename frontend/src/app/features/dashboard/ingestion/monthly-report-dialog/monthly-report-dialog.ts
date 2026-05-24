import { Component, NgZone, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AppBtnDirective } from '../../../../core/ui/button/button.directive';
import { AppIconComponent } from '../../../../core/ui/icon/icon.component';
import { ToastService } from '../../../../core/ui/toast/toast.service';
import { ReportingService } from '../../../../core/services/reporting.service';

export interface MonthlyReportDialogData {
  year: number;
  month: number;
}

@Component({
  selector: 'app-monthly-report-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    AppBtnDirective,
    AppIconComponent,
  ],
  templateUrl: './monthly-report-dialog.html',
  styleUrl: './monthly-report-dialog.css',
})
export class MonthlyReportDialogComponent {
  private reportingService = inject(ReportingService);
  private dialogRef        = inject(MatDialogRef<MonthlyReportDialogComponent>);
  public  data             = inject<MonthlyReportDialogData>(MAT_DIALOG_DATA);
  private toast            = inject(ToastService);
  private ngZone           = inject(NgZone);
  private fb               = inject(FormBuilder);

  saving      = signal(false);
  ingresoFiles = signal<File[]>([]);

  form = this.fb.group({
    emailDestino:         ['', [Validators.required, Validators.email]],
    asunto:               ['', [Validators.required, Validators.maxLength(200)]],
    mensajePersonalizado: ['', [Validators.maxLength(1000)]],
  });

  onFilesSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;
    const newFiles = Array.from(input.files).filter(f => f.type === 'application/pdf');
    this.ingresoFiles.update(prev => [...prev, ...newFiles]);
    input.value = '';
  }

  removeFile(index: number) {
    this.ingresoFiles.update(prev => prev.filter((_, i) => i !== index));
  }

  submit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    this.saving.set(true);
    const v = this.form.value;

    this.reportingService.sendMonthlyReport(
      this.data.year,
      this.data.month,
      {
        emailDestino:         v.emailDestino!,
        asunto:               v.asunto!,
        mensajePersonalizado: v.mensajePersonalizado!,
      },
      this.ingresoFiles(),
    ).subscribe({
      next: () => {
        this.ngZone.run(() => { this.saving.set(false); this.dialogRef.close(true); });
      },
      error: () => {
        this.ngZone.run(() => { this.saving.set(false); this.toast.error('Error al enviar el reporte'); });
      },
    });
  }
}
