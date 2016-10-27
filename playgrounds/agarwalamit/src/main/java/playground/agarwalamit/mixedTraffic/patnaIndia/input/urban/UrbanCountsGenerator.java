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
package playground.agarwalamit.mixedTraffic.patnaIndia.input.urban;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;

/**
 * @author amit
 */

public class UrbanCountsGenerator {
	
	private final Map<Tuple<Id<Link>,String>, Map<Integer, Double>> countStation2time2countInfo = new HashMap<>();
	
	public static void main(String[] args) {
	
		String innerCordonFile = PatnaUtils.INPUT_FILES_DIR+"/raw/counts/urbanDemandCountsFile/innerCordon_excl_rckw_"+PatnaUtils.PATNA_NETWORK_TYPE+".txt";
		String outCountsFile = PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/urban/"+PatnaUtils.PATNA_NETWORK_TYPE+"/urbanCounts_excl_rckw.xml.gz";
		
		UrbanCountsGenerator pcg = new UrbanCountsGenerator();
		pcg.readFileAndStoreCountInfo(innerCordonFile);
		pcg.writeCountsDataToFile(outCountsFile);
	}
	
	private void writeCountsDataToFile(final String outCountsFile){
		
		Counts<Link> counts = new Counts<>();
		counts.setYear(2008);
		counts.setName("Patna_counts");
		counts.setDescription("OnlyUrbanCountsCarMotorbikeBikeTruck");
		for (Tuple<Id<Link>,String> mcs : countStation2time2countInfo.keySet()){
			Count<Link> c = counts.createAndAddCount(mcs.getFirst(), mcs.getSecond());
			for(Integer i : countStation2time2countInfo.get(mcs).keySet()){
                assert c != null;
                c.createVolume(i, countStation2time2countInfo.get(mcs).get(i));
			}
		}
		new CountsWriter(counts).write(outCountsFile);
	}
	
	private void readFileAndStoreCountInfo(final String file){
		
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
 				double sum = 0.;
 				for (int index = 3; index<parts.length-1;index++){
 					sum += Double.valueOf( parts[index] );
 				}
 				
 				double count = Double.valueOf(parts[parts.length-1]); 
 				if(sum!=count) throw new RuntimeException("sum of individual modal counts does not match total count.");
 				
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
		} catch (IOException e) {
			throw new RuntimeException("Data is not written. Reason :"+e);
		}
	}
}
