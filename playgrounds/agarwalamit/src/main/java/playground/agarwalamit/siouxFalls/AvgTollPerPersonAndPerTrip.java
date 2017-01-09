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
package playground.agarwalamit.siouxFalls;

import java.io.BufferedWriter;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.TripAndPersonCounter;

/**
 * @author amit
 */
public class AvgTollPerPersonAndPerTrip {

	public static void main(String[] args) {
		String runDir = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/outputMC/";
		String [] runs = {"run201","run202","run203","run204"};
		AvgTollPerPersonAndPerTrip avgTollPersonTrip = new AvgTollPerPersonAndPerTrip();
		BufferedWriter writer = IOUtils.getBufferedWriter(runDir+"/analysis500Its/tripAndPersonsCounter.txt");
		try {
			writer.write("run \t numberOfTrips \t numberOfPersons \n");
			for (String str:runs){
				String eventsFile = runDir+str+"/ITERS/it.500/500.events.xml.gz";
				int [] cntr = avgTollPersonTrip.run(eventsFile);
				writer.write(str+"\t"+cntr[0]+"\t"+cntr[1]+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to file. Reason "+e);
		}
	}

	private int [] run(String eventsFile){
		int counter [] = new int [2];
		EventsManager manager = EventsUtils.createEventsManager();
		TripAndPersonCounter tripCounter = new TripAndPersonCounter();
		manager.addHandler(tripCounter);

		MatsimEventsReader reader = new MatsimEventsReader(manager);
		reader.readFile(eventsFile);
		counter[0] = tripCounter.getNumberOfCarTrips();
		counter[1] = tripCounter.getNumberOfCarPersons();
		return counter;
	}
}
