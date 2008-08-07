/* *********************************************************************** *
 * project: org.matsim.*
 * OnTheFlyQueueSimSWISS.java
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

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueSimulation;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Population;
import org.matsim.utils.misc.Time;
import org.matsim.world.World;


/**
 * @author DS
 *
 */
public class OnTheFlyQueueSimSWISS {

	public static void main(String[] args) {
		QueueSimulation sim;
		NetworkLayer net;
		Population population;
		Events events;

		String netFileName = "../../tmp/network.xml.gz";

		Gbl.createConfig(args);
		Gbl.startMeasurement();
		Config config = Gbl.getConfig();
		config.setParam("global", "localDTDBase", "dtd/");

		World world = Gbl.getWorld();

		net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(netFileName);
		world.setNetworkLayer(net);

		Gbl.printElapsedTime();

		population = new Population();

		events = new Events();

		config.simulation().setSnapshotFormat("otfvis");
		config.simulation().setSnapshotPeriod(600);
		config.simulation().setSnapshotFile("output/OTFQuadfileSCHWEIZ2.mvi");
		config.simulation().setStartTime(Time.parseTime("00:00:00"));
		config.simulation().setEndTime(Time.parseTime("00:00:11"));
		QueueNetworkLayer qnet = new QueueNetworkLayer(net);
		sim = new QueueSimulation(net, population, events);


		sim.run();

		Gbl.printElapsedTime();

	}


}
