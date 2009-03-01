/* *********************************************************************** *
 * project: org.matsim.*
 * OnTheFlyQueueSimWriter.java
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

package org.matsim.utils.vis.otfvis.executables;

import org.matsim.events.Events;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.network.NetworkLayer;

/* set to deprecated on 20081104. If you still have a reason to use this class
 * instead of org.matsim.run.OTFVis, then please tell me so. Otherwise I might
 * delete this class completely after some while... / marcel,20081104
 * 
 * commented out most of the code to force people switch to OTFVis / marcel,20090301
 */
/**
 * @author DS
 *
 * @deprecated please use org.matsim.run.OTFVis to start the visualizer
 */
@Deprecated
public class OnTheFlyQueueSimWriter extends QueueSimulation{
//	protected LegHistogram hist = null;
//
//	@Override
//	protected void prepareSim() {
//		if (this.netStateWriter == null) this.netStateWriter = new OTFQuadFileHandler.Writer(600,this.network,"output/OTFQuadfileNoParking10p_wip.mvi");
//		if(this.netStateWriter != null) this.netStateWriter.open();
//
//		super.prepareSim();
//
//		this.hist = new LegHistogram(300);
//		events.addHandler(this.hist);
//
//	}
//
//	@Override
//	protected void cleanupSim() {
//		try {
//			this.netStateWriter.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//			this.hist.writeGraphic("output/OTFQuadfileNoParking10p_wip.leghist.png");
//	}


	public OnTheFlyQueueSimWriter(final NetworkLayer net, final Population plans, final Events events) {
		super(net, plans, events);
		throw new RuntimeException("This class is deprecated, please use org.matsim.run.OTFVis.");
	}


	public static void main(final String[] args) {
		throw new RuntimeException("This class is deprecated, please use org.matsim.run.OTFVis.");
//		
//		String studiesRoot = "../";
//		String localDtdBase = "../matsimJ/dtd/";
//
//		// FIXME_ hard-coded filenames
//		String netFileName = studiesRoot + "berlin-wip/network/wip_net.xml";
//		String popFileName = studiesRoot + "berlin-wip/synpop-2006-04/kutter_population/kutter001car_hwh.routes_wip.plans.xml.gz"; // 15931 agents
////		String popFileName = studiesRoot + "berlin-wip/synpop-2006-04/kutter_population/kutter010car_hwh.routes_wip.plans.xml.gz"; // 160171 agents
////		String popFileName = studiesRoot + "berlin-wip/synpop-2006-04/kutter_population/kutter010car.routes_wip.plans.xml.gz";  // 299394 agents
//		String worldFileName = studiesRoot + "berlin-wip/synpop-2006-04/world_TVZ.xml";
//
//		Config config = Gbl.createConfig(args);
//
//		config.global().setLocalDtdBase(localDtdBase);
//		config.simulation().setFlowCapFactor(0.1);
//		config.simulation().setStorageCapFactor(0.2);
//
//		if(args.length >= 1) {
//			netFileName = config.network().getInputFile();
//			popFileName = config.plans().getInputFile();
//			worldFileName = config.world().getInputFile();
//		}
//
//		World world = Gbl.createWorld();
//
//		if (worldFileName != null) {
//			MatsimWorldReader world_parser = new MatsimWorldReader(Gbl.getWorld());
//			world_parser.readFile(worldFileName);
//		}
//
//		NetworkLayer net = new NetworkLayer();
//		new MatsimNetworkReader(net).readFile(netFileName);
//		world.setNetworkLayer(net);
//		world.complete();
//
//		Population population = new PopulationImpl();
//		// Read plans file with special Reader Implementation
//		PopulationReader plansReader = new MatsimPopulationReader(population);
//		plansReader.readFile(popFileName);
//
//		Events events = new Events();
//
//		config.simulation().setStartTime(Time.parseTime("00:00:00"));
//		config.simulation().setEndTime(Time.parseTime("24:00:00"));
//		config.network().setInputFile(netFileName);
//
//		config.simulation().setSnapshotFormat("none");
//		config.simulation().setSnapshotPeriod(600);
//		config.simulation().setSnapshotFile("./output/remove_thisB");
//
//
//		OnTheFlyQueueSimWriter sim = new OnTheFlyQueueSimWriter(net, population, events);
//
//
//		sim.run();
//
//		Gbl.printElapsedTime();
//
//
	}

//	public void setOtfwriter(final OTFQuadFileHandler.Writer  otfwriter) {
//		this.netStateWriter = otfwriter;
//	}

}
