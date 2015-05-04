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
package playground.agarwalamit.mixedTraffic.patnaIndia.evac;

import java.io.BufferedWriter;
import java.util.SortedMap;

import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.analysis.PersonArrivalAnalyzer;


/**
 * @author amit
 */

public class EvacProgress {

	private int shortestPathRunIteration = 0;
	private int NERunIteration = 100;

	private String dir = "../../../repos/runs-svn/patnaIndia/run105/1pct/evac_seepage/";


	public static void main(String[] args) {
		new EvacProgress().runAndWrite();
	}


	public void runAndWrite (){

		String eventsFile_sp = dir+"/ITERS/it."+shortestPathRunIteration+"/"+shortestPathRunIteration+".events.xml.gz";
		String eventsFile_ne = dir+"/ITERS/it."+NERunIteration+"/"+NERunIteration+".events.xml.gz";

		PersonArrivalAnalyzer arrivalAnalyzer = new PersonArrivalAnalyzer(eventsFile_sp, dir+"output_config.xml.gz"); 
		arrivalAnalyzer.run();
		SortedMap<String,SortedMap<Integer, Integer>> evacProgress_sp = arrivalAnalyzer.getTimeBinToNumberOfArrivals();

		arrivalAnalyzer = new PersonArrivalAnalyzer(eventsFile_ne, dir+"output_config.xml.gz"); 
		arrivalAnalyzer.run();
		SortedMap<String,SortedMap<Integer, Integer>>  evacProgress_ne = arrivalAnalyzer.getTimeBinToNumberOfArrivals();

		String outFile = dir+"/analysis/evacuationProgress.txt";
		BufferedWriter writer = IOUtils.getBufferedWriter(outFile);

		try {
			writer.write("hourOfTheDay \t travelMode \t numberOfEvacueeShortestPath \t numberOfEvacueeNashEq \n");

			for(String mode : evacProgress_sp.keySet()){
				for (Integer ii : evacProgress_sp.get(mode).keySet()){
					writer.write(ii+"\t"+mode+"\t"+evacProgress_sp.get(mode).get(ii)+"\t"+evacProgress_ne.get(mode).get(ii));
					writer.newLine();
				}
			}

			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}

	}
}
