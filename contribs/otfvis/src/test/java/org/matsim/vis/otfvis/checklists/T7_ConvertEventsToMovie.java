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

import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.vis.otfvis.OTFClientFile;

/**
 * @author florian ostermann
 */
public class T7_ConvertEventsToMovie {

	public static void main(String[] args) {
		String[] files = new String[5];
		files[1] = "./output/OTFVisTestsQSim/ITERS/it.0/0.events.xml.gz";
		files[2] = "./output/OTFVisTestsQSim/output_network.xml.gz";
		files[3] = "./output/OTFVisTestsQSim/OTFVis.mvi";
		files[4] = "60";
		OTFVis.convert(files);
		new OTFClientFile(files[3]).run();
	}
}
