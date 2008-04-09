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

package playground.david.vis;

import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.world.World;

public class PadangEventConverter {
	public static void main(String[] args) {
		if ( args.length==0 )
			args = new String[] {"./test/dstrippgen/myconfig.xml"};

		Gbl.createConfig(args);
		Gbl.startMeasurement();
		World world = Gbl.createWorld();

		String netFileName = Gbl.getConfig().getParam("network","inputNetworkFile");
		netFileName = "../../tmp/studies/padang/evacuation_net.xml";
		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(netFileName);
		world.setNetworkLayer(net);
		QueueNetworkLayer qnet = new QueueNetworkLayer(net);


		String eventFile = Gbl.getConfig().getParam("events","outputFile");
		eventFile = "../../tmp/studies/padang/0.events.txt.gz";
		OTFNetEventFileHandler test = new OTFNetEventFileHandler(10, qnet, "../../tmp/studies/padang/ds_fromEvent.vis" );
		test.run(eventFile);
	}
}
