/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysis.pngSequence2Video;

import java.io.IOException;

/**
* @author ikaddoura
*/

public class CreateVideoRun {

	private static final String runDirectory = "../../../runs-svn/decongestion/output_final/decongestion_total500it_V8_priceUpdate1it_timeBinSize300_adjustment0.1_BrainExpBeta1.0_test/";

	public static void main(String[] args) throws IOException {
		MATSimVideoUtils.createVideo(runDirectory, 10, "tolls");
		MATSimVideoUtils.createLegHistogramVideo(runDirectory);
	}
	
}

