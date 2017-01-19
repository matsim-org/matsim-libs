/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.pt.analysis;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class ExtractTaxiAgents {
public static void main(String[] args) {
	EventsManager events = EventsUtils.createEventsManager();
	final Set<Id<Person>> taxicustomers = new HashSet<>();
	events.addHandler(new PersonDepartureEventHandler() {
		
		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void handleEvent(PersonDepartureEvent event) {
			if (event.getLegMode().equals("taxi")){
				taxicustomers.add(event.getPersonId());
			}
		}
	});
	new MatsimEventsReader(events).readFile("D:/runs-svn/intermodal/5000_routeDevel/5000_routeDevel.output_events.xml.gz");
	JbUtils.collection2Text(taxicustomers, "D:/runs-svn/intermodal/5000_routeDevel/taxicustomers.txt", null);
}
}
