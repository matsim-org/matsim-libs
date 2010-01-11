/* *********************************************************************** *
 * project: org.matsim.*
 * OTFVisControlerListener
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.vis.otfvis;

import java.util.UUID;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.ptproject.qsim.QueueNetwork;
import org.matsim.vis.otfvis.data.DefaultConnectionManagerFactory;
import org.matsim.vis.otfvis.server.OnTheFlyServer;


/**
 * @author dgrether
 *
 */
public class OTFVisControlerListener implements StartupListener, ShutdownListener, BeforeMobsimListener, AfterMobsimListener{

	public static final int NOCONTROL = 0x00000000;
	
	public static final int STARTUP = 0x01000000;
	public static final int RUNNING = 0x02000000;
	public static final int PAUSED = 0x80000000; //Flag for indicating paused mode to "other clients"	
	public static final int REPLANNING = 0x04000000;
	public static final int CANCEL = 0x08000000;
	public static final int ALL_FLAGS = 0xff000000;

	
	private QueueNetwork queueNetwork;
	private OnTheFlyServer otfserver;

	public void notifyStartup(StartupEvent e) {
		UUID idOne = UUID.randomUUID();
		Scenario sc = e.getControler().getScenario();
		this.queueNetwork = new QueueNetwork(sc.getNetwork());
		this.otfserver = OnTheFlyServer.createInstance("OTFServer_" + idOne.toString(), this.queueNetwork, sc.getPopulation(), e.getControler().getEvents(), false);
		otfserver.setControllerStatus(STARTUP);
		OTFClientLive client = new OTFClientLive("rmi:127.0.0.1:4019:OTFServer_" + idOne.toString(), new DefaultConnectionManagerFactory().createConnectionManager());
		client.setConfig(sc.getConfig().otfVis());
		client.start();
	}

	public void notifyBeforeMobsim(BeforeMobsimEvent e) {
		Scenario sc = e.getControler().getScenario();
		OTFVisQueueSim sim = new OTFVisQueueSim(sc, e.getControler().getEvents());
		// overwrite network
		sim.setQueueNetwork(this.queueNetwork);
		sim.setServer(otfserver);
		sim.setVisualizeTeleportedAgents(sc.getConfig().otfVis().isShowTeleportedAgents());
		for (SimulationListener l : e.getControler().getQueueSimulationListener()) {
			sim.addQueueSimulationListeners(l);
		}
		otfserver.setControllerStatus(RUNNING + e.getControler().getIteration());
		sim.run();
	}

	public void notifyAfterMobsim(AfterMobsimEvent e) {
		otfserver.setControllerStatus(REPLANNING + e.getControler().getIteration()+1);		
	}

	public void notifyShutdown(ShutdownEvent event) {
		this.otfserver.cleanup();		
	}

	public static int getStatus(int flags) {
		return flags & ALL_FLAGS;
	}
	
	public static int getIteration(int flags) {
		int res = flags & 0xffffff;
		return res;
	}

}
