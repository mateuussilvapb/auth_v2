package com.mssousa.authserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mssousa.authserver.application.port.out.SystemTenantRepository;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenant;
import com.mssousa.authserver.domain.model.binding.systemTenant.SystemTenantId;
import com.mssousa.authserver.domain.model.system.System;
import com.mssousa.authserver.domain.model.tenant.Tenant;
import com.mssousa.authserver.domain.model.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa os vínculos usuário-sistema e usuário-sistema-perfil
 * (seção 9 do plano) ponta a ponta.
 */
@AutoConfigureMockMvc
class BindingControllerIntegrationTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SystemTenantRepository systemTenantRepository;

    @Test
    void deveVincularUsuarioASistemaEAtivarPerfil() throws Exception {
        Tenant tenant = createAndSaveTenant("initech");
        System system = createAndSaveSystem("CRM_INITECH_BIND");
        systemTenantRepository.save(SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate()))
                .tenantId(tenant.getId()).systemId(system.getId()).build());
        User user = createAndSaveUser(tenant.getId(), "samir_nagheenanajar", "samir@initech.com");

        long tenantId = tenant.getId().value();
        long systemId = system.getId().value();
        long userId = user.getId().value();

        String bindResponseBody = mockMvc.perform(post("/admin/api/v1/tenants/" + tenantId + "/users/" + userId + "/systems")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"systemId\":" + systemId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        long userSystemId = new ObjectMapper().readTree(bindResponseBody).get("id").asLong();

        mockMvc.perform(patch("/admin/api/v1/tenants/" + tenantId + "/user-systems/" + userSystemId + "/status")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"BLOCKED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        String profileResponseBody = mockMvc.perform(post("/admin/api/v1/systems/" + systemId + "/profiles")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"ADMIN"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long profileId = new ObjectMapper().readTree(profileResponseBody).get("id").asLong();

        String bindProfileResponseBody = mockMvc.perform(
                        post("/admin/api/v1/tenants/" + tenantId + "/user-systems/" + userSystemId + "/profiles")
                                .with(platformAdmin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"profileId\":" + profileId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        long userSystemProfileId = new ObjectMapper().readTree(bindProfileResponseBody).get("id").asLong();

        mockMvc.perform(patch("/admin/api/v1/tenants/" + tenantId + "/user-systems/" + userSystemId
                        + "/profiles/" + userSystemProfileId + "/status")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"INACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void deveListarVinculosUsuarioSistemaEUsuarioPerfil() throws Exception {
        Tenant tenant = createAndSaveTenant("hooli");
        System system = createAndSaveSystem("CRM_HOOLI_BIND");
        systemTenantRepository.save(SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate()))
                .tenantId(tenant.getId()).systemId(system.getId()).build());
        User user = createAndSaveUser(tenant.getId(), "richard_hendricks", "richard@hooli.com");

        long tenantId = tenant.getId().value();
        long systemId = system.getId().value();
        long userId = user.getId().value();

        mockMvc.perform(get("/admin/api/v1/tenants/" + tenantId + "/users/" + userId + "/systems")
                        .with(platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        String bindResponseBody = mockMvc.perform(post("/admin/api/v1/tenants/" + tenantId + "/users/" + userId + "/systems")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"systemId\":" + systemId + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long userSystemId = new ObjectMapper().readTree(bindResponseBody).get("id").asLong();

        mockMvc.perform(get("/admin/api/v1/tenants/" + tenantId + "/users/" + userId + "/systems")
                        .with(platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(userSystemId));

        mockMvc.perform(get("/admin/api/v1/tenants/" + tenantId + "/user-systems/" + userSystemId + "/profiles")
                        .with(platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        String profileResponseBody = mockMvc.perform(post("/admin/api/v1/systems/" + systemId + "/profiles")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"ADMIN"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long profileId = new ObjectMapper().readTree(profileResponseBody).get("id").asLong();

        mockMvc.perform(post("/admin/api/v1/tenants/" + tenantId + "/user-systems/" + userSystemId + "/profiles")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":" + profileId + "}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/admin/api/v1/tenants/" + tenantId + "/user-systems/" + userSystemId + "/profiles")
                        .with(platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].systemProfileId").value(profileId));
    }

    @Test
    void deveRejeitarListagemDePerfisDeVinculoDeOutroTenant() throws Exception {
        Tenant tenantA = createAndSaveTenant("pied-piper");
        Tenant tenantB = createAndSaveTenant("aviato");
        System systemA = createAndSaveSystem("CRM_PIED_PIPER_BIND");
        systemTenantRepository.save(SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate()))
                .tenantId(tenantA.getId()).systemId(systemA.getId()).build());
        User userA = createAndSaveUser(tenantA.getId(), "gilfoyle", "gilfoyle@piedpiper.com");

        String bindResponseBody = mockMvc.perform(post("/admin/api/v1/tenants/" + tenantA.getId().value()
                        + "/users/" + userA.getId().value() + "/systems")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"systemId\":" + systemA.getId().value() + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long userSystemId = new ObjectMapper().readTree(bindResponseBody).get("id").asLong();

        mockMvc.perform(get("/admin/api/v1/tenants/" + tenantB.getId().value() + "/user-systems/" + userSystemId + "/profiles")
                        .with(platformAdmin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRejeitarVinculoDuplicado() throws Exception {
        Tenant tenant = createAndSaveTenant("globex");
        System system = createAndSaveSystem("CRM_GLOBEX_BIND");
        systemTenantRepository.save(SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate()))
                .tenantId(tenant.getId()).systemId(system.getId()).build());
        User user = createAndSaveUser(tenant.getId(), "nina_van_horn", "nina@globex.com");

        String body = "{\"systemId\":" + system.getId().value() + "}";
        String path = "/admin/api/v1/tenants/" + tenant.getId().value() + "/users/" + user.getId().value() + "/systems";

        mockMvc.perform(post(path).with(platformAdmin()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post(path).with(platformAdmin()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deveRejeitarVinculoEntreTenantsDiferentes() throws Exception {
        Tenant tenantA = createAndSaveTenant("umbrella");
        Tenant tenantB = createAndSaveTenant("wonka");
        System systemB = createAndSaveSystem("CRM_WONKA_BIND");
        systemTenantRepository.save(SystemTenant.builder()
                .id(SystemTenantId.of(idGenerator.generate()))
                .tenantId(tenantB.getId()).systemId(systemB.getId()).build());
        User userA = createAndSaveUser(tenantA.getId(), "bob_slydell", "bob@umbrella.com");

        mockMvc.perform(post("/admin/api/v1/tenants/" + tenantA.getId().value() + "/users/" + userA.getId().value() + "/systems")
                        .with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"systemId\":" + systemB.getId().value() + "}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
