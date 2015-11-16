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

	private int shortestPathRunIteration = 0;
	private int NERunIteration = 100;

	private String dir = "../../../repos/runs-svn/patnaIndia/run105/1pct/evac_passing/";


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
		
		SortedSet<Integer> timeBins = new TreeSet<>();
		timeBins.addAll(evacProgress_sp.get(TransportMode.car).keySet());

		try {
			writer.write("hourOfTheDay \t");//\t  \t  \n
			for (Integer ii : timeBins){
				writer.write(ii+"\t");
			}
			writer.newLine();
			
			for(String mode : evacProgress_sp.keySet()){
				writer.write("numberOfEvacueeShortestPath_"+mode+"\t");
				for (Integer ii : evacProgress_sp.get(mode).keySet()){
					writer.write(evacProgress_sp.get(mode).get(ii)+"\t");
				}
				writer.newLine();
			}
			writer.newLine();
			
			for(String mode : evacProgress_ne.keySet()){
				writer.write("numberOfEvacueeNashEq_"+mode+"\t");
				for (Integer ii : evacProgress_ne.get(mode).keySet()){
					writer.write(evacProgress_ne.get(mode).get(ii)+"\t");
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
