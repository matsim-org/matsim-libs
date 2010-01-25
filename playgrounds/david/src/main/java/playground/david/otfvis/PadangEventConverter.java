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

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.ptproject.qsim.QNetwork;
import org.matsim.vis.otfvis.executables.OTFEvent2MVI;


public class PadangEventConverter {
	public static void main(String[] args) {
		if ( args.length==0 )
			args = new String[] {"./test/dstrippgen/myconfig.xml"};

		ScenarioImpl scenario = new ScenarioLoaderImpl(args[0]).getScenario();
		Config config = scenario.getConfig();
		Gbl.startMeasurement();

		String netFileName = config.getParam("network","inputNetworkFile");
		netFileName = "../../tmp/studies/padang/evacuation_net.xml";
		NetworkLayer net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFileName);
		QNetwork qnet = new QNetwork(net);

		String eventFile = config.getParam("events","outputFile");
		eventFile = "../../tmp/studies/padang/0.events.txt.gz";
		OTFEvent2MVI test = new OTFEvent2MVI(qnet, eventFile, "../../tmp/studies/padang/ds_fromEvent.vis", 10);
		test.convert();
	}
}
