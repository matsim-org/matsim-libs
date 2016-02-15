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
import java.util.SortedSet;
import java.util.TreeSet;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.analysis.PersonArrivalAnalyzer;

/**
 * @author amit
 */

public class EvacProgress {

	private final int shortestPathRunIteration = 0;
	private final int NERunIteration = 100;

	private final String dir = "../../../repos/runs-svn/patnaIndia/run105/1pct/evac_passing/";

	public static void main(String[] args) {
		new EvacProgress().runAndWrite();
	}

	public void runAndWrite (){

		String eventsFileSP = dir+"/ITERS/it."+shortestPathRunIteration+"/"+shortestPathRunIteration+".events.xml.gz";
		String eventsFileNE = dir+"/ITERS/it."+NERunIteration+"/"+NERunIteration+".events.xml.gz";

		PersonArrivalAnalyzer arrivalAnalyzer = new PersonArrivalAnalyzer(eventsFileSP, dir+"output_config.xml.gz"); 
		arrivalAnalyzer.run();
		SortedMap<String,SortedMap<Integer, Integer>> evacProgressSP = arrivalAnalyzer.getTimeBinToNumberOfArrivals();

		arrivalAnalyzer = new PersonArrivalAnalyzer(eventsFileNE, dir+"output_config.xml.gz"); 
		arrivalAnalyzer.run();
		SortedMap<String,SortedMap<Integer, Integer>>  evacProgressNE = arrivalAnalyzer.getTimeBinToNumberOfArrivals();

		String outFile = dir+"/analysis/evacuationProgress.txt";
		BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
		
		SortedSet<Integer> timeBins = new TreeSet<>();
		timeBins.addAll(evacProgressSP.get(TransportMode.car).keySet());

		try {
			writer.write("hourOfTheDay \t");//\t  \t  \n
			for (Integer ii : timeBins){
				writer.write(ii+"\t");
			}
			writer.newLine();
			
			for(String mode : evacProgressSP.keySet()){
				writer.write("numberOfEvacueeShortestPath_"+mode+"\t");
				for (Integer ii : evacProgressSP.get(mode).keySet()){
					writer.write(evacProgressSP.get(mode).get(ii)+"\t");
				}
				writer.newLine();
			}
			writer.newLine();
			
			for(String mode : evacProgressNE.keySet()){
				writer.write("numberOfEvacueeNashEq_"+mode+"\t");
				for (Integer ii : evacProgressNE.get(mode).keySet()){
					writer.write(evacProgressNE.get(mode).get(ii)+"\t");
				}
				writer.newLine();
			}

			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}
}