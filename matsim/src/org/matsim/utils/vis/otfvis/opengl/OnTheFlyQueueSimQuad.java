/* *********************************************************************** *
 * project: org.matsim.*
 * OnTheFlyQueueSimQuad.java
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

package org.matsim.utils.vis.otfvis.opengl;

import java.rmi.RemoteException;

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueSimulation;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.otfvis.gui.PreferencesDialog;
import org.matsim.utils.vis.otfvis.opengl.gui.PreferencesDialog2;
import org.matsim.utils.vis.otfvis.server.OnTheFlyServer;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;


/**
 * @author DS
 *
 */
public class OnTheFlyQueueSimQuad extends QueueSimulation{
	private OnTheFlyServer myOTFServer = null;

	@Override
	protected void prepareSim() {
		this.myOTFServer = OnTheFlyServer.createInstance("AName1", this.network, this.plans, events, false);

		super.prepareSim();

		// FOR TESTING ONLY!
		PreferencesDialog.preDialogClass = PreferencesDialog2.class;
		OnTheFlyClientQuad client = new OnTheFlyClientQuad("rmi:127.0.0.1:4019");
		client.start();
		try {
			this.myOTFServer.pause();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void cleanupSim() {
//		if (myOTFServer != null) myOTFServer.stop();
		this.myOTFServer.cleanup();
		this.myOTFServer = null;
		super.cleanupSim();
	}

	@Override
	public void afterSimStep(double time) {
		super.afterSimStep(time);
		this.myOTFServer.updateStatus(time);

	}

	public OnTheFlyQueueSimQuad(NetworkLayer net, Plans plans, Events events) {
		super(net, plans, events);

		boolean isMac = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
		if (isMac) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		}
	}

	public static void main(String[] args) {

		String studiesRoot = "../";
		String localDtdBase = "../matsimJ/dtd/";

		// FIXME hard-coded filenames
		String netFileName = studiesRoot + "berlin-wip/network/wip_net.xml";
		String popFileName = studiesRoot + "berlin-wip/synpop-2006-04/kutter_population/kutter001car_hwh.routes_wip.plans.xml.gz"; // 15931 agents
//		String popFileName = studiesRoot + "berlin-wip/synpop-2006-04/kutter_population/kutter010car_hwh.routes_wip.plans.xml.gz"; // 160171 agents
//		String popFileName = studiesRoot + "berlin-wip/synpop-2006-04/kutter_population/kutter010car.routes_wip.plans.xml.gz";  // 299394 agents
		String worldFileName = studiesRoot + "berlin-wip/synpop-2006-04/world_TVZ.xml";

		Config config = Gbl.createConfig(args);

		config.global().setLocalDtdBase(localDtdBase);

		config.simulation().setStartTime(Time.parseTime("09:00:00"));
		config.simulation().setEndTime(Time.parseTime("19:10:00"));

		config.simulation().setFlowCapFactor(0.1);
		config.simulation().setStorageCapFactor(0.1);


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
		MatsimPlansReader plansReader = new MatsimPlansReader(population);
		plansReader.readFile(popFileName);
		population.printPlansCount();

		Events events = new Events();

		OnTheFlyQueueSimQuad sim = new OnTheFlyQueueSimQuad(net, population, events);

		config.simulation().setSnapshotFormat("none");// or just set the snapshotPeriod to zero ;-)


		sim.run();

	}


}
