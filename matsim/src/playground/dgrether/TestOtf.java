/* *********************************************************************** *
 * project: org.matsim.*
 * TestOtf
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
package playground.dgrether;

import org.matsim.vis.netvis.NetVis;


/**
 * @author dgrether
 *
 */
public class TestOtf {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String runbase = "/Users/dgrether/svnworkspace/matsim/test/output/org/matsim/integration/replanning/DeterministicMultithreadedReplanningTest/testReRoute/";
		
		int iteration = 3;
		int run = 2;
		
		String runiters = "run" + run + "/ITERS/";

		final String netvis = "Snapshot";
		String otfvis = "otfvis.mvi";
		
		String filesuffix = netvis;
		
		String iter = "it." + iteration +"/" + filesuffix;
//		iter = iter + iteration + "." + filesuffix;
//		iter += filesuffix;
		
		String otfvispath = runbase + runiters + iter;
		String[] patharray = {otfvispath};
//		OTFVis.main(patharray);
		NetVis.main(patharray);
		
	}

}
