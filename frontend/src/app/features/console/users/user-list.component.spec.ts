import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { UserListComponent } from './user-list.component';
import { AdminApiService } from '../../../core/services/admin-api.service';
import { Page, UserResponse } from '../../../core/models/admin-api.models';

describe('UserListComponent', () => {
  let fixture: ComponentFixture<UserListComponent>;
  let adminApiStub: { listUsers: jasmine.Spy; updateUserStatus: jasmine.Spy; resetUserPassword: jasmine.Spy };

  const page: Page<UserResponse> = {
    content: [{ id: '1', tenantId: '1', username: 'joao_silva', email: 'joao@acme.com', name: 'João', status: 'ACTIVE' }],
    totalElements: 1,
    totalPages: 1,
    number: 0,
    size: 20,
  };

  beforeEach(async () => {
    adminApiStub = {
      listUsers: jasmine.createSpy('listUsers').and.returnValue(of(page)),
      updateUserStatus: jasmine.createSpy('updateUserStatus').and.returnValue(of(page.content[0])),
      resetUserPassword: jasmine.createSpy('resetUserPassword').and.returnValue(of(undefined)),
    };

    await TestBed.configureTestingModule({
      imports: [UserListComponent],
      providers: [
        provideRouter([]),
        { provide: AdminApiService, useValue: adminApiStub },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ tenantId: '1' }) } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserListComponent);
    fixture.detectChanges();
  });

  it('deve carregar usuários do tenant da rota', () => {
    expect(adminApiStub.listUsers).toHaveBeenCalledWith('1', 0, 20);
    expect(fixture.componentInstance.users()).toEqual(page.content);
  });

  it('deve marcar erro quando a listagem falha', () => {
    adminApiStub.listUsers.and.returnValue(throwError(() => new Error('falhou')));
    fixture.componentInstance.load();
    expect(fixture.componentInstance.errorMessage()).not.toBeNull();
  });

  it('deve chamar updateUserStatus com o status escolhido', () => {
    fixture.componentInstance.changeStatus(page.content[0], 'BLOCKED');
    expect(adminApiStub.updateUserStatus).toHaveBeenCalledWith('1', '1', { status: 'BLOCKED' });
  });

  it('deve mostrar mensagem de sucesso ao redefinir senha', () => {
    fixture.componentInstance.resetPassword(page.content[0]);
    expect(fixture.componentInstance.infoMessage()).toContain('joao@acme.com');
  });
});
