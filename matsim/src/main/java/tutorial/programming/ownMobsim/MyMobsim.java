/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package tutorial.programming.ownMobsim;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.vehicles.Vehicle;

/**
 * @author nagel
 *
 */
final class MyMobsim implements Mobsim {
	@Inject Scenario scenario ;
	@Inject EventsManager events ;

	@Override
	public void run() {
		for ( Node node : scenario.getNetwork().getNodes().values() ) {
			// construct mobsim node from data input
		}
		for ( Link link : scenario.getNetwork().getLinks().values() ) {
			// construct mobsim link from data input
		}
		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			// construct mobsim agents from data input 
		}

		// then run the mobsim.  time-stepped is one way; obviously there are others
		for ( long time = 0 ; time <= 36*3600 ; time++ ) {

			// ...
			
			// events are pushed into the events manager:
			Id<Person> driverId = null;
			Id<Link> linkId = null;
			Id<Vehicle> vehicleId = null;
			events.processEvent(new LinkLeaveEvent(time, driverId, linkId, vehicleId) );
			
			// ...
		}

	}

}
