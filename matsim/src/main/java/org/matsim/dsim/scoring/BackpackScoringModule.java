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
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
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
		bind(ScoringDataCollectorRegistry.class).in(Singleton.class);
		addControllerListenerBinding().to(Cleanup.class).in(Singleton.class);
		bind(IterationInformation.class).in(Singleton.class);
		addControllerListenerBinding().to(IterationInformation.class);
		bind(NewScoreAssigner.class).to(NewScoreAssignerImpl.class).in(Singleton.class);
		bind(ExperiencedPlansService.class).to(ExperiencedPlansCollector.class).in(Singleton.class);

		if (getConfig().scoring().isWriteExperiencedPlans()) {
			bind(WriteExperiencedPlans.class).in(Singleton.class);
			addControllerListenerBinding().to(WriteExperiencedPlans.class);
		}

		if (getConfig().scoring().isMemorizingExperiencedPlans()) {
			bind(ExperiencedPlansMemorizer.class).in(Singleton.class);
			addControllerListenerBinding().to(ExperiencedPlansMemorizer.class);
		}

		installQSimModule(new BackpackScoringQSimModule());
	}

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
			for (var person : this.population.getPersons().values()) {
				var experiencedPlan = experiencedPlansService.getExperiencedPlans().get(person.getId());
				var selectedPlan = person.getSelectedPlan();
				selectedPlan.getCustomAttributes().put(ScoringConfigGroup.EXPERIENCED_PLAN_KEY, experiencedPlan);
			}
		}
	}

	/**
	 * This mimicks the architecture of the old PlansScoringImpl. However, I think, the ExperiencedPlansCollector could itself
	 * be an iteration ends listener and handle the writing of plans.
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
			// TODO make sure this is only done on the head node
			// I think we don't really need this.
			experiencedPlansService.finishIteration();

			if (isWriteIteration(event.getIteration()) || event.isLastIteration()) {

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

	public static class Cleanup implements AfterMobsimListener {

		private final ScoringDataCollectorRegistry registry;
		private final EventsManager em;

		@Inject
		private Cleanup(ScoringDataCollectorRegistry registry, EventsManager em) {
			this.registry = registry;
			this.em = em;
		}

		@Override
		public void notifyAfterMobsim(AfterMobsimEvent e) {
			for (var collector : registry.collectors()) {
				em.removeHandler(collector);
			}
			registry.clear();
		}
	}

	private static class BackpackScoringQSimModule extends AbstractQSimModule {

		@Override
		protected void configureQSim() {
			bind(ScoringDataCollector.class).in(Singleton.class);
			bind(EndOfDayScoring.class).in(Singleton.class);
		}
	}

	public static class ScoringDataCollectorRegistry {

		private final List<ScoringDataCollector> collectors = new ArrayList<>();

		public void register(ScoringDataCollector collector) {
			collectors.add(collector);
		}

		public List<ScoringDataCollector> collectors() {
			return collectors;
		}

		public void clear() {
			collectors.clear();
		}
	}
}
