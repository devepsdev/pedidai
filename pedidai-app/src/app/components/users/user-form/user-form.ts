import { Component, inject, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { UserService } from '../../../services/user';
import { UserRequest } from '../../../models/user.model';

@Component({
  selector: 'app-user-form',
  imports: [ReactiveFormsModule, TranslateModule],
  templateUrl: './user-form.html',
})
export class UserForm implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private userService = inject(UserService);

  loading = signal(false);
  saving = signal(false);
  error = signal('');
  uuid = signal<string | null>(null);

  form = this.fb.group({
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    role: ['USER', [Validators.required]],
    password: ['', [Validators.minLength(8)]],
  });

  get isEdit() { return !!this.uuid(); }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('uuid');
    if (id) {
      this.uuid.set(id);
      this.loading.set(true);
      this.userService.getByUuid(id).subscribe({
        next: (u) => {
          this.form.patchValue({
            firstName: u.firstName,
            lastName: u.lastName,
            email: u.email,
            phone: u.phone ?? '',
            role: u.role,
          });
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Error al cargar el usuario');
          this.loading.set(false);
        },
      });
    } else {
      this.form.get('password')!.addValidators(Validators.required);
      this.form.get('password')!.updateValueAndValidity();
    }
  }

  save() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const data: UserRequest = {
      firstName: raw.firstName!,
      lastName: raw.lastName!,
      email: raw.email!,
      phone: raw.phone || undefined,
      role: raw.role!,
      ...(!this.isEdit && raw.password ? { password: raw.password } : {}),
    };

    this.saving.set(true);
    this.error.set('');

    const obs = this.isEdit
      ? this.userService.update(this.uuid()!, data)
      : this.userService.create(data);

    obs.subscribe({
      next: () => {
        this.saving.set(false);
        this.router.navigate(['/users']);
      },
      error: (err) => {
        const msg = err?.error?.message;
        this.error.set(msg || 'Error al guardar el usuario');
        this.saving.set(false);
      },
    });
  }

  cancel() {
    this.router.navigate(['/users']);
  }

  fieldError(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && ctrl?.touched);
  }
}
