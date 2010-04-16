/* *********************************************************************** *
 * project: org.matsim.*
 * 
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis.checklists;

import org.matsim.run.OTFVis;
import org.matsim.vis.otfvis.OTFClientFile;

/**
 * @author florian ostermann
 */
public class T7_MVI_ConvertEventNetwork {

	
	private static String event = "./Output/OTFVisTests/QSim/ITERS/it.1/1.events.txt.gz";
	private static String network = "./Output/OTFVisTests/QSim/output_network.xml.gz";
	private static String mviFile = "./Output/OTFVisTests/QSim/OTFVis.mvi";
	
	public static void main(String[] args) {
		String[] files = new String[5];
		files[1] = event;
		files[2] = network;
		files[3] = mviFile;
		files[4] = "60";
		OTFVis.convert(files);
		new OTFClientFile(files[3]).start();		
	}
}
