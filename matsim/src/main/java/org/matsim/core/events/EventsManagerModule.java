package org.matsim.core.events;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.handler.EventHandler;

import javax.inject.Inject;
import java.util.Set;

public final class EventsManagerModule extends AbstractModule {

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
		bind(EventHandlerRegistrator.class).asEagerSingleton();
	}

	private static class EventHandlerRegistrator {
		@Inject
		EventHandlerRegistrator(EventsManager eventsManager, Set<EventHandler> eventHandlersDeclaredByModules) {
			for (EventHandler eventHandler : eventHandlersDeclaredByModules) {
				eventsManager.addHandler(eventHandler);
			}
		}
	}
}
