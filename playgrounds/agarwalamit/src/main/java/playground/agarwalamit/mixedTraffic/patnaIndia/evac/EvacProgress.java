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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.analysis.PersonArrivalAnalyzer;
import playground.agarwalamit.utils.FileUtils;

/**
 * @author amit
 */

public class EvacProgress {

    private final String runCase = "PassingQ";

    public static void main(String[] args) {
		new EvacProgress().runAndWrite();
	}

	public void runAndWrite (){

        String dir = FileUtils.RUNS_SVN + "/patnaIndia/run109/1pct/withoutHoles/evac_" + runCase;
        int shortestPathRunIteration = 0;
        String eventsFileSP = dir +"/ITERS/it."+ shortestPathRunIteration +"/"+ shortestPathRunIteration +".events.xml.gz";
        int NERunIteration = 100;
        String eventsFileNE = dir +"/ITERS/it."+ NERunIteration +"/"+ NERunIteration +".events.xml.gz";

		String networkFile = dir +"/output_network.xml.gz";
        String inputDir = FileUtils.RUNS_SVN + "/patnaIndia/run109/1pct/input/";
        String shapeFile = inputDir +"/patnaEvacAnalysisArea.shp";
//		String outputFilePrefix = "_analysisArea";
		String outputFilePrefix = "";

		PersonArrivalAnalyzer arrivalAnalyzer = new PersonArrivalAnalyzer(eventsFileSP, dir +"/output_config.xml.gz");
		arrivalAnalyzer.run();
//		arrivalAnalyzer.run(shapeFile,networkFile);
		SortedMap<String,SortedMap<Integer, Integer>> evacProgressSP = arrivalAnalyzer.getTimeBinToNumberOfArrivals();

		arrivalAnalyzer = new PersonArrivalAnalyzer(eventsFileNE, dir +"/output_config.xml.gz");
//		arrivalAnalyzer.run(shapeFile,networkFile);
		arrivalAnalyzer.run();
		SortedMap<String,SortedMap<Integer, Integer>>  evacProgressNE = arrivalAnalyzer.getTimeBinToNumberOfArrivals();

		String outFile = dir +"/analysis/evacuationProgress"+outputFilePrefix+".txt";
		
		SortedSet<Integer> timeBins = new TreeSet<>();
		timeBins.addAll(evacProgressSP.get(TransportMode.car).keySet());

		try(BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
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
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
		
		outFile = dir +"/analysis/evacuationProgress"+outputFilePrefix+"_ggplot.txt";
		Map<String,double[]> evacSP = getCummulative(evacProgressSP);
		Map<String,double[]> evacNE = getCummulative(evacProgressNE);
		
		try(BufferedWriter writer = IOUtils.getBufferedWriter(outFile)){
			writer.write("hourOfTheDay\tmode\tevacueeSP\tevacueeNE\trunCase\n");
			for(Integer timebin : timeBins){
				for(String mode : evacSP.keySet()){
					writer.write(timebin+"\t"+mode+"\t"+evacSP.get(mode)[timebin-1]+"\t"+evacNE.get(mode)[timebin-1]+"\t"+runCase+"\n");
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
	}
	
	private static Map<String,double[]> getCummulative(SortedMap<String,SortedMap<Integer, Integer>> inMap){
		Map<String,double[]> outMap = new HashMap<>();
		double allModes [] = new double [inMap.get("car").size()];
		Arrays.fill(allModes, 0.0);
		
		for (String mode : inMap.keySet()) {
			double d [] = new double [inMap.get(mode).size()];
			for(int index = 1; index <= d.length; index++){
				if(index==1)  {
					d[index-1] = inMap.get(mode).get(index);
					allModes[index-1] = allModes[index-1] + d[index-1];
				} else {
					d[index-1] = d[index-2] + inMap.get(mode).get(index);
					allModes[index-1] = allModes[index-1] + d[index-1];
				}
			}
			outMap.put(mode, d);
		}
		outMap.put("all modes", allModes);
		return outMap;
	}
	
}