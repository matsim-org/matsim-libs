package org.matsim.dsim.scoring;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
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

		installQSimModule(new BackpackScoringQSimModule());
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
