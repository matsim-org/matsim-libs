/* *********************************************************************** *
 * project: org.matsim.*
 * OTFVisVisulalization.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.evacuation.run;

/**
 * This class is part of the evacuation project(GIS based risk
assessment and incident preparation system)
 * This class provides an entry point to the ``real'' OTFVisVisulalization
 * @author laemmel
 *
 */
public class OTFVisVisulalization {
	
	public static void main(String [] args) {
		if (args.length == 2) {
			new org.matsim.contrib.evacuation.visualization.OTFVisVisualization(args[0],Integer.parseInt(args[1])).run();
		}else if (args.length == 6){
			String config = args[0];
			int it = Integer.parseInt(args[1]);
			String wms = null;
			String layer = null;
			for (int i = 2; i < 6; i += 2) {
				if (args[i].equalsIgnoreCase("-wms")) {
					wms = args[i+1];
				}
				if (args[i].equalsIgnoreCase("-layer")) {
					layer = args[i+1];
				}
			}
			new org.matsim.contrib.evacuation.visualization.OTFVisVisualization(config,it,wms,layer).run();
		}else {	
			printUsage();
			System.exit(-1);
		}
	}

	protected static void printUsage() {
		System.out.println();
		System.out.println("OTFVisVisulalization");
		System.out.println("Takes the given iteration of evacuation evacuation scenario is input, removes evacuation links and nodes, an runs OTFVis in live mode with the modified scenario.\n" +
				" Works only if the population has been dumped for the given iteration-nr (if not defined otherwise the population is dumped every 10th iteration).");
		System.out.println();
		System.out.println("usage 1 : OTFVisVisulalization config-file iteration-nr");
		System.out.println("usage 2 : OTFVisVisulalization config-file iteration-nr -wms <url> -layer <layer name>");
		System.out.println();
		System.out.println("config-file:   A MATSim config file of an alreday simulated scenario.");
		System.out.println();
		System.out.println("---------------------");
		System.out.println("2011, 2012, matsim.org");
		System.out.println();
	}

}
