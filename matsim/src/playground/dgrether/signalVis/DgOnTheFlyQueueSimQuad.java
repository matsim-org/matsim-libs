/* *********************************************************************** *
 * project: org.matsim.*
 * DgOnTheFlyQueueSimQuad
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
package playground.dgrether.signalVis;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.vis.otfvis.OTFVisQueueSim;

/**
 * This is actually a more or less direct copy of OnTheFlyQueueSimQuad, because
 * the important command, i.e. new OnTheFlyClientQuad(..., MyConncetionManager)
 * is not accessible even in case of an overwritten prepareSim() that has to
 * call QueueSimulation.prepareSim() in any case;-).
 * 
 * @author dgrether
 * 
 */
public class DgOnTheFlyQueueSimQuad extends OTFVisQueueSim {

//	protected OnTheFlyServer myOTFServer = null;

	/**
	 * @param net
	 * @param plans
	 * @param events
	 * @param laneDefsvoid
	 */
	public DgOnTheFlyQueueSimQuad(ScenarioImpl scenario, EventsManagerImpl events) {
		super(scenario, events);
		this.setConnectionManager(new DgConnectionManagerFactory().createConnectionManager());
//		boolean isMac = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
//		if (isMac) {
//			System.setProperty("apple.laf.useScreenMenuBar", "true");
//		}
	}

//	@Override
//	protected void prepareSim() {
//		super.prepareSim();
//		UUID idOne = UUID.randomUUID();
//		this.myOTFServer = OnTheFlyServer.createInstance("OTFServer_" + idOne.toString(), this.network, this.plans,
//				getEvents(), false);
//
//		OTFConnectionManager connectionManager = setupConnectionManager();
//
//		// FOR TESTING ONLY!
//		PreferencesDialog.preDialogClass = PreferencesDialog2.class;
//		OnTheFlyClientQuad client = new OnTheFlyClientQuad("rmi:127.0.0.1:4019:OTFServer_" + idOne.toString(),
//				connectionManager);
//		client.start();
//
//		try {
//			this.myOTFServer.pause();
//		} catch (RemoteException e) {
//			e.printStackTrace();
//		}
//	}
//
//	private OTFConnectionManager setupConnectionManager() {
//		OTFConnectionManager connectionManager = new OTFConnectionManager();
//		boolean drawLanes = false;
//		boolean drawSignals = true;
//		
//		if (drawLanes){
//			// data source to writer
//			connectionManager.add(QueueLink.class, DgOtfLaneWriter.class);
//			// writer -> reader: from server to client
//			connectionManager
//			.add(DgOtfLaneWriter.class, DgOtfLaneReader.class);
//			// reader to drawer (or provider to receiver)
//			connectionManager.add(DgOtfLaneReader.class, DgLaneSignalDrawer.class);
//			// drawer -> layer
//			connectionManager.add(DgLaneSignalDrawer.class, DgOtfLaneLayer.class);
//		}
//		else if (drawSignals) {
//			// data source to writer
//			connectionManager.add(QueueLink.class, DgOtfSignalWriter.class);
//			// writer -> reader: from server to client
//			connectionManager
//			.add(DgOtfSignalWriter.class, DgSignalReader.class);
//			// reader to drawer (or provider to receiver)
//			connectionManager.add(DgSignalReader.class, DgLaneSignalDrawer.class);
//			// drawer -> layer
//			connectionManager.add(DgLaneSignalDrawer.class, DgOtfSignalLayer.class);
//		}
//
//		//agent code
//		// reader -> drawer
//		connectionManager.add(OTFLinkLanesAgentsNoParkingHandler.class, AgentPointDrawer.class);
//		//drawer -> layer
//		connectionManager.add(AgentPointDrawer.class, OGLAgentPointLayer.class);
//
//		
//		//default network code
//  	// data source to writer
//		connectionManager.add(QueueLink.class, OTFLinkLanesAgentsNoParkingHandler.Writer.class);
//		//writer -> reader
//		connectionManager
//		.add(OTFLinkLanesAgentsNoParkingHandler.Writer.class, OTFLinkLanesAgentsNoParkingHandler.class);
//		//reader -> drawer
//		connectionManager.add(OTFLinkLanesAgentsNoParkingHandler.class, SimpleStaticNetLayer.SimpleQuadDrawer.class);
//		//drawer -> layer
//		connectionManager.add(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);
//		
//		
//		
//
//		
//		
//		// //writer -> reader
//		// this.add(OTFDefaultLinkHandler.Writer.class,
//		// OTFDefaultLinkHandler.class);
//		// this.add(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
//		// this.add(OTFLinkAgentsNoParkingHandler.Writer.class,
//		// OTFLinkAgentsHandler.class);
//		//	
//		// this.add(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
//		// this.add(OTFDefaultNodeHandler.Writer.class,
//		// OTFDefaultNodeHandler.class);
//		//
//		// //reader -> drawer
//		// this.add(OTFLinkAgentsHandler.class,
//		// SimpleStaticNetLayer.SimpleQuadDrawer.class);
//		//	
//		//	
//		// this.add(OTFLinkAgentsHandler.class, AgentPointDrawer.class);
//		// this.add(OTFAgentsListHandler.Writer.class, OTFAgentsListHandler.class);
//		return connectionManager;
//	}
//
//	@Override
//	protected void cleanupSim() {
//		this.myOTFServer.cleanup();
//		this.myOTFServer = null;
//		super.cleanupSim();
//	}
//
//	@Override
//	protected void afterSimStep(final double time) {
//		super.afterSimStep(time);
//		this.myOTFServer.updateStatus(time);
//	}

}
