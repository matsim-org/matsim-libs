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
package playground.agarwalamit.mixedTraffic.patnaIndia.input;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import playground.agarwalamit.mixedTraffic.patnaIndia.PatnaConstants;

/**
 * @author amit
 */

public class PatnaCountsGenerator {
	
	private final Map<Tuple<Id<Link>,String>, Map<Integer, Double>> countStation2time2countInfo = new HashMap<>();
	
	public static void main(String[] args) {
	
		String innerCordonFile = PatnaConstants.inputFilesDir+"/innerCordon.txt";
		String outerCordonFile = PatnaConstants.inputFilesDir+"/outerCordon.txt";
		String outCountsFile = PatnaConstants.inputFilesDir+"/innerAndOuterCounts.xml.gz";
		
		PatnaCountsGenerator pcg = new PatnaCountsGenerator();
		pcg.readFileAndReturnCountInfo(innerCordonFile);
		pcg.readFileAndReturnCountInfo(outerCordonFile);
		pcg.writeCountsDataToFile(outCountsFile);
	}
	
	private void writeCountsDataToFile(String outCountsFile){
		
		Counts<Link> counts = new Counts<Link>();
		counts.setYear(2008);
		counts.setName("Patna_counts");
		counts.setDescription("OnlyOuterCordonIncludesCarMotorbikeBikeAndHeavyVehicles");
		for (Tuple<Id<Link>,String> mcs : countStation2time2countInfo.keySet()){
			Count<Link> c = counts.createAndAddCount(mcs.getFirst(), mcs.getSecond());
			for(Integer i : countStation2time2countInfo.get(mcs).keySet()){
				c.createVolume(i, countStation2time2countInfo.get(mcs).get(i));
			}
		}
		new CountsWriter(counts).write(outCountsFile);
	}
	
	private Map<Tuple<Id<Link>,String>, Map<Integer, Double>> readFileAndReturnCountInfo(String file){
		
		try (BufferedReader reader = IOUtils.getBufferedReader(file)) {
			String line = reader.readLine();
			
			while(line != null ) {
				if( line.startsWith("surveyLocation") ){
					line = reader.readLine();
					continue;
				}
				String parts [] = line.split("\t");
 				String surveyLocation = parts[0];
 				Id<Link> linkId = Id.createLinkId( parts[1] );
 				Integer time = Integer.valueOf(parts[2]);
 				Double count = Double.valueOf(parts[3]);
 				Tuple<Id<Link>,String> myCountStationInfo = new Tuple<>( linkId, surveyLocation);
 				if(countStation2time2countInfo.containsKey(myCountStationInfo)){
 					Map<Integer, Double> time2count = countStation2time2countInfo.get(myCountStationInfo);
 					time2count.put(time, count);
 				} else {
 					Map<Integer, Double> time2count = new HashMap<>();
 					time2count.put(time, count);
 					countStation2time2countInfo.put(myCountStationInfo, time2count);
 				}
 				line = reader.readLine();	
			}
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason :"+e);
		}
		return countStation2time2countInfo;
	}
}
