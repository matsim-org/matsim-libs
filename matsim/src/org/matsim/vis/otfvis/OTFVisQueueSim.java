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

package org.matsim.vis.otfvis;

import java.rmi.RemoteException;
import java.util.UUID;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.mobsim.queuesim.QueueSimEngine;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.server.OnTheFlyServer;


/**
 * This class starts OTFVis in live mode, i.e. with a running QueueSimulation.
 * @author DS
 */
public class OTFVisQueueSim extends QueueSimulation{
	protected OnTheFlyServer myOTFServer = null;
	private boolean ownServer = true;
	private boolean doVisualizeTeleportedVehicles = false;
	private OTFConnectionManager connectionManager = null;
	
	public void setServer(OnTheFlyServer server) {
		this.myOTFServer = server;
		ownServer = false;
	}
	@Override
	protected void prepareSim() {
		super.prepareSim();

		if(ownServer) {
			UUID idOne = UUID.randomUUID();
			this.myOTFServer = OnTheFlyServer.createInstance("OTFServer_" + idOne.toString(), this.network, this.population, getEvents(), false);

			OTFClient client = null;
			if (connectionManager == null) {
				client = new OTFClient("rmi:127.0.0.1:4019:OTFServer_" + idOne.toString());
			}
			else {
				client = new OTFClient("rmi:127.0.0.1:4019:OTFServer_" + idOne.toString(), this.connectionManager);
			}
			client.start();

			try {
				this.myOTFServer.pause();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void cleanupSim() {

		if(ownServer) {
			this.myOTFServer.cleanup();
		}

		this.myOTFServer = null;
		super.cleanupSim();
	}

	@Override
	protected void afterSimStep(final double time) {
		super.afterSimStep(time);
		this.myOTFServer.updateStatus(time);
		if (doVisualizeTeleportedVehicles) {
			this.visualizeTeleportedAgents();
		}
	}

	private void visualizeTeleportedAgents() {
		while (this.teleportationList.peek() != null ) {
		  
		}
	}
	public OTFVisQueueSim(final Scenario scenario, final EventsManager events) {
		super(scenario, events);
		
		boolean isMac = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
		if (isMac) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		}
	}

	public void setQueueNetwork(QueueNetwork net) {
		this.network = net;
		this.simEngine = new QueueSimEngine(this.network, MatsimRandom.getRandom());
	}
	
	public OTFConnectionManager getConnectionManager() {
		return this.connectionManager;
	}
	
	public void setConnectionManager(OTFConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}
	public void activateVisualizeTeleportedVehicles() {
		this.doVisualizeTeleportedVehicles = true;
//TODO dg		this.myOTFServer add stuff && add trac entry
	}

}
