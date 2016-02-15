/* *********************************************************************** *
 * project: org.matsim.*
 * PickUpDropOffFilter.java
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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * @author thibautd
 */
public class PickUpDropOffFilter {
	private static final Logger log =
		Logger.getLogger(PickUpDropOffFilter.class);

	private static final double MINIMAL_DURATION = 60 * 5;
	private static final double MAX_END_TIME = 30 * 3600;

	private static final String DEPARTURE = "passenger_departure";
	private static final String ARRIVAL = "passenger_arrival";

	private boolean useTimes;
	private final List<Event> events = new ArrayList<Event>();

	private EventWriterXML writer = null;

	public void process(
			final String inputFile,
			final String outputFile,
			final boolean useTime) {
		this.useTimes = useTime;
		this.writer = new EventWriterXML(outputFile);

		EventsManager eventsManager = EventsUtils.createEventsManager(); 
		FilterEventHandler handler = new FilterEventHandler() ;
		eventsManager.addHandler( handler );
		(new MatsimEventsReader(eventsManager)).readFile(inputFile);	

		handler.finish();
		writer.closeFile();
	}

	private class FilterEventHandler implements BasicEventHandler {
		long count = 0;

		@Override
		public void reset(final int iteration) {
			log.info("reset called for iteration "+iteration);
		}

		@Override
		public void handleEvent(final Event event) {
			if (event.getTime() > MAX_END_TIME) return;

			if (event instanceof PersonDepartureEvent) {
				PersonDepartureEvent departure = (PersonDepartureEvent) event;
				// to avoid artefacts due to overlaping activities.
				Id<Person> fakeId = Id.create(departure.getPersonId()+"-"+(count++), Person.class);

				if ( departure.getLegMode().equals( JointActingTypes.PASSENGER ) ) {
					events.add( new ActivityStartEvent(
								useTimes ? departure.getTime() : 0,
								fakeId,
								departure.getLinkId(),
								null , // facility
								DEPARTURE) );
					events.add( new ActivityEndEvent(
								useTimes ? departure.getTime() + MINIMAL_DURATION : 1000,
								fakeId,
								departure.getLinkId(),
								null , // facility
								DEPARTURE) );
				}
			}
			else if (event instanceof PersonArrivalEvent) {
				PersonArrivalEvent arrival = (PersonArrivalEvent) event;
				// to avoid artefacts due to overlaping activities.
				Id<Person> fakeId = Id.create(arrival.getPersonId()+"-"+(count++), Person.class);

				if ( arrival.getLegMode().equals( JointActingTypes.PASSENGER ) ) {
					events.add( new ActivityStartEvent(
								useTimes ? arrival.getTime() : 500,
								fakeId,
								arrival.getLinkId(),
								null , // facility
								ARRIVAL) );
					events.add( new ActivityEndEvent(
								useTimes ? arrival.getTime() + MINIMAL_DURATION : 1500,
								fakeId,
								arrival.getLinkId(),
								null , // facility
								ARRIVAL) );
				}
			}
		}

		public void finish() {
			//Collections.sort(
			//		events,
			//		new Comparator<Event>() {

			//			@Override
			//			public int compare(final Event o1, final Event o2) {
			//				return Double.compare(o1.getTime(), o2.getTime());
			//			}
			//		});

			for (Event event : events) {
				writer.handleEvent(event);
			}
		}
	}
	
	public static void main(final String[] args) {
		PickUpDropOffFilter filter = new PickUpDropOffFilter();
		int shift = 0;
		boolean useTime = true;
		if ("--no-time".equals( args[ 0 ] )) {
			useTime = false;
			shift = 1;
		}
		filter.process(args[0+shift], args[1+shift], useTime);
	}
}

