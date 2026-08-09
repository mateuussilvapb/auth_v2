import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { TenantListComponent } from './tenant-list.component';
import { AdminApiService } from '../../../core/services/admin-api.service';
import { Page, TenantResponse } from '../../../core/models/admin-api.models';

describe('TenantListComponent', () => {
  let fixture: ComponentFixture<TenantListComponent>;
  let adminApiStub: { listTenants: jasmine.Spy; updateTenantStatus: jasmine.Spy };

  const page: Page<TenantResponse> = {
    content: [{ id: 1, code: 'acme', name: 'Acme', status: 'ACTIVE', logoUrl: null }],
    totalElements: 1,
    totalPages: 1,
    number: 0,
    size: 20,
  };

  beforeEach(async () => {
    adminApiStub = {
      listTenants: jasmine.createSpy('listTenants').and.returnValue(of(page)),
      updateTenantStatus: jasmine.createSpy('updateTenantStatus').and.returnValue(of(page.content[0])),
    };

    await TestBed.configureTestingModule({
      imports: [TenantListComponent],
      providers: [provideRouter([]), { provide: AdminApiService, useValue: adminApiStub }],
    }).compileComponents();

    fixture = TestBed.createComponent(TenantListComponent);
    fixture.detectChanges();
  });

  it('deve carregar a lista de tenants ao iniciar', () => {
    expect(fixture.componentInstance.tenants()).toEqual(page.content);
    expect(fixture.componentInstance.loading()).toBe(false);
  });

  it('deve marcar erro quando a listagem falha', () => {
    adminApiStub.listTenants.and.returnValue(throwError(() => new Error('falhou')));

    fixture.componentInstance.load();

    expect(fixture.componentInstance.errorMessage()).not.toBeNull();
  });

  it('deve chamar updateTenantStatus com o status invertido', () => {
    fixture.componentInstance.toggleStatus(page.content[0]);
    expect(adminApiStub.updateTenantStatus).toHaveBeenCalledWith(1, { status: 'INACTIVE' });
  });
});
