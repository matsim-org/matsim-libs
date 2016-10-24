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
package playground.agarwalamit.mixedTraffic.patnaIndia.input.extDemand;

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

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.OuterCordonUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;

/**
 * @author amit
 */

public class OuterCordonCountsWriter {

	private final Map<Tuple<Id<Link>,String>, Map<Integer, Double>> countStation2time2countInfo_in = new HashMap<>();
	private final Map<Tuple<Id<Link>,String>, Map<Integer, Double>> countStation2time2countInfo_out = new HashMap<>();
	private static final String INPUT_FILES_DIR = PatnaUtils.INPUT_FILES_DIR+"/raw/counts/externalDemandCountsFile/";
	
	public static void main(String[] args) {

		String outCountsFile = PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/external/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/outerCordonCounts_10pct_OC1Excluded.xml.gz";

		OuterCordonCountsWriter pcg = new OuterCordonCountsWriter();
	
//		pcg.processCountingStation("OC1", inputFilesDir+"/oc1_fatua2Patna.txt", inputFilesDir+"/oc1_patna2Fatua.txt");
		pcg.processCountingStation("OC2", INPUT_FILES_DIR+"/oc2_fatua2Patna.txt", INPUT_FILES_DIR+"/oc2_patna2Fatua.txt");
		pcg.processCountingStation("OC3", INPUT_FILES_DIR+"/oc3_punpun2Patna.txt", INPUT_FILES_DIR+"/oc3_patna2punpun.txt");
		pcg.processCountingStation("OC4", INPUT_FILES_DIR+"/oc4_muz2Patna.txt", INPUT_FILES_DIR+"/oc4_patna2Muz.txt");
		pcg.processCountingStation("OC5", INPUT_FILES_DIR+"/oc5_danapur2Patna.txt", INPUT_FILES_DIR+"/oc5_patna2Danapur.txt");
		pcg.processCountingStation("OC6", INPUT_FILES_DIR+"/oc6_fatua2Noera.txt", INPUT_FILES_DIR+"/oc6_noera2Fatua.txt");
		pcg.processCountingStation("OC7", INPUT_FILES_DIR+"/oc7_danapur2Patna.txt", INPUT_FILES_DIR+"/oc7_patna2Danapur.txt");
		
		pcg.writeCountsDataToFile(outCountsFile);
	}

	private void writeCountsDataToFile(final String outCountsFile){
		Counts<Link> counts = new Counts<>();
		counts.setYear(2008);
		counts.setName("Patna_counts");
		counts.setDescription("OnlyOuterCordonCountsCarMotorbikeBikeTruck");
		for (Tuple<Id<Link>,String> mcs : countStation2time2countInfo_in.keySet()){
			Count<Link> c = counts.createAndAddCount(mcs.getFirst(), mcs.getSecond());
			for(Integer i : countStation2time2countInfo_in.get(mcs).keySet()){
				double vol = countStation2time2countInfo_in.get(mcs).get(i) ;
				assert c != null;
				c.createVolume(i, vol );
			}
		}
		for (Tuple<Id<Link>,String> mcs : countStation2time2countInfo_out.keySet()){
			Count<Link> c = counts.createAndAddCount(mcs.getFirst(), mcs.getSecond());
			for(Integer i : countStation2time2countInfo_out.get(mcs).keySet()){
				double vol = Math.round(countStation2time2countInfo_out.get(mcs).get(i) 
						* OuterCordonUtils.getModalOutTrafficAdjustmentFactor().get("total")); // this counts file is aggregated and therefore using aggregated factor
				assert c != null;
				c.createVolume(i, vol );
			}
		}
		new CountsWriter(counts).write(outCountsFile);
	}

	private void processCountingStation(final String countingStationNumber, final String inDirectionFile, final String outDirectionFile){
		{
			Map<Integer, Double> hourlyCounts = readFileAndReturnHourlyCounts(inDirectionFile);
			String key = OuterCordonUtils.getCountingStationKey(countingStationNumber, "in");
			Id<Link> linkId = new OuterCordonLinks(  PatnaUtils.PATNA_NETWORK_TYPE  ).getLinkId(key);
			countStation2time2countInfo_in.put(new Tuple<>(linkId, countingStationNumber), hourlyCounts);
		}
		{
			Map<Integer, Double> hourlyCounts = readFileAndReturnHourlyCounts(outDirectionFile);
			String key = OuterCordonUtils.getCountingStationKey(countingStationNumber, "out");
			Id<Link> linkId = new OuterCordonLinks(  PatnaUtils.PATNA_NETWORK_TYPE  ).getLinkId(key);
			countStation2time2countInfo_out.put(new Tuple<>(linkId, countingStationNumber), hourlyCounts);
		}
	}

	private Map<Integer, Double> readFileAndReturnHourlyCounts(final String file){
		Map<Integer, Double> time2count = new HashMap<>();
		try (BufferedReader reader = IOUtils.getBufferedReader(file)) {
			String line = reader.readLine();

			while(line != null ) {
				if( line.startsWith("time") ){
					line = reader.readLine();
					continue;
				}
				String parts[]	= line.split("\t");
				int timebin = Integer.valueOf(parts[0]);

				double totalCount = 0.;
				for ( int ii =1 ; ii<=4; ii++){
					totalCount += Double.valueOf(parts[ii]);
				}
				
				if(totalCount!=Double.valueOf(parts[5])) throw new RuntimeException("something went wrong. Check the modal count and total count in input file "+ file);
				
				time2count.put(timebin, totalCount);
				line = reader.readLine();	
			}
		} catch (IOException e) {
			throw new RuntimeException("Data is not written. Reason :"+e);
		}
		return time2count;
	}
}
