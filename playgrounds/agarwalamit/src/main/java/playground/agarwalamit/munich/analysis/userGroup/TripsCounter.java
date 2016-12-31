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
package playground.agarwalamit.munich.analysis.userGroup;

import java.io.BufferedWriter;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.TripAndPersonCounter;

/**
 * @author amit
 */

public class TripsCounter {

	public static void main(String[] args) {

		String dir = "/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run9/";
		String runCases[] ={"baseCaseCtd","ei","ci","eci"};

		TripsCounter cnt = new TripsCounter();
		BufferedWriter writer = IOUtils.getBufferedWriter(dir+"/analysis/tripsAndPersonCountInfo.txt");

		try {
			writer.write("scenario \t numberOfCarTrips \n numberOfCarPersons \t totalTrips \t totalPesons \n");

			for(int i=0;i<runCases.length;i++){
				String eventsFile = dir+runCases[i]+"/ITERS/it.1500/1500.events.xml.gz";
				String countInfo = cnt.run(eventsFile);
				writer.write(runCases[i]+"\t"+countInfo+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}

	private String run(String eventsFile) {

		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);

		TripAndPersonCounter counter = new TripAndPersonCounter();
		events.addHandler(counter);

		reader.readFile(eventsFile);

		return counter.getNumberOfCarTrips()+"\t"+counter.getNumberOfCarPersons()+"\t"+counter.getTotalNumberOfTrips()+"\t"+counter.getTotalNumberOfPersons();
	}

}
