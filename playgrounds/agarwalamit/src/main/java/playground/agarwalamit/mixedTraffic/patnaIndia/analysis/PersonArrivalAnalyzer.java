/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.patnaIndia.analysis;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

/**
 * Returns number of persons arrived in 1 hour time bin
 * @author amit
 */

public class PersonArrivalAnalyzer {

	private SortedMap<String,SortedMap<Integer, Integer>> timeBinToNumberOfArrivals = new TreeMap<>();
	private String eventsFile;

	public PersonArrivalAnalyzer(String eventsFile, String configFile) {
		this.eventsFile = eventsFile;

		Config config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).readFile(configFile);

		Collection<String> mainModes = config.qsim().getMainModes();
		double endTime = config.qsim().getEndTime();

		for ( String mode : mainModes){
			SortedMap<Integer, Integer> bin2Nr = new TreeMap<Integer, Integer>();
			for(int timeBin = 1; timeBin<=Math.ceil(endTime/3600);timeBin++){
				bin2Nr.put(timeBin, 0);
			}
			timeBinToNumberOfArrivals.put(mode, bin2Nr);
		}

	}

	public void run (){
		EventsManager manager = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(manager);

		manager.addHandler(new PersonArrivalEventHandler() {

			@Override
			public void reset(int iteration) {
			}

			@Override
			public void handleEvent(PersonArrivalEvent event) {
				double time = event.getTime();
				String mode = event.getLegMode();
				int timeBin = (int) Math.ceil(time/3600);

				SortedMap<Integer, Integer> bin2Count = timeBinToNumberOfArrivals.get(mode);
				bin2Count.put(timeBin, bin2Count.get(timeBin)+1);
				
			}
		});
		reader.readFile(eventsFile);
	}

	public SortedMap<String,SortedMap<Integer, Integer>> getTimeBinToNumberOfArrivals(){
		return timeBinToNumberOfArrivals;
	}
}
