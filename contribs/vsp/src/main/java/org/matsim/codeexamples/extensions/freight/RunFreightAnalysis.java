/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.codeexamples.extensions.freight;

import org.matsim.freight.carriers.analysis.CarriersAnalysis;

import java.util.concurrent.ExecutionException;


/**
 * @see org.matsim.freight.carriers
 */
public class RunFreightAnalysis {

	public static void main(String[] args) throws ExecutionException, InterruptedException{
		run(args, false);
	}
	public static void run( String[] args, boolean runWithOTFVis ) throws ExecutionException, InterruptedException{

		CarriersAnalysis analysis = new CarriersAnalysis(
				"MA_output\\byPopulationAndAge_demandPerPerson_1pt\\",
				"MA_output\\byPopulationAndAge_demandPerPerson_1pt\\analysis", null, "EPSG:25832");
		try {
			analysis.runCarrierAnalysis(CarriersAnalysis.CarrierAnalysisType.carriersAndEvents);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
