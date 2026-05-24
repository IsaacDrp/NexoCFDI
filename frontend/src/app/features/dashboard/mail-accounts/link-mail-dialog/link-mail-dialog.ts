import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AppBtnDirective } from '../../../../core/ui/button/button.directive';

@Component({
  selector: 'app-link-mail-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, AppBtnDirective],
  templateUrl: './link-mail-dialog.html',
  styleUrl: './link-mail-dialog.css',
})
export class LinkMailDialogComponent {
  private fb        = inject(FormBuilder);
  private dialogRef = inject(MatDialogRef<LinkMailDialogComponent>);

  form = this.fb.group({ displayName: ['', Validators.required] });

  confirm() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.dialogRef.close(this.form.value.displayName);
  }
}
