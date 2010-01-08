/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemsConsistencyChecker
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
package playground.cottbus;

/**
 * @author 	rschneid-btu
 * [based on tutorial.example7]
 */

import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.vis.netvis.NetVis;

public class CottbusController {
	
	public static void main (String[] args) {
		String config = "./input/denver/config.xml";
		// configuration that describes current scenario
		
		Controler controler = new Controler(config);
		controler.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		controler.setOverwriteFiles(true);
		// effects output-folder
		controler.run();
		
		// output via NetVis:
		String[] visargs = {"output/denver/ITERS/it.0/Snapshot"};
		NetVis.main(visargs);
	}
}