import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ConfirmationService, MessageService as MessageServicePG } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import type { Mock } from 'vitest';

import { UserForm } from './user-form';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { MessageService } from '../../../../../shared/services/message.service';
import { UserResponse } from '../../../../../core/models/admin-api.models';

describe('UserForm', () => {
  let fixture: ComponentFixture<UserForm>;
  let adminApiStub: { getUser: Mock; createUser: Mock; updateUser: Mock };
  let router: Router;
  let messageService: MessageService;

  const user: UserResponse = {
    id: '1',
    tenantId: 't1',
    username: 'joao_silva',
    email: 'joao@acme.com',
    name: 'João Silva',
    status: 'ACTIVE',
  };

  async function setup(routeData: Record<string, unknown>, paramMap: Record<string, string> = {}): Promise<void> {
    adminApiStub = {
      getUser: vi.fn().mockReturnValue(of(user)),
      createUser: vi.fn().mockReturnValue(of(user)),
      updateUser: vi.fn().mockReturnValue(of(user)),
    };

    await TestBed.configureTestingModule({
      imports: [UserForm],
      providers: [
        provideRouter([]),
        ConfirmationService,
        DialogService,
        MessageServicePG,
        { provide: AdminApiService, useValue: adminApiStub },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: new Map(Object.entries(paramMap)), data: routeData } },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    messageService = TestBed.inject(MessageService);

    fixture = TestBed.createComponent(UserForm);
    fixture.detectChanges();
  }

  it('modo criação: exige username/senha e cria o usuário', async () => {
    await setup({ formMode: 'cadastro' }, { tenantId: 't1' });

    expect(fixture.componentInstance.isCreateMode()).toBe(true);
    expect(fixture.componentInstance.form.get('password')).not.toBeNull();

    fixture.componentInstance.form.setValue({
      username: 'maria',
      name: 'Maria',
      email: 'maria@acme.com',
      password: 'senhaForte1',
    });
    await fixture.componentInstance.submit();

    expect(adminApiStub.createUser).toHaveBeenCalledWith('t1', {
      username: 'maria',
      email: 'maria@acme.com',
      password: 'senhaForte1',
      name: 'Maria',
    });
    expect(router.navigate).toHaveBeenCalledWith(['/console/tenants', 't1', 'users']);
  });

  it('modo edição: carrega o usuário, desabilita username e não tem campo de senha', async () => {
    await setup({ formMode: 'edicao' }, { tenantId: 't1', id: '1' });

    expect(fixture.componentInstance.isEditMode()).toBe(true);
    expect(adminApiStub.getUser).toHaveBeenCalledWith('t1', '1');
    expect(fixture.componentInstance.form.get('username')?.disabled).toBe(true);
    expect(fixture.componentInstance.form.get('password')).toBeNull();
    expect(fixture.componentInstance.form.getRawValue()).toEqual({
      username: 'joao_silva',
      name: 'João Silva',
      email: 'joao@acme.com',
    });

    fixture.componentInstance.form.patchValue({ name: 'João S. Silva' });
    await fixture.componentInstance.submit();

    expect(adminApiStub.updateUser).toHaveBeenCalledWith('t1', '1', { name: 'João S. Silva', email: 'joao@acme.com' });
    expect(router.navigate).toHaveBeenCalledWith(['/console/tenants', 't1', 'users']);
  });

  it('deve mostrar erro quando falha ao carregar o usuário em modo edição', async () => {
    adminApiStub = {
      getUser: vi.fn().mockReturnValue(throwError(() => new Error('falhou'))),
      createUser: vi.fn(),
      updateUser: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [UserForm],
      providers: [
        provideRouter([]),
        ConfirmationService,
        DialogService,
        MessageServicePG,
        { provide: AdminApiService, useValue: adminApiStub },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: new Map([['tenantId', 't1'], ['id', '1']]), data: { formMode: 'edicao' } } },
        },
      ],
    }).compileComponents();

    messageService = TestBed.inject(MessageService);
    const errorSpy = vi.spyOn(messageService, 'showError');

    fixture = TestBed.createComponent(UserForm);
    fixture.detectChanges();
    await new Promise((resolve) => setTimeout(resolve));

    expect(errorSpy).toHaveBeenCalledWith('Não foi possível carregar o usuário.');
  });

  it('deve mostrar a mensagem de erro da API quando falha ao salvar', async () => {
    await setup({ formMode: 'cadastro' }, { tenantId: 't1' });
    adminApiStub.createUser.mockReturnValue(throwError(() => ({ error: { message: 'username já em uso.' } })));
    const errorSpy = vi.spyOn(messageService, 'showError');

    fixture.componentInstance.form.setValue({
      username: 'joao_silva',
      name: 'João',
      email: 'joao@acme.com',
      password: 'senhaForte1',
    });
    await fixture.componentInstance.submit();

    expect(errorSpy).toHaveBeenCalledWith('username já em uso.');
  });

  it('não submete quando o formulário é inválido', async () => {
    await setup({ formMode: 'cadastro' }, { tenantId: 't1' });

    await fixture.componentInstance.submit();

    expect(adminApiStub.createUser).not.toHaveBeenCalled();
  });
});
