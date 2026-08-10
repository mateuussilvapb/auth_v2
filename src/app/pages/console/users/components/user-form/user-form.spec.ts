import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import type { Mock } from 'vitest';

import { UserForm } from './user-form';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { UserResponse } from '../../../../../core/models/admin-api.models';

describe('UserForm (criação)', () => {
  let fixture: ComponentFixture<UserForm>;
  let adminApiStub: { createUser: Mock; getUser: Mock };

  beforeEach(async () => {
    adminApiStub = { createUser: vi.fn(), getUser: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [UserForm],
      providers: [
        provideRouter([{ path: '**', component: UserForm }]),
        { provide: AdminApiService, useValue: adminApiStub },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ tenantId: '1' }) } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserForm);
    fixture.detectChanges();
  });

  it('não deve carregar usuário nenhum (sem :id na rota)', () => {
    expect(fixture.componentInstance.isEditing).toBe(false);
    expect(adminApiStub.getUser).not.toHaveBeenCalled();
  });

  it('deve chamar createUser e navegar para a lista ao salvar', async () => {
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    adminApiStub.createUser.mockReturnValue(of({ id: '1' } as UserResponse));

    fixture.componentInstance.username = 'joao_silva';
    fixture.componentInstance.email = 'joao@acme.com';
    fixture.componentInstance.password = 'senhaSegura123';
    fixture.componentInstance.name = 'João';
    fixture.componentInstance.submit();

    expect(adminApiStub.createUser).toHaveBeenCalledWith('1', {
      username: 'joao_silva',
      email: 'joao@acme.com',
      password: 'senhaSegura123',
      name: 'João',
    });
    expect(navigateSpy).toHaveBeenCalledWith(['/console/tenants', '1', 'users']);
  });

  it('deve mostrar erro quando a criação falha', () => {
    adminApiStub.createUser.mockReturnValue(throwError(() => ({ error: { message: 'usuário já existe' } })));
    fixture.componentInstance.submit();
    expect(fixture.componentInstance.errorMessage()).toBe('usuário já existe');
  });
});

describe('UserForm (edição)', () => {
  let fixture: ComponentFixture<UserForm>;
  let adminApiStub: { updateUser: Mock; getUser: Mock };

  beforeEach(async () => {
    adminApiStub = {
      updateUser: vi.fn(),
      getUser: vi
        .fn()
        .mockReturnValue(
          of({ id: '2', tenantId: '1', username: 'joao_silva', email: 'joao@acme.com', name: 'João', status: 'ACTIVE' } as UserResponse),
        ),
    };

    await TestBed.configureTestingModule({
      imports: [UserForm],
      providers: [
        provideRouter([{ path: '**', component: UserForm }]),
        { provide: AdminApiService, useValue: adminApiStub },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ tenantId: '1', id: '2' }) } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserForm);
    fixture.detectChanges();
  });

  it('deve carregar o usuário existente e preencher o formulário', () => {
    expect(fixture.componentInstance.isEditing).toBe(true);
    expect(fixture.componentInstance.username).toBe('joao_silva');
    expect(fixture.componentInstance.email).toBe('joao@acme.com');
  });

  it('deve chamar updateUser (sem username/senha) ao salvar', () => {
    adminApiStub.updateUser.mockReturnValue(of({ id: '2' } as UserResponse));

    fixture.componentInstance.name = 'João da Silva';
    fixture.componentInstance.submit();

    expect(adminApiStub.updateUser).toHaveBeenCalledWith('1', '2', { name: 'João da Silva', email: 'joao@acme.com' });
  });
});
