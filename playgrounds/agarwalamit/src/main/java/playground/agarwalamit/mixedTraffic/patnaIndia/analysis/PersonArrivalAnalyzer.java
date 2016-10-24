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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import playground.agarwalamit.utils.AreaFilter;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * Returns number of persons arrived in 1 hour time bin
 * @author amit
 */

public class PersonArrivalAnalyzer {

	private final SortedMap<String,SortedMap<Integer, Integer>> timeBinToNumberOfArrivals = new TreeMap<>();
	private final String eventsFile;

	public PersonArrivalAnalyzer(final String eventsFile, final String configFile) {
		this.eventsFile = eventsFile;

		Config config = new Config();
		config.addCoreModules();
		new ConfigReader(config).readFile(configFile);

		Collection<String> mainModes = config.qsim().getMainModes();
		double endTime = config.qsim().getEndTime();

		for ( String mode : mainModes){
			SortedMap<Integer, Integer> bin2Nr = new TreeMap<>();
			for(int timeBin = 1; timeBin<=Math.ceil(endTime/3600);timeBin++){
				bin2Nr.put(timeBin, 0);
			}
			timeBinToNumberOfArrivals.put(mode, bin2Nr);
		}
	}

	public void run (){
		run(null,null);
	}

	public void run (final String shpFileToCheckPersonDepartures, final String networkFile){
		EventsManager manager = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(manager);

		List<Id<Person>> excludedPersonsList = new ArrayList<>();
		if (shpFileToCheckPersonDepartures!=null) {
			// if person is departed only on the given shape, it will be included in the analysis.
			AreaFilter af = new AreaFilter(shpFileToCheckPersonDepartures);
			Network network = LoadMyScenarios.loadScenarioFromNetwork(networkFile).getNetwork();
			manager.addHandler(new PersonDepartureEventHandler() {

				@Override
				public void reset(int iteration) {

				}

				@Override
				public void handleEvent(PersonDepartureEvent event) {
					Link l = network.getLinks().get(event.getLinkId());
					if ( ! af.isLinkInsideShape(l) ) excludedPersonsList.add(event.getPersonId());
				}
			});
		}

		manager.addHandler(new PersonArrivalEventHandler() {
			@Override
			public void reset(int iteration) {
			}
			@Override
			public void handleEvent(PersonArrivalEvent event) {
				
				if(excludedPersonsList.contains(event.getPersonId())) return;
				
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