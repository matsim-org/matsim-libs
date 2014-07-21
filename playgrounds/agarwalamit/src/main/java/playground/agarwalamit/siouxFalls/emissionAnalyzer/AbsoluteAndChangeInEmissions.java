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
package playground.agarwalamit.siouxFalls.emissionAnalyzer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.utils.io.IOUtils;

import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;

/**
 * @author amit
 */
public class AbsoluteAndChangeInEmissions {

	private static String clusterPathDesktop = "/Users/aagarwal/Desktop/ils4/agarwal/munich/output//1pct/";

	public static void main(String[] args) {

		BufferedWriter writer ;
		String eventFileLocation = "/ITERS/it.1500/1500.emission.events.xml.gz";



		SortedMap<String, Double> emissions1 = calculateTotalEmissions(clusterPathDesktop+"/baseCaseCtd/"+eventFileLocation);
		SortedMap<String, Double> emissions2 = calculateTotalEmissions(clusterPathDesktop+"/ei/"+"/ITERS/it.1500/out.xml");
		SortedMap<String, Double> emissions3 = calculateTotalEmissions(clusterPathDesktop+"/ci/"+eventFileLocation);
		SortedMap<String, Double> emissions4 = calculateTotalEmissions(clusterPathDesktop+"/eci/"+eventFileLocation);

		String [] pollutants =  emissions1.keySet().toArray(new String [0]);


		double [] r1r2 = new double [pollutants.length];
		double [] r1r3 = new double [pollutants.length];
		double [] r1r4 = new double [pollutants.length];

		try {
			writer = IOUtils.getBufferedWriter(clusterPathDesktop+"/analysis/r/rAbsoluteEmissions.txt");
			writer.write("pollutants"+"\t"+"baseCase"+"\t"+"onlyEmissions"+"\t"+"onlyCongestion"+"\t"+"both"+"\n");

			for(Entry<String, Double> e : emissions1.entrySet()){
				writer.write(e.getKey()+"\t"+e.getValue()+"\t"+
						emissions2.get(e.getKey())+"\t"+
						emissions3.get(e.getKey())+"\t"+
						emissions4.get(e.getKey())+
						"\n");
			}
			writer.close();

			writer = IOUtils.getBufferedWriter(clusterPathDesktop+"/analysis/r/rChangeInEmissions.txt");
			writer.write("pollutants"+"\t"+"onlyEmissions"+"\t"+"onlyCongestion"+"\t"+"both"+"\n");

			for(int i=0;i<pollutants.length;i++){
				r1r2[i] = ((emissions2.get(pollutants[i]) - emissions1.get(pollutants[i]))*100)/(emissions1.get(pollutants[i])) ;
				r1r3[i] = ((emissions3.get(pollutants[i]) - emissions1.get(pollutants[i]))*100)/(emissions1.get(pollutants[i])) ;
				r1r4[i] = ((emissions4.get(pollutants[i]) - emissions1.get(pollutants[i]))*100)/(emissions1.get(pollutants[i])) ;
				writer.write(pollutants[i]+"\t"+r1r2[i]+"\t"+r1r3[i]+"\t"+r1r4[i]+"\n");
			}
			writer.close();
		} catch (IOException e1) {
			throw new RuntimeException("Data is not written in file. Reason : "+e1);
		}
	}

	private static SortedMap<String,Double> calculateTotalEmissions (String emissionsEventsFile){
		EmissionsAnalyzer analyzer = new EmissionsAnalyzer(emissionsEventsFile);
		analyzer.init(null);
		analyzer.preProcessData();
		analyzer.postProcessData();
		return analyzer.getTotalEmissions();
	}
}
