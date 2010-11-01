/* *********************************************************************** *
 * project: org.matsim.*
 * IteratedFourWayVis
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
package playground.dgrether.signalsystems.otfvis;

import org.matsim.run.OTFVis;


/**
 * @author dgrether
 *
 */
public class IteratedFourWayVis {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String mvi = "test/output/org/matsim/integration/signalsystems/SignalSystemsIntegrationTest/testSignalSystems/ITERS/it.10/10.otfvis.mvi";
		
		
		String[] a = {mvi};
		OTFVis.playMVI(a);
	}

}
