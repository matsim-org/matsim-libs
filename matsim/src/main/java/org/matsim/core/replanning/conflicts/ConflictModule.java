package org.matsim.core.replanning.conflicts;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import java.io.File;
import java.util.Random;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;

/**
 * Prepares injection of the conflict resolution logic during replanning
 *
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ConflictModule extends AbstractModule {
  private static final Logger logger = LogManager.getLogger(ConflictModule.class);

  private static final String OUTPUT_FILE = "conflicts.csv";

  @Override
  public void install() {
    // initialize the builder
    getMultibinder(binder());
  }

  @Provides
  @Singleton
  ConflictWriter provideConflictWriter(OutputDirectoryHierarchy outputDirectoryHierarchy) {
    File outputPath = new File(outputDirectoryHierarchy.getOutputFilename(OUTPUT_FILE));
    return new ConflictWriter(outputPath);
  }

  @Provides
  @Singleton
  ConflictManager provideConflictManager(Set<ConflictResolver> resolvers, ConflictWriter writer) {
    if (!getConfig()
        .replanning()
        .getPlanSelectorForRemoval()
        .equals(WorstPlanForRemovalSelectorWithConflicts.SELECTOR_NAME)) {
      logger.warn(
          "The replanning.planSelectorForRemoval is not set to "
              + WorstPlanForRemovalSelectorWithConflicts.SELECTOR_NAME
              + ". This will likely cause problems with the conflict logic if you are not sure what you are doing.");
    }

    Random random = MatsimRandom.getRandom(); // no need for local instance, not parallel!
    return new ConflictManager(resolvers, writer, random);
  }

  static Multibinder<ConflictResolver> getMultibinder(Binder binder) {
    return Multibinder.newSetBinder(binder, ConflictResolver.class);
  }

  /**
   * Allows to bind a conflict resolver in an AbstractModule, for instance: <code>
   * ConflictModule.bindResolver(binder()).toInstance(new ConflictResolver() {
   *    // ...
   * });
   * </code>
   */
  public static LinkedBindingBuilder<ConflictResolver> bindResolver(Binder binder) {
    return getMultibinder(binder).addBinding();
  }
}
