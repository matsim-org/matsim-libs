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
package playground.agarwalamit.munich.analysis;

import java.io.BufferedWriter;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.trip.LegModeTripTravelTimeHandler;
import playground.agarwalamit.utils.ListUtils;

/**
 * @author amit
 */

public class ModalTravelTime {

	public static void main(String[] args) {

		String dir = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run9/";
		String runCases[] ={"baseCaseCtd","ei","ci","eci"};

		for(String runCase : runCases){
			new ModalTravelTime().run(dir+runCase+"/ITERS/it.1500/1500.events.xml.gz", dir+runCase+"/analysis/modalTravelTimes.txt");
		}
		

	}

	private  void run(String eventsFile, String outputFile){
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);

		LegModeTripTravelTimeHandler timeHandler = new LegModeTripTravelTimeHandler();
		events.addHandler(timeHandler);

		reader.readFile(eventsFile);

		SortedMap<String,Map<Id<Person>,List<Double>>> mode2Person2TripTimes = timeHandler.getLegMode2PesonId2TripTimes();

		SortedMap<String, Double> mode2TotalTripTimes = new TreeMap<String, Double>();
		SortedMap<String, Integer> mode2NoOfLegs = new TreeMap<String, Integer>();

		for(String mode : mode2Person2TripTimes.keySet()){
			double modeSum = 0.;
			int modeLegs = 0;
			for(Id<Person> p : mode2Person2TripTimes.get(mode).keySet()){
				modeLegs += mode2Person2TripTimes.get(mode).get(p).size();
				modeSum = ListUtils.doubleSum(mode2Person2TripTimes.get(mode).get(p));
			}
			mode2TotalTripTimes.put(mode, modeSum);
			mode2NoOfLegs.put(mode, modeLegs);
		}

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			writer.write("mode \t totalTravelTimeInHr \t numberOfLegs \n");
			
			for(String mode : mode2TotalTripTimes.keySet()){
				writer.write(mode+"\t"+mode2TotalTripTimes.get(mode)/3600+"\t"+mode2NoOfLegs.get(mode)+"\n");
			}
			
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}
}
