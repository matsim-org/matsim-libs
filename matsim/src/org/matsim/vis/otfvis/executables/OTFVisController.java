/* *********************************************************************** *
 * project: org.matsim.*
 * OTFVisController.java
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

package org.matsim.vis.otfvis.executables;

import java.util.UUID;

import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationListener;
import org.matsim.vis.otfvis.OTFClient;
import org.matsim.vis.otfvis.OTFVisQueueSim;
import org.matsim.vis.otfvis.data.DefaultConnectionManagerFactory;
import org.matsim.vis.otfvis.server.OnTheFlyServer;

/**
 * This class shows how the controller is overloaded to run a "live" simulation.
 * 
 * @author dstrippgen
 *
 */
public class OTFVisController extends Controler {

	private OnTheFlyServer myOTFServer;
	private QueueNetwork queueNetwork;

	public static final int NOCONTROL = 0x00000000;
	
	public static final int STARTUP = 0x01000000;
	public static final int RUNNING = 0x02000000;
	public static final int PAUSED = 0x80000000; //Flag for indicating paused mode to "other clients"	
	public static final int REPLANNING = 0x04000000;
	public static final int CANCEL = 0x08000000;
	public static final int ALL_FLAGS = 0xff000000;

	private boolean doVisualizeTeleportedAgents = false;
	
	public static int getStatus(int flags) {
		return flags & ALL_FLAGS;
	}
	
	public static int getIteration(int flags) {
		int res = flags & 0xffffff;
		return res;
	}
	
	@Override
	protected void setUp() {
		super.setUp();
		UUID idOne = UUID.randomUUID();
		this.queueNetwork = new QueueNetwork(this.network);
		this.myOTFServer = OnTheFlyServer.createInstance("OTFServer_" + idOne.toString(), this.queueNetwork, this.population, getEvents(), false);
		myOTFServer.setControllerStatus(STARTUP);
		OTFClient client = new OTFClient("rmi:127.0.0.1:4019:OTFServer_" + idOne.toString(), new DefaultConnectionManagerFactory().createConnectionManager());
		client.start();
	}

	@Override
	protected void runMobSim() {
		OTFVisQueueSim sim = new OTFVisQueueSim(this.scenarioData, this.events);
		// overwrite network
		sim.setQueueNetwork(this.queueNetwork);
		sim.setServer(myOTFServer);
		sim.setVisualizeTeleportedAgents(this.doVisualizeTeleportedAgents);
		for (QueueSimulationListener l : this.getQueueSimulationListener()) {
			sim.addQueueSimulationListeners(l);
		}
		myOTFServer.setControllerStatus(RUNNING + getIteration());
		sim.run();
		myOTFServer.setControllerStatus(REPLANNING + getIteration()+1);
	}

	@Override
	protected void shutdown(boolean unexpected) {
		super.shutdown(unexpected);
		this.myOTFServer.cleanup();
	}

	public OTFVisController(String[] args) {
		super(args);
	}

	public OTFVisController(String configFileName) {
		super(configFileName);
	}
}

