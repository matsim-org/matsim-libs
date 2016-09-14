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

package playground.agarwalamit.mixedTraffic.counts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.input.extDemand.OuterCordonLinks;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.OuterCordonUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.MapUtils;

/**
 * @author amit
 */

public class MultiModalCountsComperator {

	private final String inputCountsFile_urban = PatnaUtils.INPUT_FILES_DIR+"/raw/counts/urbanDemandCountsFile/innerCordon_excl_rckw_"+PatnaUtils.PATNA_NETWORK_TYPE+".txt";
	private final String inputCountsFile_external = PatnaUtils.INPUT_FILES_DIR+"/raw/counts/externalDemandCountsFile/outerCordonData_allCounts_"+PatnaUtils.PATNA_NETWORK_TYPE+".txt";
	private final int itNr = 1200;
	private final String afterITERSCountsFile = "../../../../repos/runs-svn/patnaIndia/run108/jointDemand/calibration/"+PatnaUtils.PCU_2W.toString()+"pcu/afterCadyts/c203_14/ITERS/it."+itNr+"/"+itNr+".multiMode_hourlyCounts.txt";

	private final Map<String,Set<String>> linkId2CountedModes= new HashMap<>();
	
	private final String outputFile = "../../../../repos/runs-svn/patnaIndia/run108/jointDemand/calibration/"+PatnaUtils.PCU_2W.toString()+"pcu/afterCadyts/c203_14/ITERS/it."+itNr+"/"+itNr+".multiMode_AWTVcountscompare.txt";

	private final Map<String, Map<String,Map<Integer,Double>>> link2mode2time2count_input = new HashMap<>();

	public static void main(String[] args) {
		new MultiModalCountsComperator().run();
	}

	private void run () {

		readCountsAndStoreMap(inputCountsFile_urban);
		readCountsAndStoreMap(inputCountsFile_external);

		Map<String,Map<String,Double>> link2mode2count = new HashMap<>();

		// store sim count
		try (BufferedReader reader = IOUtils.getBufferedReader(afterITERSCountsFile)) {
			String line = reader.readLine();

			while (line!=null) {
				if(line.startsWith("linkID")) {
					line = reader.readLine();
					continue;
				}

				String parts [] = line.split("\t");
				String linkId = parts[0];

				Map<String,Double> mode2count = link2mode2count.get(linkId);
				if ( mode2count == null ) mode2count = new HashMap<>();

				String mode = parts[1];

				double outCountSum = 0 ;
				for (int i=2; i<parts.length;i++){
					outCountSum+=Double.valueOf( parts[i] );
				}

				if ( mode2count.containsKey( mode ) ) throw new RuntimeException("should not happen. Aborting...");
				else mode2count.put( mode, outCountSum );

				link2mode2count.put(linkId, mode2count);
				line = reader.readLine();
			}
		} catch (IOException e) {
			throw new RuntimeException("Data is not written. Reason :"+e);
		}

		// write 
		try (BufferedWriter writer = IOUtils.getBufferedWriter(outputFile) ) {
			writer.write("linkId \t mode \t simCount \t realCount \n");

			for (String linkId : link2mode2count.keySet()) { 
				for (String mode : link2mode2count.get(linkId).keySet() ) {
					if(! linkId2CountedModes.get(linkId).contains(mode)) continue;
					writer.write(linkId+"\t"+mode+"\t"+ link2mode2count.get(linkId).get(mode)+ "\t");
					writer.write( MapUtils.doubleValueSum( link2mode2time2count_input.get(linkId).get(mode) ) + "\n");
				}
			}
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("Data is not written. Reason :"+e);
		}
	}


	private void readCountsAndStoreMap(final String inFile){
		try (BufferedReader reader = IOUtils.getBufferedReader(inFile)) {
			String line = reader.readLine();
			Map<Integer, String> index2mode = new HashMap<>();
			while (line!=null) {
				if(line.startsWith("survey")) {
					String parts[] = line.split("\t");
					for( int inx = 0; inx< parts.length;inx++){
						index2mode.put(inx, parts[inx]);
					}
					line = reader.readLine();
					continue;
				} 
				String parts [] = line.split("\t");
				String linkId = parts[1];
				
				OuterCordonLinks ocl = new OuterCordonLinks(PatnaUtils.PATNA_NETWORK_TYPE);
				Map<String, Map<Integer,Double>> mode2time2count = link2mode2time2count_input.get(linkId);

				if ( mode2time2count == null) {
					mode2time2count = new HashMap<>();
					link2mode2time2count_input.put(linkId, mode2time2count);
				} 

				Integer time = Integer.valueOf(parts[2]);

				String countinStation = ocl.getCountingStation(linkId); 
				
				// store info
				for (int i=3;i<parts.length-1;i++) { // length -1 to exclude total
					String mode = index2mode.get(i);	
					double count = Double.valueOf(parts[i]);
					
					double outTripReductionFactor = 1;
					if ( countinStation!=null && countinStation.split("_")[1].equals("P2X") ) {
						outTripReductionFactor = OuterCordonUtils.getModalOutTrafficAdjustmentFactor().get(mode);  
					}
					
					count = Math.round( outTripReductionFactor*count );
					
					if(! mode2time2count.containsKey(mode)) mode2time2count.put(mode, new HashMap<>());
					
					Map<Integer,Double> time2count = mode2time2count.get(mode);
					if (time2count.containsKey(time)) {
						time2count.put( time, time2count.get(time) + count );
					}	else time2count.put(time, count);
				}
				linkId2CountedModes.put(linkId, mode2time2count.keySet());
				line = reader.readLine();
			}

		} catch (IOException e) {
			throw new RuntimeException("Data is not written. Reason :"+e);
		}
	}
}