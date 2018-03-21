package org.matsim.core.events;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Injector;

public class EventsUtils {

    public static EventsManager createEventsManager() {
        return new EventsManagerImpl();
    }

    public static EventsManager createEventsManager(Config config) {
        return Injector.createInjector(config, new EventsManagerModule())
                .getInstance(EventsManager.class);
    }

    /**
     * The SimStepParallelEventsManagerImpl can handle events from multiple threads.
     * The (Parallel)EventsMangerImpl cannot, therefore it has to be wrapped into a
     * SynchronizedEventsManagerImpl.
     */
    public static EventsManager getParallelFeedableInstance(EventsManager events) {
    	if (events instanceof SimStepParallelEventsManagerImpl) {
    		return events;
    	} else if (events instanceof ParallelEventsManager) {
    		return events;
    	}
    	else if (events instanceof SynchronizedEventsManagerImpl) {
    		return events;
    	} else {
    		return new SynchronizedEventsManagerImpl(events);
    	}
    }
    public static void readEvents( EventsManager events, String filename ) {
    	new MatsimEventsReader(events).readFile(filename) ;
	}
}
