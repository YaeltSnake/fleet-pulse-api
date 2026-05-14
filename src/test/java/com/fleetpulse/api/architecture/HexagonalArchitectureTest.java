package com.fleetpulse.api.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.fleetpulse.api")
class HexagonalArchitectureTest {

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
}
