/* *********************************************************************** *
 * project: org.matsim.*
 * RunSpatialAveragingManteuffel.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.scenarios.munich.analysis.spatialAvg;

import java.io.IOException;

/**
 * @author benjamin
 *
 */
public class RunSpatialAveragingManteuffel {

	private static SpatialAveragingInputData inputData;

	public static void main(String[] args) throws IOException {
		inputData = configure();
		SpatialAveragingDemandEmissions sade = new SpatialAveragingDemandEmissions();
		sade.setInputData(inputData);
		sade.run();
	}

	private static SpatialAveragingInputData configure() {
		
		return inputData;
	}
}
