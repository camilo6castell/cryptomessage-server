package com.cryptomessage.server.architecture;

import com.cryptomessage.server.infrastructure.eventstore.ChatAggregateRepositoryAdapter;
import com.cryptomessage.server.infrastructure.eventstore.JpaChatIdGenerator;
import com.cryptomessage.server.infrastructure.notification.StompNotificationAdapter;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClass.Predicates;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Use cases must depend on the domain's PORTS for the write side (event
 * store, id generation, notifications) — never the concrete adapter that
 * implements them. Swap the persistence technology behind
 * ChatAggregateRepository tomorrow and no use case should need to change.
 *
 * NOTE what these rules deliberately do NOT forbid: application classes still
 * depend directly on repositories.*, model.entity.* and
 * infrastructure.projection.ChatProjector for reads and for updating the
 * read-model projection. That's an intentional CQRS choice, not an oversight
 * — see "Key compatibility decisions" #3 in docs/MIGRATION_NOTES.md. There is
 * exactly one projector and it will only ever have one implementation, by
 * definition (it exists to keep two tables in the same database consistent)
 * — a port there would be indirection with no real swap-point behind it.
 */
@AnalyzeClasses(packages = "com.cryptomessage.server", importOptions = ImportOption.DoNotIncludeTests.class)
public class ApplicationLayerTest {

    private static final String APPLICATION = "..application..";

    @ArchTest
    static final ArchRule application_should_not_depend_on_the_web_layer =
            noClasses().that().resideInAPackage(APPLICATION)
                    .should().dependOnClassesThat().resideInAPackage("..controller..")
                    .because("dependencies point inward: controllers call use cases, never the reverse");

    private static final DescribedPredicate<JavaClass> writeSidePortAdapters =
            Predicates.equivalentTo(ChatAggregateRepositoryAdapter.class)
                    .or(Predicates.equivalentTo(JpaChatIdGenerator.class))
                    .or(Predicates.equivalentTo(StompNotificationAdapter.class))
                    .as("write-side port adapters (ChatAggregateRepositoryAdapter, JpaChatIdGenerator, "
                            + "StompNotificationAdapter)");

    @ArchTest
    static final ArchRule application_should_depend_on_write_side_ports_not_their_adapters =
            noClasses().that().resideInAPackage(APPLICATION)
                    .should().dependOnClassesThat(writeSidePortAdapters)
                    .because("use cases must depend on domain.chat.port interfaces "
                            + "(ChatAggregateRepository, ChatIdGenerator, NotificationPort), not their "
                            + "concrete adapters — Spring wires the real implementation in at runtime");
}
