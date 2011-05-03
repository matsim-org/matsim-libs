/* *********************************************************************** *
 * project: org.matsim.*
 * RunCountPossibleSharedRides.java
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
package playground.thibautd.analysis.possiblesharedrides;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

/**
 * Executable wrapper for {@link CountPossibleSharedRides}
 * @author thibautd
 */
public class RunCountPossibleSharedRides {
	/**
	 * Usage: RunCountPossibleSharedRides eventFile configFile.
	 *
	 * configFile must just define the network and the (output) plan file to consider.
	 * This is dirty, but this is the simplest (if not the only) way to import the
	 * network.
	 */
	public static void main(String[] args) {
		double acceptableDistance = 500d;
		double timeWindow = 15*60d;
		String eventFile;
		String fakeConfig;
		//String networkFile;
		//String plansFile;

		try {
			eventFile = args[0];
			fakeConfig = args[1];
			//networkFile = args[1];
			//plansFile = args[2];
		} catch (Exception e) {
			throw new IllegalArgumentException(e.getMessage());
		}

		EventsManager eventsManager = (EventsManager) EventsUtils.createEventsManager();
		EventsAccumulator eventsAccumulator = new EventsAccumulator();
		Controler dummyControler = new Controler(fakeConfig);
		Network network = dummyControler.getScenario().getNetwork();
		Population population = dummyControler.getScenario().getPopulation();

		eventsManager.addHandler(eventsAccumulator);
		
		(new MatsimEventsReader(eventsManager)).readFile(eventFile);

		CountPossibleSharedRides countRides = new CountPossibleSharedRides(
				network, eventsAccumulator, population, acceptableDistance, timeWindow);

		// TODO: process
		countRides.run();
	}
}

