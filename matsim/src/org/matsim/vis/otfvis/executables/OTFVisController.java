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
import org.matsim.vis.otfvis.gui.PreferencesDialog;
import org.matsim.vis.otfvis.opengl.OnTheFlyClientQuad;
import org.matsim.vis.otfvis.opengl.OnTheFlyQueueSimQuad;
import org.matsim.vis.otfvis.opengl.gui.PreferencesDialog2;
import org.matsim.vis.otfvis.server.OnTheFlyServer;

public class OTFVisController extends Controler {

	private OnTheFlyServer myOTFServer;
	private QueueNetwork queueNetwork;

	public static final int NOCONTROL = 0x00000000;
	
	public static final int STARTUP = 0x01000000;
	public static final int RUNNING = 0x02000000;
	public static final int REPLANNING = 0x04000000;
	public static final int ALL_FLAGS = 0xff000000;

	public static int getStatus(int flags) {
		return flags & ALL_FLAGS;
	}
	
	public static int getIteration(int flags) {
		int res = flags & 0xffffff;
		return res;
	}
	
//	@Override
//	protected void loadControlerListeners() {
//		super.loadControlerListeners();
//	}
//
//	@Override
//	protected void loadCoreListeners() {
//		super.loadCoreListeners();
//	}
//
	@Override
	protected void setUp() {
		super.setUp();
		UUID idOne = UUID.randomUUID();
		this.queueNetwork = new QueueNetwork(this.network);
		this.myOTFServer = OnTheFlyServer.createInstance("OTFServer_" + idOne.toString(), this.queueNetwork, this.population, getEvents(), false);
		myOTFServer.setControllerStatus(STARTUP);
		// FOR TESTING ONLY!
		PreferencesDialog.preDialogClass = PreferencesDialog2.class;
		OnTheFlyClientQuad client = new OnTheFlyClientQuad("rmi:127.0.0.1:4019:OTFServer_" + idOne.toString());
		client.start();
	}

	@Override
	protected void runMobSim() {
		OnTheFlyQueueSimQuad sim = new OnTheFlyQueueSimQuad(this.network, this.population, this.events);
		// overwrite network
		sim.setQueueNetwork(this.queueNetwork);
		sim.setServer(myOTFServer);
		
		sim.addQueueSimulationListeners(this.getQueueSimulationListener());
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


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String config = "../../tmp/studies/ivtch/Diss/config1p.xml";
		OTFVisController controller = new OTFVisController(config);
		controller.setOverwriteFiles(true);
		controller.run();
	}

}

