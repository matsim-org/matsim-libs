/* *********************************************************************** *
 * project: org.matsim.*
 * OnTheFlyQueueSim.java
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

package org.matsim.utils.vis.otfivs.executables;

import java.io.IOException;

import org.matsim.analysis.LegHistogram;
import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueSimulation;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.otfivs.server.OTFQuadFileHandler;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;


/**
 * @author DS
 *
 */
public class OnTheFlyQueueSimWriter extends QueueSimulation{
	protected LegHistogram hist = null;

	@Override
	protected void prepareSim() {
		if (this.netStateWriter == null) this.netStateWriter = new OTFQuadFileHandler.Writer(600,this.network,"output/OTFQuadfileNoParking10p_wip.mvi");
		if(this.netStateWriter != null) this.netStateWriter.open();

		super.prepareSim();

		this.hist = new LegHistogram(300);
		events.addHandler(this.hist);

	}

	@Override
	protected void cleanupSim() {
		try {
			this.netStateWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			this.hist.writeGraphic("output/OTFQuadfileNoParking10p_wip.leghist.png");

	}


	public OnTheFlyQueueSimWriter(NetworkLayer net, Plans plans, Events events) {
		super(net, plans, events);
		// TODO Auto-generated constructor stub
	}


	public static void main(String[] args) {

		String studiesRoot = "../";
		String localDtdBase = "../matsimJ/dtd/";


		String netFileName = studiesRoot + "berlin-wip/network/wip_net.xml";
		String popFileName = studiesRoot + "berlin-wip/synpop-2006-04/kutter_population/kutter001car_hwh.routes_wip.plans.xml.gz"; // 15931 agents
//		String popFileName = studiesRoot + "berlin-wip/synpop-2006-04/kutter_population/kutter010car_hwh.routes_wip.plans.xml.gz"; // 160171 agents
//		String popFileName = studiesRoot + "berlin-wip/synpop-2006-04/kutter_population/kutter010car.routes_wip.plans.xml.gz";  // 299394 agents
		String worldFileName = studiesRoot + "berlin-wip/synpop-2006-04/world_TVZ.xml";

		Config config = Gbl.createConfig(args);

		config.global().setLocalDtdBase(localDtdBase);
		config.simulation().setFlowCapFactor(0.1);
		config.simulation().setStorageCapFactor(0.2);

		if(args.length >= 1) {
			netFileName = config.network().getInputFile();
			popFileName = config.plans().getInputFile();
			worldFileName = config.world().getInputFile();
		}

		World world = Gbl.createWorld();

		if (worldFileName != null) {
			MatsimWorldReader world_parser = new MatsimWorldReader(Gbl.getWorld());
			world_parser.readFile(worldFileName);
		}

		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(netFileName);
		world.setNetworkLayer(net);

		Plans population = new Plans();
		// Read plans file with special Reader Implementation
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(popFileName);

		Events events = new Events();

		config.simulation().setStartTime(Time.parseTime("00:00:00"));
		config.simulation().setEndTime(Time.parseTime("24:00:00"));
		config.network().setInputFile(netFileName);

		config.simulation().setSnapshotFormat("none");
		config.simulation().setSnapshotPeriod(600);
		config.simulation().setSnapshotFile("./output/remove_thisB");


		OnTheFlyQueueSimWriter sim = new OnTheFlyQueueSimWriter(net, population, events);


		sim.run();

		Gbl.printElapsedTime();


	}

	public void setOtfwriter(OTFQuadFileHandler.Writer  otfwriter) {
		this.netStateWriter = otfwriter;
	}


}
