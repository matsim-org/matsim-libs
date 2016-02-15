/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateActivityEventsForParkedVehicles.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.parknride.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;
import playground.thibautd.parknride.ParkAndRideConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses an event file, and creates an event file with activity start
 * and end events for parking and unparking (allows to visualize parking
 * occupancy in VIA).
 *
 * @author thibautd
 */
public class GenerateActivityEventsForParkedVehicles {
	public static final String ACTIVITY_TYPE = "parking";
	public static final String NO_TIME_FLAG = "--no-time";

	public static void main(final String[] args) {
		int shift = 0;
		boolean useTime = true;
		if ( args[ 0 ].equals( NO_TIME_FLAG ) ) {
			useTime = false;
			shift = 1;
		}
		String inFile = args[ shift + 0 ];
		String outFile = args[ shift + 1 ];

		EventsManager events = EventsUtils.createEventsManager();
		EventWriterXML writer = new EventWriterXML( outFile );
		events.addHandler( new EventsInterpreter( writer , useTime ) );
		(new MatsimEventsReader( events )).readFile( inFile );
		writer.closeFile();
	}
	
	private static class EventsInterpreter implements ActivityStartEventHandler {
		private final List<Id> parkedAgents = new ArrayList<Id>();
		private final BasicEventHandler eventWriter;
		private final boolean useTime;
		// via does strange things if an agent is at several places at the same time.
		// THis happend if time info is dropped to get daily parking occupation
		private final Map<Id, Integer> counts = new HashMap<Id, Integer>();

		public EventsInterpreter(
				final BasicEventHandler eventWriter,
				final boolean useTime) {
			this.eventWriter = eventWriter;
			this.useTime = useTime;
		}

		@Override
		public void reset(int iteration) {}

		@Override
		public void handleEvent(final ActivityStartEvent event) {
			if (event.getActType().equals( ParkAndRideConstants.PARKING_ACT )) {
				if (parkedAgents.remove( event.getPersonId() )) {
					// the agent was parked
					Integer c = counts.get( event.getPersonId() );

					eventWriter.handleEvent(
							new ActivityEndEvent(useTime ? event.getTime() : 100, useTime ?
							event.getPersonId() :
							Id.create( event.getPersonId()+"->"+c , Person.class ), event.getLinkId(), event.getFacilityId(), ACTIVITY_TYPE) );
				}
				else {
					// the agent was not parked
					Integer c = counts.get( event.getPersonId() );
					c = c == null ? 1 : c + 1;
					counts.put( event.getPersonId() , c );

					eventWriter.handleEvent(
							new ActivityStartEvent(useTime ? event.getTime() : 0, useTime ?
							event.getPersonId() :
							Id.create( event.getPersonId()+"->"+c , Person.class ), event.getLinkId(), event.getFacilityId(), ACTIVITY_TYPE) );
					parkedAgents.add( event.getPersonId() );

				}
			}
		}
	}

}

