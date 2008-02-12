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

package playground.david.vis;

import java.io.IOException;

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueSimulation;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.netvis.NetVis;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;

/**
 * @author DS
 *
 */
public class OnTheFlyQueueSim2FILE extends QueueSimulation{
	protected OnTheFlyServer myOTFServer = null;
	protected OTFQuadFileHandler.Writer  otfwriter  = null;

	@Override
	protected void prepareSim() {
		//myOTFServer = OnTheFlyServer.createInstance("AName1", network, plans);
		if (otfwriter == null) otfwriter = new OTFQuadFileHandler.Writer (60,network,"output/OTFQuadfile10p.mvi.gz");
		if(otfwriter != null) otfwriter.open();

		super.prepareSim();

		// FOR TESTING ONLY!
		//OnTheFlyClient client = new OnTheFlyClient();
		//client.start();
	}

	@Override
	protected void cleanupSim() {
		if(myOTFServer != null) myOTFServer.cleanup();
		myOTFServer = null;
		super.cleanupSim();

		try {
			if(otfwriter != null) otfwriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	}

	@Override
	public void afterSimStep(double time) {
		super.afterSimStep(time);

		try {
			if(otfwriter != null) otfwriter.dump((int)time);
		} catch (IOException e) {
			Gbl.errorMsg("QueueSimulation.dumpWriters(): Unable to dump state.");
		}

		//int status = myOTFServer.getStatus(time);

	}

	public OnTheFlyQueueSim2FILE(QueueNetworkLayer net, Plans plans, Events events) {
		super(net, plans, events);
		// TODO Auto-generated constructor stub
	}


	public static void main(String[] args) {

		String studiesRoot = "../";
		String localDtdBase = "../matsimJ/dtd/";


		String netFileName = studiesRoot + "berlin-wip/network/wip_net.xml";
//		String popFileName = studiesRoot + "berlin-wip/synpop-2006-04/kutter_population/kutter001car_hwh.routes_wip.plans.xml.gz"; // 15931 agents
		String popFileName = studiesRoot + "berlin-wip/synpop-2006-04/kutter_population/kutter010car_hwh.routes_wip.plans.xml.gz"; // 160171 agents
//		String popFileName = studiesRoot + "berlin-wip/synpop-2006-04/kutter_population/kutter010car.routes_wip.plans.xml.gz";  // 299394 agents
		String worldFileName = studiesRoot + "berlin-wip/synpop-2006-04/world_TVZ.xml";

		Config config = Gbl.createConfig(args);

		config.global().setLocalDtdBase(localDtdBase);

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

		QueueNetworkLayer net = new QueueNetworkLayer();
		new MatsimNetworkReader(net).readFile(netFileName);
		world.setNetworkLayer(net);

		Plans population = new Plans();
		// Read plans file with special Reader Implementation
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(popFileName);
		world.setPopulation(population);

		Events events = new Events() ;
		world.setEvents(events);

		config.simulation().setStartTime(Time.parseTime("00:00:00"));
		config.simulation().setEndTime(Time.parseTime("12:02:00"));
		config.network().setInputFile(netFileName);

		config.simulation().setSnapshotFormat("none");
		config.simulation().setSnapshotPeriod(10);
		config.simulation().setSnapshotFile("./output/remove_thisB");


		OnTheFlyQueueSim2FILE sim = new OnTheFlyQueueSim2FILE(net, population, events);


		sim.run();

		Gbl.printElapsedTime();

		String[] visargs = {"./output/remove_thisB"};
		NetVis.main(visargs);

	}

	public void setOtfwriter(OTFQuadFileHandler.Writer  otfwriter) {
		this.otfwriter = otfwriter;
	}


}
