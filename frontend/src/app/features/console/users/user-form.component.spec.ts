import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { UserFormComponent } from './user-form.component';
import { AdminApiService } from '../../../core/services/admin-api.service';
import { UserResponse } from '../../../core/models/admin-api.models';

describe('UserFormComponent (criação)', () => {
  let fixture: ComponentFixture<UserFormComponent>;
  let adminApiStub: { createUser: jasmine.Spy; getUser: jasmine.Spy };

  beforeEach(async () => {
    adminApiStub = { createUser: jasmine.createSpy('createUser'), getUser: jasmine.createSpy('getUser') };

    await TestBed.configureTestingModule({
      imports: [UserFormComponent],
      providers: [
        provideRouter([]),
        { provide: AdminApiService, useValue: adminApiStub },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ tenantId: '1' }) } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserFormComponent);
    fixture.detectChanges();
  });

  it('não deve carregar usuário nenhum (sem :id na rota)', () => {
    expect(fixture.componentInstance.isEditing).toBe(false);
    expect(adminApiStub.getUser).not.toHaveBeenCalled();
  });

  it('deve chamar createUser e navegar para a lista ao salvar', async () => {
    const router = TestBed.inject(Router);
    const navigateSpy = spyOn(router, 'navigate').and.resolveTo(true);
    adminApiStub.createUser.and.returnValue(of({ id: 1 } as UserResponse));

    fixture.componentInstance.username = 'joao_silva';
    fixture.componentInstance.email = 'joao@acme.com';
    fixture.componentInstance.password = 'senhaSegura123';
    fixture.componentInstance.name = 'João';
    fixture.componentInstance.submit();

    expect(adminApiStub.createUser).toHaveBeenCalledWith(1, {
      username: 'joao_silva',
      email: 'joao@acme.com',
      password: 'senhaSegura123',
      name: 'João',
    });
    expect(navigateSpy).toHaveBeenCalledWith(['/console/tenants', 1, 'users']);
  });

  it('deve mostrar erro quando a criação falha', () => {
    adminApiStub.createUser.and.returnValue(throwError(() => ({ error: { message: 'usuário já existe' } })));
    fixture.componentInstance.submit();
    expect(fixture.componentInstance.errorMessage()).toBe('usuário já existe');
  });
});

describe('UserFormComponent (edição)', () => {
  let fixture: ComponentFixture<UserFormComponent>;
  let adminApiStub: { updateUser: jasmine.Spy; getUser: jasmine.Spy };

  beforeEach(async () => {
    adminApiStub = {
      updateUser: jasmine.createSpy('updateUser'),
      getUser: jasmine
        .createSpy('getUser')
        .and.returnValue(
          of({ id: 2, tenantId: 1, username: 'joao_silva', email: 'joao@acme.com', name: 'João', status: 'ACTIVE' } as UserResponse),
        ),
    };

    await TestBed.configureTestingModule({
      imports: [UserFormComponent],
      providers: [
        provideRouter([]),
        { provide: AdminApiService, useValue: adminApiStub },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ tenantId: '1', id: '2' }) } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserFormComponent);
    fixture.detectChanges();
  });

  it('deve carregar o usuário existente e preencher o formulário', () => {
    expect(fixture.componentInstance.isEditing).toBe(true);
    expect(fixture.componentInstance.username).toBe('joao_silva');
    expect(fixture.componentInstance.email).toBe('joao@acme.com');
  });

  it('deve chamar updateUser (sem username/senha) ao salvar', () => {
    adminApiStub.updateUser.and.returnValue(of({ id: 2 } as UserResponse));

    fixture.componentInstance.name = 'João da Silva';
    fixture.componentInstance.submit();

    expect(adminApiStub.updateUser).toHaveBeenCalledWith(1, 2, { name: 'João da Silva', email: 'joao@acme.com' });
  });
});
