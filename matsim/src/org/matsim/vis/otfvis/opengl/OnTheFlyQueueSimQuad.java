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

package org.matsim.vis.otfvis.opengl;

import java.rmi.RemoteException;
import java.util.UUID;

import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.events.Events;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.otfvis.gui.PreferencesDialog;
import org.matsim.vis.otfvis.opengl.gui.PreferencesDialog2;
import org.matsim.vis.otfvis.server.OnTheFlyServer;


/**
 * @author DS
 *
 */
public class OnTheFlyQueueSimQuad extends QueueSimulation{
	private OnTheFlyServer myOTFServer = null;

	@Override
	protected void prepareSim() {
		UUID idOne = UUID.randomUUID();

		this.myOTFServer = OnTheFlyServer.createInstance("OTFServer_" + idOne.toString(), this.network, this.plans, events, false);

		super.prepareSim();

		// FOR TESTING ONLY!
		PreferencesDialog.preDialogClass = PreferencesDialog2.class;
		OnTheFlyClientQuad client = new OnTheFlyClientQuad("rmi:127.0.0.1:4019:OTFServer_" + idOne.toString());
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
	protected void afterSimStep(final double time) {
		super.afterSimStep(time);
		this.myOTFServer.updateStatus(time);

	}

	public OnTheFlyQueueSimQuad(final NetworkLayer net, final Population plans, final Events events) {
		super(net, plans, events);

		boolean isMac = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
		if (isMac) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		}
	}

	public static void main(final String[] args) {

		String studiesRoot = "../";
		String localDtdBase = "../matsimJ/dtd/";

		// FIXME hard-coded filenames
		String netFileName = studiesRoot + "berlin-wip/network/wip_net.xml";
		String popFileName = studiesRoot + "berlin-wip/synpop-2006-04/kutter_population/kutter001car_hwh.routes_wip.plans.xml.gz"; // 15931 agents
//		String popFileName = studiesRoot + "berlin-wip/synpop-2006-04/kutter_population/kutter010car_hwh.routes_wip.plans.xml.gz"; // 160171 agents
//		String popFileName = studiesRoot + "berlin-wip/synpop-2006-04/kutter_population/kutter010car.routes_wip.plans.xml.gz";  // 299394 agents
//		String worldFileName = studiesRoot + "berlin-wip/synpop-2006-04/world_TVZ.xml";

		Config config = Gbl.createConfig(args);

		config.global().setLocalDtdBase(localDtdBase);

		config.simulation().setStartTime(Time.parseTime("09:00:00"));
		config.simulation().setEndTime(Time.parseTime("19:10:00"));

		config.simulation().setFlowCapFactor(0.1);
		config.simulation().setStorageCapFactor(0.1);


		if(args.length >= 1) {
			netFileName = config.network().getInputFile();
			popFileName = config.plans().getInputFile();
//			worldFileName = config.world().getInputFile();
		}

		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(netFileName);

		Population population = new PopulationImpl();
		MatsimPopulationReader plansReader = new MatsimPopulationReader(population, net);
		plansReader.readFile(popFileName);
		population.printPlansCount();

		Events events = new Events();

		OnTheFlyQueueSimQuad sim = new OnTheFlyQueueSimQuad(net, population, events);

		config.simulation().setSnapshotFormat("none");// or just set the snapshotPeriod to zero ;-)

		sim.run();

	}


}
