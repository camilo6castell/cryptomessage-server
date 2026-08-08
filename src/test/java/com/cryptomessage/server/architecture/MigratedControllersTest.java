package com.cryptomessage.server.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Scoped deliberately to just the two controllers this migration touched.
 * AuthenticationController / ContactController / UserController were left
 * exactly as they were (see docs/MIGRATION_NOTES.md, "What did NOT change")
 * and still legitimately call repositories/services directly — asserting
 * this rule against the whole controller package would be dishonest about
 * what actually happened.
 */
@AnalyzeClasses(packages = "com.cryptomessage.server", importOptions = ImportOption.DoNotIncludeTests.class)
public class MigratedControllersTest {

    @ArchTest
    static final ArchRule chat_and_message_controllers_should_not_bypass_the_application_layer =
            noClasses().that().haveSimpleName("ChatController").or().haveSimpleName("MessageController")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..repositories..",
                            "..infrastructure..",
                            "..domain..",
                            "..services.."
                    )
                    .because("ChatController/MessageController now delegate everything to "
                            + "application.chat / application.message use cases — no more direct "
                            + "repository or service access, which is exactly what the old "
                            + "ChatService/MessageService did before this migration");
}
