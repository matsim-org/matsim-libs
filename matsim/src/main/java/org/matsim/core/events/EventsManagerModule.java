package org.matsim.core.events;

import org.matsim.core.controler.AbstractModule;

public class EventsManagerModule extends AbstractModule {

	@Override
	public void install() {
		if (getConfig().parallelEventHandling().getOneThreadPerHandler() != null && getConfig().parallelEventHandling().getOneThreadPerHandler()) {
			bindEventsManager().to(ParallelEventsManager.class).asEagerSingleton();
		} else if (getConfig().parallelEventHandling().getNumberOfThreads() != null) {
			if (getConfig().parallelEventHandling().getSynchronizeOnSimSteps() != null && getConfig().parallelEventHandling().getSynchronizeOnSimSteps()) {
				bindEventsManager().to(SimStepParallelEventsManagerImpl.class).asEagerSingleton();
			} else {
				bindEventsManager().to(ParallelEventsManagerImpl.class).asEagerSingleton();
			}
		} else {
			bindEventsManager().to(SimStepParallelEventsManagerImpl.class).asEagerSingleton();
		}
	}

}
