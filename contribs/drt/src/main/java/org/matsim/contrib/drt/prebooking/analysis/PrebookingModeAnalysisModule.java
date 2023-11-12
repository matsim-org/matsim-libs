package org.matsim.contrib.drt.prebooking.analysis;

import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Singleton;

public class PrebookingModeAnalysisModule extends AbstractDvrpModeModule {
	public PrebookingModeAnalysisModule(String mode) {
		super(mode);
	}

	@Override
	public void install() {
		addControlerListenerBinding().to(modalKey(PrebookingAnalysisListener.class));

		bindModal(PrebookingAnalysisListener.class).toProvider(modalProvider(getter -> {
			EventsManager eventsManager = getter.get(EventsManager.class);
			OutputDirectoryHierarchy outputHierarchy = getter.get(OutputDirectoryHierarchy.class);

			return new PrebookingAnalysisListener(getMode(), eventsManager, outputHierarchy);
		})).in(Singleton.class);
	}
}
