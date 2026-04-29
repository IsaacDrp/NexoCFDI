import { Component, NgZone, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UserService } from '../../../core/services/user.service';
import {
  PERSON_TYPE_LABELS,
  PersonType,
  RegisterUserRequest,
  RegimenFiscalOption,
  getRegimenesByPersonType,
} from '../../../core/models/user.model';

@Component({
  selector: 'app-profile',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class ProfileComponent implements OnInit {
  private userService = inject(UserService);
  private fb = inject(FormBuilder);
  private snackBar = inject(MatSnackBar);
  private ngZone = inject(NgZone);

  loading = signal(true);
  saving = signal(false);
  isNewUser = signal(true);

  private selectedPersonType = signal<PersonType | null>(null);

  personTypeOptions: { value: PersonType; label: string }[] = [
    { value: 'FISICA', label: PERSON_TYPE_LABELS['FISICA'] },
    { value: 'MORAL', label: PERSON_TYPE_LABELS['MORAL'] },
  ];

  filteredRegimes = computed<RegimenFiscalOption[]>(() => {
    const pt = this.selectedPersonType();
    return pt ? getRegimenesByPersonType(pt) : [];
  });

  form = this.fb.group({
    firstName: ['', Validators.required],
    paternalSurname: ['', Validators.required],
    maternalSurname: [''],
    rfc: ['', [Validators.required, Validators.pattern(/^[A-Z&Ñ]{3,4}\d{6}[A-Z0-9]{3}$/)]],
    razonSocial: ['', Validators.required],
    postalCode: ['', [Validators.required, Validators.pattern(/^\d{5}$/)]],
    personType: [null as PersonType | null, Validators.required],
    regimenFiscal: [{ value: null as string | null, disabled: true }, Validators.required],
  });

  ngOnInit() {
    this.form.get('personType')!.valueChanges.subscribe((pt) => {
      this.ngZone.run(() => {
        this.selectedPersonType.set(pt as PersonType | null);
        const rfCtrl = this.form.get('regimenFiscal')!;
        rfCtrl.setValue(null);
        pt ? rfCtrl.enable() : rfCtrl.disable();
      });
    });

    this.userService.getProfile().subscribe({
      next: (user) => {
        this.ngZone.run(() => {
          this.form.patchValue({
            ...user,
            personType: user.personType ?? null,
            regimenFiscal: user.regimenFiscal ?? null,
          });
          if (user.personType) {
            this.selectedPersonType.set(user.personType);
            this.form.get('regimenFiscal')!.enable();
          }
          this.isNewUser.set(false);
          this.loading.set(false);
        });
      },
      error: (err: HttpErrorResponse) => {
        this.ngZone.run(() => {
          if (err.status !== 404) {
            this.snackBar.open('Error al cargar el perfil', 'Cerrar', { duration: 4000 });
          }
          this.loading.set(false);
        });
      },
    });
  }

  save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    const v = this.form.getRawValue();

    const request: RegisterUserRequest = {
      firstName: v.firstName!,
      paternalSurname: v.paternalSurname!,
      maternalSurname: v.maternalSurname ?? '',
      rfc: v.rfc!,
      razonSocial: v.razonSocial!,
      postalCode: v.postalCode!,
      personType: v.personType!,
      regimenFiscal: v.regimenFiscal!,
    };

    this.userService.saveProfile(request).subscribe({
      next: (user) => {
        this.ngZone.run(() => {
          this.form.patchValue(user);
          this.form.markAsPristine();
          this.isNewUser.set(false);
          this.saving.set(false);
          this.snackBar.open('Perfil guardado correctamente', 'Cerrar', { duration: 3000 });
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.saving.set(false);
          this.snackBar.open('Error al guardar el perfil', 'Cerrar', { duration: 4000 });
        });
      },
    });
  }
}
