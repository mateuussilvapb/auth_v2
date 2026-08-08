package com.mssousa.authserver.config;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/**
 * Regras de dependência da arquitetura hexagonal (seção 5.1 do plano). Enquanto os
 * pacotes domain/application/adapter estiverem vazios, estas regras falham por design
 * (nenhuma classe para verificar) — servem de guarda-chuva até que o código real,
 * respeitando as regras, seja escrito nas fases seguintes.
 */
@AnalyzeClasses(packages = "com.mssousa.authserver", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN = "..domain..";
    private static final String APPLICATION = "..application..";
    private static final String ADAPTER = "..adapter..";
    private static final String CONFIG = "..config..";

    @ArchTest
    static final ArchRule domain_nao_depende_de_application =
            ArchRuleDefinition.noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAPackage(APPLICATION)
                    .because("domain não pode depender de application (regra 5.1.1 do plano)");

    @ArchTest
    static final ArchRule domain_nao_depende_de_adapter =
            ArchRuleDefinition.noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAPackage(ADAPTER)
                    .because("domain não pode depender de adapter (regra 5.1.1 do plano)");

    @ArchTest
    static final ArchRule domain_nao_depende_de_config =
            ArchRuleDefinition.noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAPackage(CONFIG)
                    .because("domain não pode depender de config (regra 5.1.1 do plano)");

    @ArchTest
    static final ArchRule domain_nao_depende_de_spring =
            ArchRuleDefinition.noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..")
                    .because("domain não pode depender de framework — exceção tolerada apenas para "
                            + "BCryptPasswordEncoder dentro do VO Password (regra 5.1.1 do plano)");

    @ArchTest
    static final ArchRule application_nao_depende_de_adapter =
            ArchRuleDefinition.noClasses().that().resideInAPackage(APPLICATION)
                    .should().dependOnClassesThat().resideInAPackage(ADAPTER)
                    .because("application não pode depender de adapter (regra 5.1.2 do plano)");

    @ArchTest
    static final ArchRule application_nao_depende_de_web_jpa_oauth2 =
            ArchRuleDefinition.noClasses().that().resideInAPackage(APPLICATION)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.servlet..", "jakarta.persistence..",
                            "org.springframework.web..", "org.springframework.security.oauth2..")
                    .because("application não pode depender de classes web/JPA/OAuth2 (regra 5.1.2 do plano)");
}
