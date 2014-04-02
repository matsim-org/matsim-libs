/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractEventsFilter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.analysis.eventsfilters;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;

/**
 * Class to extend to create easily events filters
 * @author thibautd
 */
public abstract class AbstractEventsFilter {
	private static final Logger log =
		Logger.getLogger(AbstractEventsFilter.class);

	private EventWriterXML writer = null;

	public void process(
			final String inputFile,
			final String outputFile) {
		this.writer = new EventWriterXML(outputFile);

		EventsManager eventsManager = EventsUtils.createEventsManager(); 
		FilterEventHandler handler = new FilterEventHandler() ;
		eventsManager.addHandler( handler );
		(new MatsimEventsReader(eventsManager)).readFile(inputFile);	

		writer.closeFile();
	}

	/**
	 * says if the event is to keep. First pass, before any sorting.
	 *
	 * @param event an event to handle
	 *
	 * @return true if the event is to keep, false if it is to trash
	 */
	abstract protected boolean acceptEvent(final EventWrapper event);

	public static class EventWrapper {
		public final Event event;
		private EventWrapper(final Event event) {
			this.event = event;
		}
	}

	private class FilterEventHandler implements BasicEventHandler {
		@Override
		public void reset(final int iteration) {
			log.info("reset called for iteration "+iteration);
		}

		@Override
		public void handleEvent(final Event event) {
			EventWrapper wrapper = new EventWrapper(event);
			if (acceptEvent(wrapper)) {
				writer.handleEvent(wrapper.event);
			}
		}
	}
}
