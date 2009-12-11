/* *********************************************************************** *
 * project: org.matsim.*
 * PadangEventConverter.java
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

package playground.david.otfvis;

import org.matsim.core.gbl.Gbl;
import org.matsim.ptproject.qsim.QueueNetwork;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.run.OTFVis;
import org.matsim.vis.otfvis.executables.OTFEvent2MVI;
import org.matsim.world.World;

//Usage ConfigEventConverter event-file config-file mvi-file
public class ConfigEventConverter {
	public static void main(String[] args) {
		if ( args.length != 3  ){
			System.out.println("Wrong argument count: Usage ConfigEventConverter event-file config-file mvi-file");
			System.exit(0);
		}
		
		Gbl.createConfig(new String[]{args[1]});

		String netFileName = Gbl.getConfig().getParam("network","inputNetworkFile");
		Double period = Gbl.getConfig().simulation().getSnapshotPeriod();
		if(period == 0.0) period = 600.; // in the movie writing a period of zero does not make sense, use default value 
		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(netFileName);

		OTFEvent2MVI converter = new OTFEvent2MVI(new QueueNetwork(net), args[0], args[2], period);
		converter.convert();

	}
}
