package com.habittracker.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ModuleBoundaryTest {

    private static final String BASE_PACKAGE = "com.habittracker";

    private static final List<String> MODULES = List.of("habits", "checkins", "streaks", "groups", "notifications");

    @Test
    void modulesMustNotReachIntoAnotherModulesInfrastructurePackage() {
        var classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE_PACKAGE);

        for (String module : MODULES) {
            String infrastructure = BASE_PACKAGE + "." + module + ".infrastructure..";
            ArchRule rule = noClasses()
                .that().resideOutsideOfPackage(infrastructure)
                .should().dependOnClassesThat().resideInAPackage(infrastructure)
                .allowEmptyShould(true);

            rule.check(classes);
        }
    }

    @Test
    void domainClassesMustNotDependOnSpringOrJpa() {
        var classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE_PACKAGE);

        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..");

        rule.check(classes);
    }
}
