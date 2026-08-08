package com.cryptomessage.server.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * General hygiene rules that apply across the whole codebase, not just the
 * migrated slice.
 */
@AnalyzeClasses(packages = "com.cryptomessage.server", importOptions = ImportOption.DoNotIncludeTests.class)
public class GeneralCodeQualityTest {

    @ArchTest
    static final ArchRule constructor_injection_only_no_field_autowiring =
            noFields().should().beAnnotatedWith(Autowired.class)
                    .because("constructor injection makes dependencies explicit and lets a class be "
                            + "instantiated in a plain unit test without a Spring context — every class "
                            + "added in this migration (use cases, adapters, ports) already follows this");

    @ArchTest
    static final ArchRule domain_chat_should_be_free_of_package_cycles =
            slices().matching("com.cryptomessage.server.domain.chat.(*)..")
                    .should().beFreeOfCycles();
}
