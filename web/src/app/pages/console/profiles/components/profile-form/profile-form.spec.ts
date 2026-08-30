import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ConfirmationService, MessageService as MessageServicePG } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import type { Mock } from 'vitest';

import { ProfileForm } from './profile-form';
import { AdminApiService } from '../../../../../core/services/admin-api.service';
import { MessageService } from '../../../../../shared/services/message.service';
import { SystemProfileResponse } from '../../../../../core/models/admin-api.models';

describe('ProfileForm', () => {
  let fixture: ComponentFixture<ProfileForm>;
  let adminApiStub: { getProfile: Mock; createProfile: Mock; updateProfile: Mock };
  let router: Router;
  let messageService: MessageService;

  const profile: SystemProfileResponse = {
    id: '1',
    systemId: '1',
    code: 'ADMIN',
    description: 'Administrador',
    status: 'ACTIVE',
  };

  async function setup(routeData: Record<string, unknown>, paramMap: Record<string, string> = {}): Promise<void> {
    adminApiStub = {
      getProfile: vi.fn().mockReturnValue(of(profile)),
      createProfile: vi.fn().mockReturnValue(of(profile)),
      updateProfile: vi.fn().mockReturnValue(of(profile)),
    };

    await TestBed.configureTestingModule({
      imports: [ProfileForm],
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

    fixture = TestBed.createComponent(ProfileForm);
    fixture.detectChanges();
  }

  it('modo criação: código habilitado, cria e navega para a listagem do sistema', async () => {
    await setup({ formMode: 'cadastro' }, { systemId: '1' });

    expect(fixture.componentInstance.isCreateMode()).toBe(true);
    expect(fixture.componentInstance.form.get('code')?.disabled).toBe(false);

    fixture.componentInstance.form.setValue({ code: 'OPERADOR', description: 'Operador' });
    await fixture.componentInstance.submit();

    expect(adminApiStub.createProfile).toHaveBeenCalledWith('1', { code: 'OPERADOR', description: 'Operador' });
    expect(router.navigate).toHaveBeenCalledWith(['/console/systems', '1', 'profiles']);
  });

  it('modo edição: carrega o perfil, desabilita código e atualiza só a descrição', async () => {
    await setup({ formMode: 'edicao' }, { systemId: '1', id: '1' });

    expect(fixture.componentInstance.isEditMode()).toBe(true);
    expect(adminApiStub.getProfile).toHaveBeenCalledWith('1', '1');
    expect(fixture.componentInstance.form.get('code')?.disabled).toBe(true);
    expect(fixture.componentInstance.form.getRawValue()).toEqual({ code: 'ADMIN', description: 'Administrador' });

    fixture.componentInstance.form.patchValue({ description: 'Administrador geral' });
    await fixture.componentInstance.submit();

    expect(adminApiStub.updateProfile).toHaveBeenCalledWith('1', '1', { description: 'Administrador geral' });
    expect(router.navigate).toHaveBeenCalledWith(['/console/systems', '1', 'profiles']);
  });

  it('deve mostrar erro quando falha ao carregar o perfil em modo edição', async () => {
    adminApiStub = {
      getProfile: vi.fn().mockReturnValue(throwError(() => new Error('falhou'))),
      createProfile: vi.fn(),
      updateProfile: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [ProfileForm],
      providers: [
        provideRouter([]),
        ConfirmationService,
        DialogService,
        MessageServicePG,
        { provide: AdminApiService, useValue: adminApiStub },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: new Map([['systemId', '1'], ['id', '1']]), data: { formMode: 'edicao' } } },
        },
      ],
    }).compileComponents();

    messageService = TestBed.inject(MessageService);
    const errorSpy = vi.spyOn(messageService, 'showError');

    fixture = TestBed.createComponent(ProfileForm);
    fixture.detectChanges();
    await new Promise((resolve) => setTimeout(resolve));

    expect(errorSpy).toHaveBeenCalledWith('Não foi possível carregar o perfil.');
  });

  it('deve mostrar a mensagem de erro da API quando falha ao salvar', async () => {
    await setup({ formMode: 'cadastro' }, { systemId: '1' });
    adminApiStub.createProfile.mockReturnValue(throwError(() => ({ error: { message: 'code já em uso.' } })));
    const errorSpy = vi.spyOn(messageService, 'showError');

    fixture.componentInstance.form.setValue({ code: 'ADMIN', description: '' });
    await fixture.componentInstance.submit();

    expect(errorSpy).toHaveBeenCalledWith('code já em uso.');
  });

  it('não submete quando o formulário é inválido', async () => {
    await setup({ formMode: 'cadastro' }, { systemId: '1' });

    fixture.componentInstance.form.get('code')?.setValue('');
    await fixture.componentInstance.submit();

    expect(adminApiStub.createProfile).not.toHaveBeenCalled();
  });
});
