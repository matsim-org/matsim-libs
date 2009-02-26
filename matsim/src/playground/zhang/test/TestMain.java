/* *********************************************************************** *
 * project: org.matsim.*
 * TestMain.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.zhang.test;

import org.matsim.controler.Controler;
import org.matsim.facilities.Facilities;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.utils.misc.Time;

import playground.ou.scenario.FileOperate;

public class TestMain {

	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			for (int i=0; i<2; i++) {
				// run the controler to produce demand
				final Controler controler = new Controler(args);
				controler.run();
				// change the network capacities according to a optimizetion function dependent on whatever
				// store it into the capacities of the link
				NetworkLayer network = controler.getNetwork();
//				for (Link l : network.getLinks().values()) {
//					double mutate = l.getCapacity(Time.UNDEFINED_TIME)*0.2- l.getCapacity(Time.UNDEFINED_TIME)*0.1;
//					mutate = mutate * MatsimRandom.random.nextDouble();
//					l.setCapacity(l.getCapacity(Time.UNDEFINED_TIME)+mutate);
//				}
				// move to old input network to a save place
				String oldNet = Gbl.getConfig().getModule("network").getValue("inputNetworkFile");
				new FileOperate().renameFile(oldNet,oldNet+"."+i);
				new FileOperate().deleteFile(oldNet);
				// clean up output folder
				String outFolder = Gbl.getConfig().getModule("controler").getValue("outputDirectory");
				new FileOperate().renameFile(outFolder,outFolder+"."+i);
				// write done the new network to the old location
				new NetworkWriter(network,oldNet).write();
				
				// clean up databases
				controler.getWorld().getLayers().clear();
				controler.getWorld().getRules().clear();
				controler.getWorld().complete();
			}
		}
	}
}
