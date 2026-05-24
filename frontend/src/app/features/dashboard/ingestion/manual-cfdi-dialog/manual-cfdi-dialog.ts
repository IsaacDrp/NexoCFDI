import { Component, NgZone, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AppBtnDirective } from '../../../../core/ui/button/button.directive';
import { AppIconComponent } from '../../../../core/ui/icon/icon.component';
import { ToastService } from '../../../../core/ui/toast/toast.service';
import { IngestionService } from '../../../../core/services/ingestion.service';
import { IngestedEmailResponse } from '../../../../core/models/ingestion.model';

export interface ManualCfdiDialogData {
  year: number;
  month: number;
  editMode?: boolean;
  email?: IngestedEmailResponse;
}

@Component({
  selector: 'app-manual-cfdi-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    AppBtnDirective,
    AppIconComponent,
  ],
  templateUrl: './manual-cfdi-dialog.html',
  styleUrl: './manual-cfdi-dialog.css',
})
export class ManualCfdiDialogComponent {
  private ingestionService = inject(IngestionService);
  private dialogRef        = inject(MatDialogRef<ManualCfdiDialogComponent>);
  private data             = inject<ManualCfdiDialogData>(MAT_DIALOG_DATA);
  private toast            = inject(ToastService);
  private ngZone           = inject(NgZone);
  private fb               = inject(FormBuilder);

  saving       = signal(false);
  selectedFile = signal<File | null>(null);
  replaceFile  = signal(false);

  readonly editMode      = this.data.editMode ?? false;
  readonly existingEmail = this.data.email ?? null;
  readonly year          = this.data.year;
  readonly month         = this.data.month;

  get acceptedFileTypes(): string {
    return this.editMode ? '.pdf,.xml,.zip' : '.pdf,.zip';
  }

  get showFilePicker(): boolean {
    if (!this.editMode) return true;
    return !(this.existingEmail?.hasPdf) || !(this.existingEmail?.hasXml) || this.replaceFile();
  }

  form = this.fb.group({
    rfcEmisor:    [this.editMode ? (this.existingEmail?.cfdiRfcEmisor    ?? '') : '', [Validators.required, Validators.maxLength(13)]],
    nombreEmisor: [this.editMode ? (this.existingEmail?.cfdiNombreEmisor ?? '') : '', Validators.maxLength(300)],
    cfdiUuid:     [this.editMode ? (this.existingEmail?.cfdiUuid         ?? '') : '', Validators.maxLength(36)],
    fecha:        [this.editMode ? this._isoToDatetimeLocal(this.existingEmail?.cfdiFecha) : '', Validators.required],
    subtotal:     [this.editMode ? (this.existingEmail?.cfdiSubtotal?.toString() ?? '') : ''],
    iva:          [this.editMode ? (this.existingEmail?.cfdiIva?.toString()      ?? '') : ''],
    total:        [this.editMode ? (this.existingEmail?.cfdiTotal?.toString()    ?? '') : ''],
  });

  private _isoToDatetimeLocal(iso: string | null | undefined): string {
    if (!iso) return '';
    return iso.length >= 16 ? iso.substring(0, 16) : iso;
  }

  previewStoredPdf() {
    if (!this.existingEmail) return;
    const att = this.existingEmail.attachments.find(a => a.extension.toLowerCase() === 'pdf' && a.storageKey != null);
    if (!att) return;
    this.ingestionService.getAttachmentPreviewUrl(this.existingEmail.id, att.id).subscribe({
      next: (res) => window.open(res.url, '_blank'),
      error: () => this.toast.error('Error al generar vista previa'),
    });
  }

  hasStoredPdf(): boolean {
    if (!this.existingEmail) return false;
    return this.existingEmail.attachments.some(a => a.extension.toLowerCase() === 'pdf' && a.storageKey != null);
  }

  onFileChange(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    if (file) {
      const ext = file.name.split('.').pop()?.toLowerCase();
      const allowed = this.editMode ? ['pdf', 'xml', 'zip'] : ['pdf', 'zip'];
      if (!ext || !allowed.includes(ext)) {
        const types = this.editMode ? 'PDF, XML o ZIP' : 'PDF o ZIP';
        this.toast.warning(`Solo se aceptan archivos ${types}`);
        input.value = '';
        this.selectedFile.set(null);
        return;
      }
    }
    this.selectedFile.set(file);
  }

  removeFile() { this.selectedFile.set(null); }

  submit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    const v = this.form.value;
    const fd = new FormData();
    if (v.rfcEmisor)         fd.append('rfcEmisor',    v.rfcEmisor.trim().toUpperCase());
    if (v.nombreEmisor)      fd.append('nombreEmisor',  v.nombreEmisor.trim());
    if (v.cfdiUuid)          fd.append('cfdiUuid',      v.cfdiUuid.trim());
    if (v.fecha)             fd.append('fecha',          v.fecha);
    const toDecimal = (s: string | null | undefined) => s?.trim().replace(/,/g, '') || null;
    const sub = toDecimal(v.subtotal);
    const iva = toDecimal(v.iva);
    const tot = toDecimal(v.total);
    if (sub) fd.append('subtotal', sub);
    if (iva) fd.append('iva',      iva);
    if (tot) fd.append('total',    tot);
    const file = this.selectedFile();
    if (file) fd.append('file', file, file.name);

    this.saving.set(true);

    const obs$ = this.editMode && this.existingEmail
      ? this.ingestionService.updateEmail(this.existingEmail.id, fd)
      : this.ingestionService.addManualEntry(fd);

    obs$.subscribe({
      next: (result: IngestedEmailResponse) => {
        this.ngZone.run(() => { this.saving.set(false); this.dialogRef.close(result); });
      },
      error: () => {
        this.ngZone.run(() => {
          this.saving.set(false);
          this.toast.error(this.editMode ? 'Error al guardar los cambios' : 'Error al guardar el CFDI manual');
        });
      },
    });
  }
}
