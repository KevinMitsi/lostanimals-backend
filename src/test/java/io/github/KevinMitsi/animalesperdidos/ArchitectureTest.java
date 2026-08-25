package io.github.KevinMitsi.animalesperdidos;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web.validation.NoSqlInjection;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "io.github.KevinMitsi.animalesperdidos", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_is_framework_free = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideOutsideOfPackages("java..", "..domain..");

    @ArchTest
    static final ArchRule application_does_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule controllers_only_depend_on_input_boundaries = noClasses()
            .that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().resideInAnyPackage("..application.service..", "..application.port.out..", "..domain..");

    @ArchTest
    static final ArchRule every_request_dto_rejects_sql_syntax = classes()
            .that().resideInAPackage("..infrastructure.adapter.web.dto")
            .and().haveSimpleNameEndingWith("Request")
            .should().beAnnotatedWith(NoSqlInjection.class);
}
