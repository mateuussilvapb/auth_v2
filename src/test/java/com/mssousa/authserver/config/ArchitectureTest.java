package com.mssousa.authserver.config;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;

/**
 * Regras de dependência da arquitetura hexagonal (seção 5.1 do plano). As regras usam
 * {@code allowEmptyShould(true)} porque domain/application/adapter ainda não têm todo o
 * código das fases seguintes — cada regra passa a valer de fato assim que houver classes
 * nos pacotes envolvidos, funcionando como guarda-chuva permanente contra violações futuras.
 */
@AnalyzeClasses(packages = "com.mssousa.authserver", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN = "..domain..";
    private static final String APPLICATION = "..application..";
    private static final String ADAPTER = "..adapter..";
    private static final String CONFIG = "..config..";

    // Exceção tolerada, herdada do projeto de referência (seção 5.1, item 1):
    // BCryptPasswordEncoder dentro do VO Password.
    private static final DescribedPredicate<JavaClass> FRAMEWORK_NAO_TOLERADO =
            resideInAnyPackage("org.springframework..", "jakarta.persistence..")
                    .and(DescribedPredicate.not(resideInAnyPackage("org.springframework.security.crypto..")));

    @ArchTest
    static final ArchRule domain_nao_depende_de_application =
            ArchRuleDefinition.noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAPackage(APPLICATION)
                    .because("domain não pode depender de application (regra 5.1.1 do plano)")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_nao_depende_de_adapter =
            ArchRuleDefinition.noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAPackage(ADAPTER)
                    .because("domain não pode depender de adapter (regra 5.1.1 do plano)")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_nao_depende_de_config =
            ArchRuleDefinition.noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAPackage(CONFIG)
                    .because("domain não pode depender de config (regra 5.1.1 do plano)")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_nao_depende_de_spring =
            ArchRuleDefinition.noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat(FRAMEWORK_NAO_TOLERADO)
                    .because("domain não pode depender de framework — exceção tolerada apenas para "
                            + "BCryptPasswordEncoder dentro do VO Password (regra 5.1.1 do plano)")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule application_nao_depende_de_adapter =
            ArchRuleDefinition.noClasses().that().resideInAPackage(APPLICATION)
                    .should().dependOnClassesThat().resideInAPackage(ADAPTER)
                    .because("application não pode depender de adapter (regra 5.1.2 do plano)")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule application_nao_depende_de_web_jpa_oauth2 =
            ArchRuleDefinition.noClasses().that().resideInAPackage(APPLICATION)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.servlet..", "jakarta.persistence..",
                            "org.springframework.web..", "org.springframework.security.oauth2..")
                    .because("application não pode depender de classes web/JPA/OAuth2 (regra 5.1.2 do plano)")
                    .allowEmptyShould(true);
}
