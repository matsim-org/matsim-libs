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

package playground.agarwalamit.mixedTraffic.patnaIndia.input.joint;

import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.counts.CountsWriter;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;

/**
 * @author amit
 */

public class JointCountsWriter {
	
	private static final String urbanCountFile = PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/urban/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/urbanCounts_excl_rckw.xml.gz";
	private static final String externalCountFile = PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/external/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/outerCordonCounts_10pct_OC1Excluded.xml.gz";
	private static final String jointCountFile = PatnaUtils.INPUT_FILES_DIR+"/simulationInputs/joint/"+PatnaUtils.PATNA_NETWORK_TYPE.toString()+"/joint_counts.xml.gz";
	
	public static void main(String[] args) {

		Counts<Link> counts = new Counts<>();
		CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(counts);
		reader.readFile(urbanCountFile);
		reader.readFile(externalCountFile);
		
		new CountsWriter(counts).write(jointCountFile);
	}
}
