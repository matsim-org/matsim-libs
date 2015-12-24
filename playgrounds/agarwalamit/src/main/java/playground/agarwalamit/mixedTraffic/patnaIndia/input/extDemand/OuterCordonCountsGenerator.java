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

public class OuterCordonCountsGenerator {

	private final Map<Tuple<Id<Link>,String>, Map<Integer, Double>> countStation2time2countInfo = new HashMap<>();
	private static final String inputFilesDir = PatnaUtils.INPUT_FILES_DIR+"/externalDemandInputFiles/";

	public static void main(String[] args) {

		String outCountsFile = "../../../../repos/runs-svn/patnaIndia/run108/input/"+"/outerCordonCounts_OC1Excluded.xml.gz";

		OuterCordonCountsGenerator pcg = new OuterCordonCountsGenerator();
	
//		pcg.processCountingStation("OC1", inputFilesDir+"/oc1_fatua2Patna.txt", inputFilesDir+"/oc1_patna2Fatua.txt");
		pcg.processCountingStation("OC2", inputFilesDir+"/oc2_fatua2Patna.txt", inputFilesDir+"/oc2_patna2Fatua.txt");
		pcg.processCountingStation("OC3", inputFilesDir+"/oc3_punpun2Patna.txt", inputFilesDir+"/oc3_patna2punpun.txt");
		pcg.processCountingStation("OC4", inputFilesDir+"/oc4_muz2Patna.txt", inputFilesDir+"/oc4_patna2Muz.txt");
		pcg.processCountingStation("OC5", inputFilesDir+"/oc5_danapur2Patna.txt", inputFilesDir+"/oc5_patna2Danapur.txt");
		pcg.processCountingStation("OC6", inputFilesDir+"/oc6_fatua2Noera.txt", inputFilesDir+"/oc6_noera2Fatua.txt");
		pcg.processCountingStation("OC7", inputFilesDir+"/oc7_danapur2Patna.txt", inputFilesDir+"/oc7_patna2Danapur.txt");
		
		pcg.writeCountsDataToFile(outCountsFile);
	}

	private void writeCountsDataToFile(final String outCountsFile){

		Counts<Link> counts = new Counts<Link>();
		counts.setYear(2008);
		counts.setName("Patna_counts");
		counts.setDescription("OnlyOuterCordonCountsCarMotorbikeBikeTruck");
		for (Tuple<Id<Link>,String> mcs : countStation2time2countInfo.keySet()){
			Count<Link> c = counts.createAndAddCount(mcs.getFirst(), mcs.getSecond());
			for(Integer i : countStation2time2countInfo.get(mcs).keySet()){
				c.createVolume(i, countStation2time2countInfo.get(mcs).get(i));
			}
		}
		new CountsWriter(counts).write(outCountsFile);
	}

	private void processCountingStation(final String countingStationNumber, final String inDirectionFile, final String outDirectionFile){
		{
			Map<Integer, Double> hourlyCounts = readFileAndReturnHourlyCounts(inDirectionFile);
			Id<Link> linkId = OuterCordonUtils.getCountStationLinkId(OuterCordonUtils.getCountingStationKey(countingStationNumber, "in"));
			countStation2time2countInfo.put(new Tuple<Id<Link>, String>(linkId, countingStationNumber), hourlyCounts);
		}
		{
			Map<Integer, Double> hourlyCounts = readFileAndReturnHourlyCounts(outDirectionFile);
			Id<Link> linkId = OuterCordonUtils.getCountStationLinkId(OuterCordonUtils.getCountingStationKey(countingStationNumber, "out"));
			countStation2time2countInfo.put(new Tuple<Id<Link>, String>(linkId, countingStationNumber), hourlyCounts);
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
				time2count.put(timebin, totalCount);
				line = reader.readLine();	
			}
		} catch (IOException e) {
			throw new RuntimeException("Data is not written. Reason :"+e);
		}
		return time2count;
	}
}
