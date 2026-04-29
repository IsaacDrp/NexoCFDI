import { Component, NgZone, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { IngestionService } from '../../../../core/services/ingestion.service';
import { IngestedEmailResponse } from '../../../../core/models/ingestion.model';

export interface ManualCfdiDialogData {
  year: number;
  month: number;
  /** Modo edición: pre-pobla el formulario y llama PUT en lugar de POST */
  editMode?: boolean;
  email?: IngestedEmailResponse;
}

@Component({
  selector: 'app-manual-cfdi-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './manual-cfdi-dialog.html',
  styleUrl: './manual-cfdi-dialog.css',
})
export class ManualCfdiDialogComponent {
  private ingestionService = inject(IngestionService);
  private dialogRef = inject(MatDialogRef<ManualCfdiDialogComponent>);
  private data = inject<ManualCfdiDialogData>(MAT_DIALOG_DATA);
  private snackBar = inject(MatSnackBar);
  private ngZone = inject(NgZone);
  private fb = inject(FormBuilder);

  saving = signal(false);
  selectedFile = signal<File | null>(null);

  readonly editMode = this.data.editMode ?? false;
  readonly existingEmail = this.data.email ?? null;
  readonly year = this.data.year;
  readonly month = this.data.month;

  /** Acepta PDF, ZIP o XML en modo edición (XML solo si no tiene XML todavía) */
  get acceptedFileTypes(): string {
    if (this.editMode) return '.pdf,.xml,.zip';
    return '.pdf,.zip';
  }

  /** Muestra el file picker si:
   *  - No es modo edición (siempre en CFDI manual nuevo)
   *  - Es modo edición y le falta PDF o XML
   */
  get showFilePicker(): boolean {
    if (!this.editMode) return true;
    return !(this.existingEmail?.hasPdf) || !(this.existingEmail?.hasXml) || this.replaceFile();
  }

  replaceFile = signal(false);

  previewStoredPdf() {
    if (!this.existingEmail) return;
    const att = this.existingEmail.attachments.find(a => a.extension.toLowerCase() === 'pdf' && a.storageKey != null);
    if (!att) return;
    this.ingestionService.getAttachmentPreviewUrl(this.existingEmail.id, att.id).subscribe({
      next: (res) => window.open(res.url, '_blank'),
      error: () => this.snackBar.open('Error al generar vista previa', 'Cerrar', { duration: 3000 })
    });
  }

  hasStoredPdf(): boolean {
    if (!this.existingEmail) return false;
    return this.existingEmail.attachments.some(a => a.extension.toLowerCase() === 'pdf' && a.storageKey != null);
  }

  form = this.fb.group({
    rfcEmisor:    [this.editMode ? (this.existingEmail?.cfdiRfcEmisor ?? '') : '',
                   [Validators.required, Validators.maxLength(13)]],
    nombreEmisor: [this.editMode ? (this.existingEmail?.cfdiNombreEmisor ?? '') : '',
                   Validators.maxLength(300)],
    cfdiUuid:     [this.editMode ? (this.existingEmail?.cfdiUuid ?? '') : '',
                   Validators.maxLength(36)],
    fecha:        [this.editMode ? this._isoToDatetimeLocal(this.existingEmail?.cfdiFecha) : '',
                   Validators.required],
    subtotal:     [this.editMode ? (this.existingEmail?.cfdiSubtotal ?? null) : null as number | null],
    iva:          [this.editMode ? (this.existingEmail?.cfdiIva      ?? null) : null as number | null],
    total:        [this.editMode ? (this.existingEmail?.cfdiTotal     ?? null) : null as number | null],
  });

  private _isoToDatetimeLocal(iso: string | null | undefined): string {
    if (!iso) return '';
    // "2024-03-15T12:00:00" → compatible con datetime-local input
    return iso.length >= 16 ? iso.substring(0, 16) : iso;
  }

  onFileChange(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    if (file) {
      const ext = file.name.split('.').pop()?.toLowerCase();
      const allowed = this.editMode ? ['pdf', 'xml', 'zip'] : ['pdf', 'zip'];
      if (!ext || !allowed.includes(ext)) {
        const types = this.editMode ? 'PDF, XML o ZIP' : 'PDF o ZIP';
        this.snackBar.open(`Solo se aceptan archivos ${types}`, 'Cerrar', { duration: 3000 });
        input.value = '';
        this.selectedFile.set(null);
        return;
      }
    }
    this.selectedFile.set(file);
  }

  removeFile() {
    this.selectedFile.set(null);
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.value;
    const fd = new FormData();
    if (v.rfcEmisor)    fd.append('rfcEmisor',    v.rfcEmisor.trim().toUpperCase());
    if (v.nombreEmisor) fd.append('nombreEmisor',  v.nombreEmisor.trim());
    if (v.cfdiUuid)     fd.append('cfdiUuid',      v.cfdiUuid.trim());
    if (v.fecha)        fd.append('fecha',          v.fecha);
    if (v.subtotal != null) fd.append('subtotal', String(v.subtotal));
    if (v.iva != null)      fd.append('iva',       String(v.iva));
    if (v.total != null)    fd.append('total',     String(v.total));
    const file = this.selectedFile();
    if (file) fd.append('file', file, file.name);

    this.saving.set(true);

    const obs$ = this.editMode && this.existingEmail
      ? this.ingestionService.updateEmail(this.existingEmail.id, fd)
      : this.ingestionService.addManualEntry(fd);

    obs$.subscribe({
      next: (result: IngestedEmailResponse) => {
        this.ngZone.run(() => {
          this.saving.set(false);
          this.dialogRef.close(result);
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.saving.set(false);
          const msg = this.editMode
            ? 'Error al guardar los cambios'
            : 'Error al guardar el CFDI manual';
          this.snackBar.open(msg, 'Cerrar', { duration: 4000 });
        });
      },
    });
  }
}
