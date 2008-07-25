/* *********************************************************************** *
 * project: org.matsim.*
 * OTFVis.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.run;

import java.io.IOException;

import org.matsim.config.Config;
import org.matsim.config.MatsimConfigReader;
import org.matsim.controler.ScenarioData;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.vis.otfvis.executables.OTFEvent2MVI;
import org.matsim.utils.vis.otfvis.opengl.OnTheFlyClientFileQuad;
import org.matsim.utils.vis.otfvis.opengl.OnTheFlyClientQuad;
import org.matsim.utils.vis.otfvis.opengl.OnTheFlyQueueSimQuad;

/**
 * A generic starter for the OnTheFly Visualizer that supports
 * MATSim Visualizer Files (MVI), TRANSIMS vehicle Files (VEH),
 * and MATSim Config Files for the live simulation and visualization.
 *
 * @author mrieser
 */
public class OTFVis {

	private static void printUsage() {
		System.out.println();
		System.out.println("OTFVis");
		System.out.println("Starts the MATSim OnTheFly-Visualizer.");
		System.out.println();
		System.out.println("usage 1: OTFVis mvi-file");
		System.out.println("usage 2: OTFVis veh-file network-file");
		System.out.println("usage 3: OTFVis config-file");
		System.out.println("usage 4: OTFVis -convert event-file network-file mvi-file [snapshot-period]");
		System.out.println();
		System.out.println("Usages 1-3: Starts the Visualizer");
		System.out.println("mvi-file:      A MATSim visualizer file that contains a pre-recorder state");
		System.out.println("               to be visualized (*.mvi).");
		System.out.println("veh-file:      A TRANSIMS vehicle file to be visualized (*.veh).");
		System.out.println("network-file:  A MATSim network file (*.xml).");
		System.out.println("config-file:   A complete MATSim config file to run a simulation. In that case,");
		System.out.println("               a QueueSimulation will be started and visualized in real-time, ");
		System.out.println("               allowing to interactively query the state of single agents");
		System.out.println();
		System.out.println("Usage 4: Convert events into a mvi-file");
		System.out.println("snapshot-period:  Optional. Specify how often a snapshot should be taken when");
		System.out.println("                  reading the events, in seconds. Default: 600 seconds");
		System.out.println();
		System.out.println("----------------");
		System.out.println("2008, matsim.org");
		System.out.println();
	}

	public static void main(final String[] args) {
		if (args.length == 0) {
			printUsage();
			return;
		}
		String arg0l = args[0].toLowerCase();
		if (arg0l.endsWith(".veh.gz") || arg0l.toLowerCase().endsWith(".veh")) {
			playVEH(args);
		} else if (arg0l.endsWith(".mvi")) {
			playMVI(args);
		} else if (arg0l.contains("config") && (arg0l.endsWith(".xml") || arg0l.endsWith(".xml.gz"))) {
			playConfig(args);
		} else if (arg0l.equals("-convert")) {
			convert(args);
		} else {
			printUsage();
		}
	}

	public static final void playMVI(String[] args) {
		new OnTheFlyClientFileQuad(args[0]).run();
	}

	public static final void playVEH(String[] args) {
		// we should have the network as second argument
		String vehFileName = args[0];
		if (args.length < 2) {
			printUsage();
			return;
		}
		if (args[1].toLowerCase().endsWith(".xml") || args[1].toLowerCase().endsWith(".xml.gz")) {
			String netFileName = args[1];
			if (Gbl.getConfig() == null) Gbl.createConfig(null); // is this really required?
			new OnTheFlyClientQuad("tveh:"+vehFileName + "@" + netFileName).run();
		}
	}

	public static final void playConfig(String[] args) {
		Config config = Gbl.createConfig(null);
		if ((args.length > 1) && args[1].toLowerCase().endsWith(".dtd")) {
			try {
				new MatsimConfigReader(config).readFile(args[0], args[1]);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		} else {
			new MatsimConfigReader(config).readFile(args[0]);
		}
		ScenarioData data = new ScenarioData(config);
		Events events = new Events();
		OnTheFlyQueueSimQuad client = new OnTheFlyQueueSimQuad(data.getNetwork(), data.getPopulation(), events);
		client.run();
	}

	public static final void convert(String[] args) {
		if ((args.length < 4) || (args.length > 5)) {
			printUsage();
			return;
		}
		String eventFile = args[1];
		String networkFile = args[2];
		String mviFile = args[3];
		int snapshotPeriod = 600;
		if (args.length == 5) {
			snapshotPeriod = Integer.parseInt(args[5]);
		}

		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(networkFile);

		OTFEvent2MVI converter = new OTFEvent2MVI(new QueueNetworkLayer(net), eventFile, mviFile, snapshotPeriod);
		converter.convert();
	}
}
