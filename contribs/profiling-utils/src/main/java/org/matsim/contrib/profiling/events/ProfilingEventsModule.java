package org.matsim.contrib.profiling.events;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;

/**
 * Hook into MATSim Listeners to create JFR profiling events at different phases within MATSim.
 */
public class ProfilingEventsModule extends AbstractModule {

	@Override
	public void install() {
		addControlerListenerBinding().to(IterationTimer.class);
		addControlerListenerBinding().to(MatsimEvents.class);
		addMobsimListenerBinding().to(MobsimTimer.class);
	}

	private static final class IterationTimer implements IterationStartsListener, IterationEndsListener {

		private JFRIterationEvent event = null;


		@Override
		public double priority() {
			return Double.MAX_VALUE - 100;
		}

		@Override
		public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
			if (event == null) {
				var errorEvent = new JFRProfilingErrorEvent("Iteration ended but event was null");
				errorEvent.commit();
			} else {
				event.commit();
				event = null;
			}
		}

		@Override
		public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {
			if (event != null) {
				var errorEvent = new JFRProfilingErrorEvent("Iteration started but event was not null");
				errorEvent.commit();
				event.commit();
			}
			event = new JFRIterationEvent(iterationStartsEvent.getIteration());
			event.begin();
		}
	}

	private static final class MobsimTimer implements MobsimInitializedListener, MobsimBeforeCleanupListener {

		private JFRMobsimEvent event = null;

		@Override
		public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent mobsimBeforeCleanupEvent) {
			if (event == null) {
				var errorEvent = new JFRProfilingErrorEvent("Mobsim ended but event was null");
				errorEvent.commit();
			} else {
				event.commit();
				event = null;
			}
		}

		@Override
		public void notifyMobsimInitialized(MobsimInitializedEvent mobsimInitializedEvent) {
			if (event != null) {
				var errorEvent = new JFRProfilingErrorEvent("Mobsim started but event was not null");
				errorEvent.commit();
				event.commit();
			}
			event = new JFRMobsimEvent();
			event.begin();
		}
	}

	private static final class MatsimEvents implements StartupListener, ShutdownListener, ReplanningListener, ScoringListener {

		@Override
		public void notifyReplanning(ReplanningEvent replanningEvent) {
			JFRMatsimEvent.create("replanning").commit();
		}

		@Override
		public void notifyScoring(ScoringEvent scoringEvent) {
			JFRMatsimEvent.create("scoring").commit();
		}

		@Override
		public void notifyShutdown(ShutdownEvent shutdownEvent) {
			JFRMatsimEvent.create("shutdown").commit();
		}

		@Override
		public void notifyStartup(StartupEvent startupEvent) {
			JFRMatsimEvent.create("startup").commit();
		}
	}
}
