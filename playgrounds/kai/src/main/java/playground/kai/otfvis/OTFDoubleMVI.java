/* *********************************************************************** *
 * project: org.matsim.*
 * OTFDoubleMVI.java
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

package playground.kai.otfvis;


public class OTFDoubleMVI {

	public static void main( String[] args) {

		String filename;
		String filename2;
		
		if (args.length == 2) {
			filename = args[0];
			filename2 = args[1];
		} else {
			
			// use myDoubleOTFVis instead !!!!
			
			
//			filename = "../MatsimJ/output/OTFQuadfileNoParking10p_wip.mvi.gz";
			filename2 = "output/OTFQuadfile10p.mvi";
			filename = "testCUDA10p.mvi";
//			filename = "../../tmp/1000.events.mvi";
//			filename = "/TU Berlin/workspace/MatsimJ/output/OTFQuadfileNoParking10p_wip.mvi";
//			filename = "/TU Berlin/workspace/MatsimJ/otfvisSwitzerland10p.mvi";
//			filename = "testCUDA10p.mvi";
		}

		String[] a = {filename, filename2};
		org.matsim.vis.otfvis.OTFDoubleMVI.main(a);
	}

}

