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

package playground.agarwalamit.mixedTraffic.patnaIndia.input.extDemand;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.OuterCordonUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.OuterCordonUtils.PatnaNetworkType;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.NumberUtils;

/**
 * Clearly, for modal counts calibration, total in-/out-traffic must be balanced for all modes.
 * 
 * @author amit
 */

public final class OuterCordonModalCountsAdjustmentCalculator{
	
	private static final Logger LOG = Logger.getLogger(OuterCordonModalCountsAdjustmentCalculator.class);

	/**
	 * mode 2 count volume in both directions.
	 */
	private final Map<String, Map<String ,Double>> direction2mode2counts = new HashMap<>();

	private final String inputFile = PatnaUtils.INPUT_FILES_DIR+"/raw/counts/externalDemandCountsFile/outerCordonData_allCounts_shpNetwork.txt";

	public static void main(String[] args) {
		new OuterCordonModalCountsAdjustmentCalculator().run();
	}


	private void run(){

		// initialize
		Map<String,Double> mode2count = new HashMap<>();
		for(String s : PatnaUtils.EXT_MAIN_MODES){
			mode2count.put(s, 0.);
		}
		mode2count.put("total", 0.);

		direction2mode2counts.put("in",  new HashMap<>(mode2count));
		direction2mode2counts.put("out", new HashMap<>(mode2count));

		processInputFile(inputFile);
		
		Map<String, Double> mode2adjustmentFactor = new HashMap<>();
		for (String mode : direction2mode2counts.get("in").keySet()){
			double inCount = direction2mode2counts.get("in").get(mode);
			double outCount = direction2mode2counts.get("out").get(mode);
			
			double factor = NumberUtils.round( (outCount - inCount )/ inCount, 2) ;
			mode2adjustmentFactor.put(mode, 1- factor);
			LOG.info("External counts for mode "+mode+" will be reduced by a factor of "+ (1- factor)+".");
		}
	}

	private void processInputFile(final String file){

		try (BufferedReader reader = IOUtils.getBufferedReader(file)) {
			String line = reader.readLine();

			List<String> mode2index = new ArrayList<>();

			while(line != null ) {
				if( line.startsWith("survey") ){
					String [] labels = line.split("\t"); // time car 2w truck cycle total
					mode2index = Arrays.asList(labels);
					line = reader.readLine();
					continue;
				}
				String direction = null;

				String parts[]	= line.split("\t");
				String linkId = parts[1];
				Id<Link> lId = Id.createLinkId(linkId);

				if ( OuterCordonUtils.getInternalToExternalCountStationLinkIds(PatnaNetworkType.shpNetwork).contains(lId) ){
					direction = "out";
				} else if ( OuterCordonUtils.getExternalToInternalCountStationLinkIds(PatnaNetworkType.shpNetwork).contains(lId) ){
					direction = "in";
				}

				Map<String,Double> mode2counter = direction2mode2counts.get(direction);

				for (int index = 3; index < parts.length; index++){
					String mode = mode2index.get(index);
					if(mode.equals("2w")) mode = "motorbike";
					else if(mode.equals("cycle")) mode = "bike";
					mode2counter.put(mode, mode2counter.get(mode)+ Double.valueOf(parts[index]) );
				}
				line = reader.readLine();	
				this.direction2mode2counts.put(direction, mode2counter);
			}
		} catch (IOException e) {
			throw new RuntimeException("Data is not written. Reason :"+e);
		}
	}

}
