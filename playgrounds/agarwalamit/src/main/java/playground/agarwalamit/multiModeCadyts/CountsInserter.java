/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.multiModeCadyts;

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

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.OuterCordonUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.NumberUtils;

/**
 * A class to read the text file for hourly, mode-specific counts and prepare them to insert in multi-mode cadyts context.
 * As of now, this has some settings for Patna scenario, however, as long as the links in another scenario does not match
 * Patna network, this will not have any effect.
 * 
 * @author amit
 */

public class CountsInserter {

	private final Map<Tuple<Id<Link>,String>, Map<String, Map<Integer,Double>>> countStation2time2countInfo = new HashMap<>();
	
	private Counts<ModalLink> counts = new Counts<>();
	private final Map<String,ModalLink> mappedModalLink = new HashMap<>(); // central database of objects to retrieve, if needed. (similar to links in the scenario)

	public Map<String, ModalLink> getModalLinkContainer() {
		return mappedModalLink;
	}

	public static void main(String[] args) {
		String inputFolder = PatnaUtils.INPUT_FILES_DIR;
		CountsInserter jcg = new CountsInserter();
		jcg.processInputFile( inputFolder+"/raw/counts/urbanDemandCountsFile/innerCordon_excl_rckw_shpNetwork.txt" );
		jcg.processInputFile( inputFolder+"/raw/counts/externalDemandCountsFile/outerCordonData_allCounts_shpNetwork.txt" );
		jcg.run();
	}

	public Counts<ModalLink> getModalLinkCounts() {
		return this.counts;
	}

	public void run(){
		for (Tuple<Id<Link>,String> mcs : countStation2time2countInfo.keySet()){
			for (String mode : this.countStation2time2countInfo.get(mcs).keySet()) {
				if(mode.equals("total")) continue;				
				if(counts==null) {
					counts = new Counts<>();
				}

				ModalLink ml = new ModalLink(mode, mcs.getFirst()); 
				Id<ModalLink> modalLinkId = Id.create(ml.getId(), ModalLink.class);
				mappedModalLink.put(modalLinkId.toString(), ml);
				
				Count<ModalLink> c = counts.createAndAddCount(modalLinkId, mcs.getSecond());
				for(Integer i : countStation2time2countInfo.get(mcs).get(mode).keySet()){
					double vol = countStation2time2countInfo.get(mcs).get(mode).get(i) ;
					assert c != null;
					c.createVolume(i, vol );
				}
			}
		}
	}

	public void processInputFile(final String file){
		Map<Integer,  Map<String,Double>> time2count = new HashMap<>();
		try (BufferedReader reader = IOUtils.getBufferedReader(file)) {
			String line = reader.readLine();

			List<String> mode2index = new ArrayList<>();
			Tuple<Id<Link>,String> link2stationNumber = null;

			while(line != null ) {
				if( line.startsWith("survey") ){
					String [] labels = line.split("\t"); // time car 2w truck cycle total
					mode2index = Arrays.asList(labels);
					line = reader.readLine();
					continue;
				}

				String parts[]	= line.split("\t");
				String surveyLocation = parts[0];
				String linkId = parts[1];

				link2stationNumber = new Tuple<>(Id.createLinkId(linkId), surveyLocation);

				int timebin = Integer.valueOf(parts[2]);
				Map<String,Double> mode2count = new HashMap<>();

				for (int index = 3; index < parts.length; index++){
					String mode = mode2index.get(index);
					if(mode.equals("2w")) mode = "motorbike";
					else if(mode.equals("cycle")) mode = "bike";
					
					double countAdjustmentFactor = 1; // only for out external traffic.
					if(OuterCordonUtils.getInternalToExternalCountStationLinkIds(PatnaUtils.PATNA_NETWORK_TYPE).contains(Id.createLinkId(linkId))){
						countAdjustmentFactor = OuterCordonUtils.getModalOutTrafficAdjustmentFactor().get(mode);
					}
					mode2count.put(mode, NumberUtils.round( countAdjustmentFactor * Double.valueOf(parts[index]), 0) );
				}

				time2count.put(timebin, mode2count);
				line = reader.readLine();	

				// store the data here
				this.countStation2time2countInfo.put(link2stationNumber, keyReversal(time2count));
			}
		} catch (IOException e) {
			throw new RuntimeException("Data is not written. Reason :"+e);
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
}