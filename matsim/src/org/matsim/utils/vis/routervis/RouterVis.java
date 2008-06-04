/* *********************************************************************** *
 * project: org.matsim.*
 * RouterVis.java
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

package org.matsim.utils.vis.routervis;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.Route;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;
import org.matsim.utils.vis.netvis.NetVis;
import org.matsim.utils.vis.netvis.VisConfig;

/**
 * RouterVis is a package for router visualization. It creates NetVis compatible
 * files which shows the graph exploration procedure of a LeastCostPathCalculator.
 * To visualize the graph exploration of a particular LeastCostPathColculator it has to implement
 * VisLeastCostPathCalculator.
 *
 * @author laemmel
 */
public class RouterVis {

	private static final Logger log = Logger.getLogger(RouterVis.class);

	private RouterNetStateWriter writer;

	private VisLeastCostPathCalculator router;

	/**
	 * Constructor
	 *
	 * @param network
	 * @param costCalculator
	 * @param timeCalculator
	 */
	public RouterVis(NetworkLayer network, TravelCostI costCalculator,
			TravelTimeI timeCalculator){
		this.writer = getNetStateWriter(network);
		this.router = new VisDijkstra(network,costCalculator,timeCalculator,this.writer);
	}

/**
 * Calculates the cheapest route from 'fromNode' to 'toNode' at starting time 'time' and
 * generates a NetVis file to track Dijkstra's graph exploration in the following manner:
 * While expanding a node Dijkstra's algorithm checks weather the expanded node belongs
 * to the shortest path to the succeeding node (in the sub-graph that has been explored so far).
 * If so, the link from the expanded node to the succeeding node will be colored green and
 * red otherwise. If the expanded node belongs to the shortest path to the succeeding node
 * a possible other 'green' link to this node will be colored red.
 *
 * @param fromNode
 * @param toNode
 * @param time
 *
 * @return route
 */
	public Route runRouter(Node fromNode, Node toNode, double time){
		Route route = this.router.calcLeastCostPath(fromNode, toNode, time);

		try {
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return route;
	}

	private RouterNetStateWriter getNetStateWriter(NetworkLayer network) {
		String snapshotFile = Gbl.getConfig().controler().getOutputDirectory() + "/Snapshot";

		Config config = Gbl.getConfig();
		int buffers = network.getLinks().size();
		String buffString = config.findParam("vis", "buffersize");
		if (buffString == null) {
			buffers = Math.max(5, Math.min(50000/buffers, 100));
		} else {
			buffers = Integer.parseInt(buffString);
		}

		VisConfig myVisConfig = VisConfig.newDefaultConfig();
		myVisConfig.set(VisConfig.DELAY, "100");

		RouterNetStateWriter netStateWriter = new RouterNetStateWriter(network, config.network().getInputFile(), myVisConfig, snapshotFile, 1, buffers);
		netStateWriter.open();
		return netStateWriter;
	}

	public static void main(String [] args){

		Id fromNodeId;
		Id toNodeId;

		log.info("starting RouterVis demo");
		String testConfigFile = "./examples/siouxfalls/config.xml";

		if (args.length == 3) {
			Gbl.createConfig(new String[]{args[0], "config_v1.dtd"});
			fromNodeId = new IdImpl(args[1]);
			toNodeId = new IdImpl(args[2]);

		}	else {
			log.info(" reading default config file: " + testConfigFile);
			Gbl.createConfig(new String[] {testConfigFile});
			fromNodeId = new IdImpl("13");
			toNodeId = new IdImpl("7");
		}
		log.info(" done.");


		log.info("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		log.info("  done.");

		log.info("  creating output dir if needed");
		File outputDir = new File(Gbl.getConfig().controler().getOutputDirectory());
		if (!outputDir.exists()){
			outputDir.mkdir();
		} else if (outputDir.list().length > 0) {
			log.error("The output directory " + outputDir + " exists already but has files in it! Please delete its content or the directory and start again. We will not delete or overwrite any existing files.");
			System.exit(-1);
		}
		log.info( "done");

		log.info("  creating RouterVis object.");
		TravelTimeI costCalc = new FreespeedTravelTimeCost();
		RouterVis vis = new RouterVis(network,(TravelCostI) costCalc,costCalc);
		log.info("  done.");

		log.info("  running RouterVis.");
		Node fromNode = network.getNode(fromNodeId.toString());
		Node toNode = network.getNode(toNodeId.toString());
		vis.runRouter(fromNode, toNode,0.0);
		log.info("  done.");

		log.info("  starting NetVis.");
		String [] visargs = {Gbl.getConfig().controler().getOutputDirectory() + "/Snapshot"};
		Gbl.reset();
		NetVis.main(visargs);
		log.info("  done.");
	}

}
