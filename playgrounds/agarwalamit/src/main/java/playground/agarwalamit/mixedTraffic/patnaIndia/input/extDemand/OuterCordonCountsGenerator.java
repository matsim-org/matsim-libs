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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;

import playground.agarwalamit.mixedTraffic.multiModeCadyts.ModalLink;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.OuterCordonUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.MapUtils;

/**
 * @author amit
 */

public class OuterCordonCountsGenerator {

	public OuterCordonCountsGenerator(final String inputFolder) {
		INPUT_FILES_DIR = inputFolder+"/raw/counts/externalDemandCountsFile/";
	}

	private final Map<Tuple<Id<Link>,String>, Map<String, Map<Integer,Double>>> countStation2time2countInfo_in = new HashMap<>();
	private final Map<Tuple<Id<Link>,String>, Map<String, Map<Integer,Double>>> countStation2time2countInfo_out = new HashMap<>();
	private Counts<ModalLink> counts;

	private static String INPUT_FILES_DIR ;

	public static void main(String[] args) {
		OuterCordonCountsGenerator pcg = new OuterCordonCountsGenerator(PatnaUtils.INPUT_FILES_DIR);
		pcg.run();
	}
	
	public void run() {
		//		pcg.processCountingStation("OC1", inputFilesDir+"/oc1_fatua2Patna.txt", inputFilesDir+"/oc1_patna2Fatua.txt");
		processCountingStations("OC2", INPUT_FILES_DIR+"/oc2_fatua2Patna.txt", INPUT_FILES_DIR+"/oc2_patna2Fatua.txt");
		processCountingStations("OC3", INPUT_FILES_DIR+"/oc3_punpun2Patna.txt", INPUT_FILES_DIR+"/oc3_patna2punpun.txt");
		processCountingStations("OC4", INPUT_FILES_DIR+"/oc4_muz2Patna.txt", INPUT_FILES_DIR+"/oc4_patna2Muz.txt");
		processCountingStations("OC5", INPUT_FILES_DIR+"/oc5_danapur2Patna.txt", INPUT_FILES_DIR+"/oc5_patna2Danapur.txt");
		processCountingStations("OC6", INPUT_FILES_DIR+"/oc6_fatua2Noera.txt", INPUT_FILES_DIR+"/oc6_noera2Fatua.txt");
		processCountingStations("OC7", INPUT_FILES_DIR+"/oc7_danapur2Patna.txt", INPUT_FILES_DIR+"/oc7_patna2Danapur.txt");
		storeModalCounts();
	}
	
	public Counts<ModalLink> getModalLinkCounts() {
		return this.counts;
	}
	
	private void storeModalCounts(){

		for (Tuple<Id<Link>,String> mcs : countStation2time2countInfo_in.keySet()){
			for (String mode : this.countStation2time2countInfo_in.get(mcs).keySet()) {
				if(counts==null) {
					counts = new Counts<ModalLink>();
					counts.setYear(2008);
					counts.setName("Patna_counts");
					counts.setDescription(mode);
				}
				
				ModalLink ml = new ModalLink(mode, mcs.getFirst());
				Id<ModalLink> modalLinkId = Id.create(ml.getId(), ModalLink.class);
				Count<ModalLink> c = counts.createAndAddCount(modalLinkId, mcs.getSecond());
				for(Integer i : countStation2time2countInfo_in.get(mcs).get(mode).keySet()){
					double vol = countStation2time2countInfo_in.get(mcs).get(mode).get(i) ;
					c.createVolume(i, vol );
				}
			}
		}

		for (Tuple<Id<Link>,String> mcs : countStation2time2countInfo_out.keySet()){
			for (String mode : this.countStation2time2countInfo_out.get(mcs).keySet()) {

				ModalLink ml = new ModalLink(mode, mcs.getFirst());
				Id<ModalLink> modalLinkId = Id.create(ml.getId(), ModalLink.class);
				Count<ModalLink> c = counts.createAndAddCount(modalLinkId, mcs.getSecond());
				for(Integer i : countStation2time2countInfo_out.get(mcs).get(mode).keySet()){
					double vol = Math.round(countStation2time2countInfo_out.get(mcs).get(mode).get(i) * OuterCordonUtils.E2I_TRIP_REDUCTION_FACTOR);
					c.createVolume(i, vol );
					c.setCsId("mode");
				}
			}
		}
	}

	private void processCountingStations(final String countingStationNumber, final String inDirectionFile, final String outDirectionFile){
		{
			Map<String, Map<Integer, Double>> hourlyCounts = readFileAndReturnHourlyCounts(inDirectionFile);
			String key = OuterCordonUtils.getCountingStationKey(countingStationNumber, "in");
			Id<Link> linkId = new OuterCordonLinks(  PatnaUtils.PATNA_NETWORK_TYPE  ).getLinkId(key);
			countStation2time2countInfo_in.put(new Tuple<Id<Link>, String>(linkId, countingStationNumber), hourlyCounts);
		}
		{
			Map<String, Map<Integer, Double>> hourlyCounts = readFileAndReturnHourlyCounts(outDirectionFile);
			String key = OuterCordonUtils.getCountingStationKey(countingStationNumber, "out");
			Id<Link> linkId = new OuterCordonLinks(  PatnaUtils.PATNA_NETWORK_TYPE  ).getLinkId(key);
			countStation2time2countInfo_out.put(new Tuple<Id<Link>, String>(linkId, countingStationNumber), hourlyCounts);
		}
	}

	private Map<String, Map<Integer, Double>> keyReversal(Map<Integer, Map<String, Double>> inCounts ) {
		Map<String, Map<Integer, Double>> mode2bin2count = new HashMap<>();
		for(Integer ii :inCounts.keySet()) {
			for(String mode : inCounts.get(ii).keySet()){
				Map<Integer,Double> bin2count = mode2bin2count.get(mode);
				if (bin2count == null) {
					bin2count = new HashMap<>();
				}
				bin2count.put(ii, inCounts.get(ii).get(mode));
				mode2bin2count.put(mode, bin2count);
			}
		}
		return mode2bin2count;
	}

	private Map<String, Map<Integer, Double>> readFileAndReturnHourlyCounts(final String file){
		Map<Integer,  Map<String,Double>> time2count = new HashMap<>();
		try (BufferedReader reader = IOUtils.getBufferedReader(file)) {
			String line = reader.readLine();

			List<String> mode2index = new ArrayList<>();

			while(line != null ) {
				if( line.startsWith("time") ){
					String [] labels = line.split("\t"); // time car 2w truck cycle total
					mode2index = Arrays.asList(labels);
					line = reader.readLine();
					continue;
				}
				String parts[]	= line.split("\t");
				int timebin = Integer.valueOf(parts[0]);
				Map<String,Double> mode2count = new HashMap<>();

				for (int index = 1; index < parts.length; index++){
					String mode = mode2index.get(index);
					if(mode.equals("2w")) mode = "motorbike";
					else if(mode.equals("cycle")) mode = "bike";
					else if(mode.equals("total")) {
						if (MapUtils.doubleValueSum(mode2count)!=Double.valueOf(parts[index])){
							throw new RuntimeException("something went wrong. Check the modal count and total count in input file "+ file);
						}
						continue;
					}
					mode2count.put(mode, Double.valueOf(parts[index]));
				}

				time2count.put(timebin, mode2count);
				line = reader.readLine();	
			}
		} catch (IOException e) {
			throw new RuntimeException("Data is not written. Reason :"+e);
		}
		return  keyReversal(time2count );
	}
}
