package org.matsim.dsim.scoring;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.scoring.ExperiencedPlansService;
import org.matsim.core.scoring.NewScoreAssigner;
import org.matsim.core.scoring.NewScoreAssignerImpl;
import org.matsim.dsim.simulation.IterationInformation;

import java.util.ArrayList;
import java.util.List;

public class BackpackScoringModule extends AbstractModule {
	@Override
	public void install() {
		bind(BackpackDataCollectorRegistry.class).in(Singleton.class);
		addControllerListenerBinding().to(Cleanup.class).in(Singleton.class);
		bind(IterationInformation.class).in(Singleton.class);
		addControllerListenerBinding().to(IterationInformation.class);
		bind(NewScoreAssigner.class).to(NewScoreAssignerImpl.class).in(Singleton.class);
		bind(EndOfDayScoring.class).in(Singleton.class);
		bind(FinishedBackpackCollector.class).in(Singleton.class);
		bind(ExperiencedPlansService.class).to(FinishedBackpackCollector.class);

		bind(BackpackScoringListener.class).in(Singleton.class);
		addControllerListenerBinding().to(BackpackScoringListener.class);

		// The original code also writes partial scores for each scored element. We have now changed the scoring
		// to be ready for trip-based scoring. Don't know whether it makes sense to still have these partial scores.
		if (getConfig().scoring().isWriteExperiencedPlans()) {
			bind(WriteExperiencedPlans.class).in(Singleton.class);
			addControllerListenerBinding().to(WriteExperiencedPlans.class);
		}

		if (getConfig().scoring().isMemorizingExperiencedPlans()) {
			bind(ExperiencedPlansMemorizer.class).in(Singleton.class);
			addControllerListenerBinding().to(ExperiencedPlansMemorizer.class);
		}

		// each SimProcess must have its own data collector. So, bind it as QSimModule.
		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(BackpackDataCollector.class).in(Singleton.class);
			}
		});
	}

	/**
	 * Binds the data collector to the scoring. This way we don't have to have events management in
	 * those classes.
	 */
	static class BackpackScoringListener implements ScoringListener {

		private final FinishedBackpackCollector collector;
		private final EndOfDayScoring scoring;

		@Inject
		BackpackScoringListener(FinishedBackpackCollector collector, EndOfDayScoring scoring) {
			this.collector = collector;
			this.scoring = scoring;
		}

		@Override
		public void notifyScoring(ScoringEvent event) {
			collector.exchangePlansForScoring();
			var backpacks = collector.backpacks();
			for (var backpack : backpacks) {
				scoring.score(backpack);
			}
		}
	}

	/**
	 * This class adds experienced plans to the persons' selected plan. This used to be mashed together with experienced plans writing but I think
	 * these things are not really related.
	 */
	static class ExperiencedPlansMemorizer implements IterationEndsListener {

		private final Population population;
		private final ExperiencedPlansService experiencedPlansService;

		@Inject
		ExperiencedPlansMemorizer(Population population, ExperiencedPlansService experiencedPlansService) {
			this.population = population;
			this.experiencedPlansService = experiencedPlansService;
		}

		@Override
		public void notifyIterationEnds(IterationEndsEvent event) {

			// iterate over the experienced plans. Each compute node loads all persons into memory, but each compute node only has
			// experienced plans for the persons which start on that node. Exception is the head node, which might have all the
			// experienced plans if the current iteration is an iteration where experienced plans are written to disk. See {@link WriteExperiencedPlans}
			for (var experiencedEntry : experiencedPlansService.getExperiencedPlans().entrySet()) {
				var personId = experiencedEntry.getKey();
				var person = this.population.getPersons().get(personId);
				var selectedPlan = person.getSelectedPlan();
				var experiencedPlan = experiencedEntry.getValue();
				selectedPlan.getCustomAttributes().put(ScoringConfigGroup.EXPERIENCED_PLAN_KEY, experiencedPlan);
			}
		}
	}

	/**
	 * This mimicks the architecture of the old PlansScoringImpl. Though I think the FinishedPlansCollector could itself
	 * be an iteration ends listener and handle the writing of plans, we'll have this structure here, to adhere to the
	 * existing architecture of PlansScoringImpl.
	 */
	static class WriteExperiencedPlans implements IterationEndsListener {

		private final Config config;
		private final OutputDirectoryHierarchy outputDirectoryHierarchy;
		private final ExperiencedPlansService experiencedPlansService;

		@Inject
		WriteExperiencedPlans(Config config, OutputDirectoryHierarchy outputDirectoryHierarchy, ExperiencedPlansService experiencedPlansService) {
			this.config = config;
			this.outputDirectoryHierarchy = outputDirectoryHierarchy;
			this.experiencedPlansService = experiencedPlansService;
		}

		@Override
		public void notifyIterationEnds(IterationEndsEvent event) {

			if (isWriteIteration(event.getIteration()) || event.isLastIteration()) {

				experiencedPlansService.finishIteration();

				var filename = outputDirectoryHierarchy.getIterationFilename(
					event.getIteration(), Controler.DefaultFiles.experiencedPlans, config.controller().getCompressionType());
				experiencedPlansService.writeExperiencedPlans(filename);
			}
		}

		private boolean isWriteIteration(int iteration) {
			var writeInterval = config.controller().getWriteEventsInterval();
			return writeInterval > 0 && iteration % writeInterval == 0;
		}
	}

	/**
	 * Cleanup code for this module. At the moment it:
	 * 1. Removes all backpack data collectors from the events manager and clears the corresponding registry after an iteration finishes.
	 * 2. Resests the backpack collector, so it is ready for the next iteration.
	 */
	public static class Cleanup implements AfterMobsimListener, IterationStartsListener {

		private final BackpackDataCollectorRegistry registry;
		private final EventsManager em;
		private final FinishedBackpackCollector backpackCollector;

		@Inject
		private Cleanup(BackpackDataCollectorRegistry registry, EventsManager em, FinishedBackpackCollector backpackCollector) {
			this.registry = registry;
			this.em = em;
			this.backpackCollector = backpackCollector;
		}

		@Override
		public void notifyAfterMobsim(AfterMobsimEvent e) {
			for (var collector : registry.collectors()) {
				em.removeHandler(collector);
			}
			registry.clear();
		}

		@Override
		public void notifyIterationStarts(IterationStartsEvent event) {
			backpackCollector.reset();
		}
	}

	/**
	 * This maintains a reference to all the backpack data collectors. We need to keep track of the collectors, so that we can remove them
	 * from the events manager after an iteration finishes.
	 */
	public static class BackpackDataCollectorRegistry {

		private final List<BackpackDataCollector> collectors = new ArrayList<>();

		public void register(BackpackDataCollector collector) {
			collectors.add(collector);
		}

		public List<BackpackDataCollector> collectors() {
			return collectors;
		}

		public void clear() {
			collectors.clear();
		}
	}
}
