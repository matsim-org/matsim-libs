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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.tripDistance.TripDistanceHandler;
import playground.agarwalamit.utils.ListUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class ModalTravelDistance {

	public static void main(String[] args) {

		String dir = "../../../../repos/runs-svn/patnaIndia/run108/jointDemand/calibration/shpNetwork/c1/";
//		String runCases[] ={"baseCaseCtd","ei","ci","eci"};

//		for(String runCase : runCases){
			new ModalTravelDistance().run(dir+"/ITERS/it.100/100.events.xml.gz", dir);
//		}
	}

	private  void run(String eventsFile, String outputDir){
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);

		Scenario sc = LoadMyScenarios.loadScenarioFromNetworkAndConfig(outputDir+"/output_network.xml.gz", outputDir+"/output_config.xml.gz");
		
		TripDistanceHandler distHandler = new TripDistanceHandler(sc);
		events.addHandler(distHandler);

		reader.readFile(eventsFile);

		SortedMap<String,Map<Id<Person>,List<Double>>> mode2Person2TripDistances = distHandler.getMode2PersonId2TravelDistances();

		SortedMap<String, Double> mode2TotalTripDists = new TreeMap<>();
		SortedMap<String, Integer> mode2NoOfLegs = new TreeMap<>();

		for(String mode : mode2Person2TripDistances.keySet()){
			double modeSum = 0.;
			int modeLegs = 0;
			for(Id<Person> p : mode2Person2TripDistances.get(mode).keySet()){
				modeLegs += mode2Person2TripDistances.get(mode).get(p).size();
				modeSum += ListUtils.doubleSum(mode2Person2TripDistances.get(mode).get(p));
			}
			mode2TotalTripDists.put(mode, modeSum);
			mode2NoOfLegs.put(mode, modeLegs);
		}

		BufferedWriter writer = IOUtils.getBufferedWriter(outputDir+"/analysis/modalTravelDistances.txt");
		try {
			writer.write("mode \t totalTravelDistanceInKm \t numberOfLegs \n");
			
			for(String mode : mode2TotalTripDists.keySet()){
				writer.write(mode+"\t"+mode2TotalTripDists.get(mode)/1000+"\t"+mode2NoOfLegs.get(mode)+"\n");
			}
			
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}
}
