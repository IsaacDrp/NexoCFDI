import { Component, NgZone, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { KeywordService } from '../../../../core/services/keyword.service';
import { KeywordType, UserKeyword } from '../../../../core/models/keyword.model';

@Component({
  selector: 'app-keyword-config-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatTabsModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './keyword-config-dialog.html',
  styleUrl: './keyword-config-dialog.css',
})
export class KeywordConfigDialogComponent implements OnInit {
  private keywordService = inject(KeywordService);
  private snackBar = inject(MatSnackBar);
  private ngZone = inject(NgZone);
  private fb = inject(FormBuilder);

  loading = signal(true);
  savingInclude = signal(false);
  savingExclude = signal(false);
  savingSenderInclude = signal(false);
  savingSenderExclude = signal(false);
  deletingId = signal<string | null>(null);

  includeKeywords = signal<UserKeyword[]>([]);
  excludeKeywords = signal<UserKeyword[]>([]);
  senderIncludeKeywords = signal<UserKeyword[]>([]);
  senderExcludeKeywords = signal<UserKeyword[]>([]);

  includeForm = this.fb.group({ phrase: ['', [Validators.required, Validators.maxLength(255)]] });
  excludeForm = this.fb.group({ phrase: ['', [Validators.required, Validators.maxLength(255)]] });
  senderIncludeForm = this.fb.group({ phrase: ['', [Validators.required, Validators.maxLength(255)]] });
  senderExcludeForm = this.fb.group({ phrase: ['', [Validators.required, Validators.maxLength(255)]] });

  ngOnInit() {
    this.load();
  }

  private load() {
    this.loading.set(true);
    this.keywordService.findAll().subscribe({
      next: (kws) => {
        this.ngZone.run(() => {
          this.includeKeywords.set(kws.filter((k) => k.type === 'INCLUDE'));
          this.excludeKeywords.set(kws.filter((k) => k.type === 'EXCLUDE'));
          this.senderIncludeKeywords.set(kws.filter((k) => k.type === 'SENDER_INCLUDE'));
          this.senderExcludeKeywords.set(kws.filter((k) => k.type === 'SENDER_EXCLUDE'));
          this.loading.set(false);
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.snackBar.open('Error al cargar las palabras clave', 'Cerrar', { duration: 4000 });
          this.loading.set(false);
        });
      },
    });
  }

  addKeyword(type: KeywordType) {
    const formMap = {
      INCLUDE: this.includeForm,
      EXCLUDE: this.excludeForm,
      SENDER_INCLUDE: this.senderIncludeForm,
      SENDER_EXCLUDE: this.senderExcludeForm,
    };
    const savingMap = {
      INCLUDE: this.savingInclude,
      EXCLUDE: this.savingExclude,
      SENDER_INCLUDE: this.savingSenderInclude,
      SENDER_EXCLUDE: this.savingSenderExclude,
    };
    const listMap = {
      INCLUDE: this.includeKeywords,
      EXCLUDE: this.excludeKeywords,
      SENDER_INCLUDE: this.senderIncludeKeywords,
      SENDER_EXCLUDE: this.senderExcludeKeywords,
    };

    const form = formMap[type];
    if (form.invalid) {
      form.markAllAsTouched();
      return;
    }

    const saving = savingMap[type];
    saving.set(true);

    this.keywordService.create({ phrase: form.value.phrase!.trim(), type }).subscribe({
      next: (kw) => {
        this.ngZone.run(() => {
          listMap[type].update((list) => [...list, kw]);
          form.reset();
          saving.set(false);
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          saving.set(false);
          const isSender = type === 'SENDER_INCLUDE' || type === 'SENDER_EXCLUDE';
          const msg =
            err?.error?.code === 'DUPLICATE_KEYWORD'
              ? isSender
                ? 'Esa dirección ya está en la lista'
                : 'Esa palabra clave ya existe'
              : isSender
                ? 'Error al guardar la dirección'
                : 'Error al guardar la palabra clave';
          this.snackBar.open(msg, 'Cerrar', { duration: 4000 });
        });
      },
    });
  }

  deleteKeyword(kw: UserKeyword) {
    this.deletingId.set(kw.id);
    const listMap = {
      INCLUDE: this.includeKeywords,
      EXCLUDE: this.excludeKeywords,
      SENDER_INCLUDE: this.senderIncludeKeywords,
      SENDER_EXCLUDE: this.senderExcludeKeywords,
    };
    this.keywordService.delete(kw.id).subscribe({
      next: () => {
        this.ngZone.run(() => {
          listMap[kw.type].update((list) => list.filter((k) => k.id !== kw.id));
          this.deletingId.set(null);
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.deletingId.set(null);
          this.snackBar.open('Error al eliminar', 'Cerrar', { duration: 4000 });
        });
      },
    });
  }
}
