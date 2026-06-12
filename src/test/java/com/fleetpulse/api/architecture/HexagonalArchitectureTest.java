package com.fleetpulse.api.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

@AnalyzeClasses(packages = "com.fleetpulse.api", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    // ── Existing boundary rules ──────────────────────────────────────────────

    @ArchTest
    static final ArchRule domainDoesNotImportInfrastructure = noClasses()
            .that().resideInAPackage("com.fleetpulse.api.domain..")
            .should().dependOnClassesThat().resideInAPackage("com.fleetpulse.api.infrastructure..");

    @ArchTest
    static final ArchRule applicationDoesNotImportInfrastructure = noClasses()
            .that().resideInAPackage("com.fleetpulse.api.application..")
            .should().dependOnClassesThat().resideInAPackage("com.fleetpulse.api.infrastructure..");

    @ArchTest
    static final ArchRule domainDoesNotImportSpring = noClasses()
            .that().resideInAPackage("com.fleetpulse.api.domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule applicationDoesNotImportSpring = noClasses()
            .that().resideInAPackage("com.fleetpulse.api.application..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    // ── 7.8 additions — enforce hexagonal prohibitions ───────────────────────

    /** Prohibition: field-injection banned; constructor injection only. */
    @ArchTest
    static final ArchRule noAutowiredOnFields = noFields()
            .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .because("constructor injection is mandatory — @Autowired on fields is forbidden");

    /** Prohibition: @Service must not appear in application/ layer. */
    @ArchTest
    static final ArchRule noServiceAnnotationInApplicationLayer = noClasses()
            .that().resideInAPackage("com.fleetpulse.api.application..")
            .should().beAnnotatedWith("org.springframework.stereotype.Service")
            .because("application services are wired via ApplicationConfig @Bean, not @Service");

    /** Prohibition: @Transactional must not appear on adapter classes. */
    @ArchTest
    static final ArchRule noTransactionalInAdapters = noClasses()
            .that().resideInAPackage("com.fleetpulse.api.infrastructure.adapter..")
            .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
            .because("@Transactional belongs in application services, not in adapters");

    /** Prohibition: all classes in the web DTO package must be records. */
    @ArchTest
    static final ArchRule dtosMustBeRecords = classes()
            .that().resideInAPackage("com.fleetpulse.api.infrastructure.adapter.in.web.dto..")
            .should().beRecords()
            .because("DTOs must be immutable Java records");

    @ArchTest
    static final ArchRule noImplSuffix = noClasses()
            .should().haveSimpleNameEndingWith("Impl")
            .because("'Impl' suffix is a naming anti-pattern; use descriptive adapter/service names");
}
