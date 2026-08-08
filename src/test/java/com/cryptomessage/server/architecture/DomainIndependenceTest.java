package com.cryptomessage.server.architecture;

import com.cryptomessage.server.model.entity.chat.ChatStatus;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClass.Predicates;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces that domain.* stays a plain, framework-free Java model — no Spring,
 * no JPA, no knowledge of the use cases that orchestrate it or the adapters
 * that implement its ports. This is the guarantee multi-module Gradle gives
 * for free via the compiler (see Library Provider); here it's enforced by
 * these tests instead — see docs/MIGRATION_NOTES.md for why that trade-off
 * was made for this migration specifically.
 */
@AnalyzeClasses(packages = "com.cryptomessage.server", importOptions = ImportOption.DoNotIncludeTests.class)
public class DomainIndependenceTest {

    private static final String DOMAIN = "..domain..";

    @ArchTest
    static final ArchRule domain_should_not_depend_on_application_layer =
            noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAPackage("..application..")
                    .because("the domain is the innermost layer — how a use case orchestrates it "
                            + "must never leak back in");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure_layer =
            noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .because("this is the entire point of domain.chat.port: the domain must stay "
                            + "swappable without knowing what implements it");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_the_web_layer =
            noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAPackage("..controller..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_spring =
            noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "org.springdoc..")
                    .because("the aggregate must stay unit-testable with zero framework on the classpath");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_jpa =
            noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
                    .because("Event Sourcing IS the domain's persistence mechanism — it must not also "
                            + "know about JPA, which belongs entirely to the read-model side");

    // The one deliberate, documented exception — see the class-level javadoc on
    // domain.chat.Chat. ChatStatus is a plain two-value enum with no JPA
    // annotations of its own; duplicating it purely to satisfy layering purity
    // would trade a real risk (two enums silently drifting apart) for a
    // theoretical one.
    private static final DescribedPredicate<JavaClass> modelEntityExceptChatStatus =
            Predicates.resideInAPackage("..model.entity..")
                    .and(DescribedPredicate.not(Predicates.equivalentTo(ChatStatus.class)))
                    .as("model.entity.* (excluding the documented ChatStatus reuse)");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_the_read_model_except_ChatStatus =
            noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat(modelEntityExceptChatStatus)
                    .because("model.entity.* is the read-model / JPA side; ChatStatus is the one "
                            + "documented exception");
}
