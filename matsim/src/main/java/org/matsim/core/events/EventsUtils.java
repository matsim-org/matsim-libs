
/* *********************************************************************** *
 * project: org.matsim.*
 * EventsUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

 package org.matsim.core.events;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Injector;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

public class EventsUtils {

    public static EventsManager createEventsManager() {
		final EventsManagerImpl events = new EventsManagerImpl();
//		events.initProcessing();
		return events;
    }

    public static EventsManager createEventsManager(Config config) {
		final EventsManager events = Injector.createInjector( config, new EventsManagerModule() ).getInstance( EventsManager.class );
//		events.initProcessing();
		return events;
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

	public static EventsFileComparator.Result compareEventsFiles( String filename1, String filename2 ) {
		EventsFileComparator.Result result = EventsFileComparator.compare( filename1, filename2 );
		return result ;
	}

}
