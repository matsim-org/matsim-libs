/* *********************************************************************** *
 * project: org.matsim.*
 * OnTheFlyClientQuadSwing.java
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


import java.awt.BorderLayout;
import java.rmi.RemoteException;

import org.matsim.vis.otfvis.caching.SimpleSceneLayer;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.gui.OTFSwingDrawerContainer;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.gui.SwingAgentDrawer;
import org.matsim.vis.otfvis.gui.SwingSimpleQuadDrawer;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultLinkHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultNodeHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsNoParkingHandler;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.opengl.gui.OTFTimeLine;
import org.matsim.vis.otfvis.opengl.gui.SettingsSaver;



/**
 * This Client is capable of running on SWING only computers. It does not need OpenGL acceleration.
 * But it does not feature the whole set of operations possible with the OpenGL client.
 * It is also very slow, but for small networks it should work.
 *
 * @author dstrippgen
 * @author dgrether
 */
public class OTFClientSwing extends OTFClient {

	private OTFConnectionManager connectionManager = new OTFConnectionManager();

	/**
	 * @param url path to a file including a marker "file:" or "net:" at the very beginning.
	 */
	public OTFClientSwing(String url) {
		super(url);

		this.connectionManager.connectQLinkToWriter(OTFLinkLanesAgentsNoParkingHandler.Writer.class);
		this.connectionManager.connectQueueLinkToWriter(OTFLinkAgentsHandler.Writer.class);

		this.connectionManager.connectWriterToReader(OTFLinkLanesAgentsNoParkingHandler.Writer.class, OTFLinkLanesAgentsNoParkingHandler.class);
		this.connectionManager.connectWriterToReader(OTFAgentsListHandler.Writer.class, OTFAgentsListHandler.class);
		this.connectionManager.connectWriterToReader(OTFDefaultLinkHandler.Writer.class, OTFDefaultLinkHandler.class);
		this.connectionManager.connectWriterToReader(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
		this.connectionManager.connectWriterToReader(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
		this.connectionManager.connectWriterToReader(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
		
		this.connectionManager.connectReaderToReceiver(OTFLinkLanesAgentsNoParkingHandler.class, SwingSimpleQuadDrawer.class);
		this.connectionManager.connectReaderToReceiver(OTFLinkAgentsHandler.class, SwingSimpleQuadDrawer.class);
		this.connectionManager.connectReaderToReceiver(OTFLinkLanesAgentsNoParkingHandler.class, SwingAgentDrawer.class);
		this.connectionManager.connectReaderToReceiver(OTFLinkAgentsHandler.class, SwingAgentDrawer.class);
		this.connectionManager.connectReaderToReceiver(OTFAgentsListHandler.class, SwingAgentDrawer.class);
		
		this.connectionManager.connectReceiverToLayer(SwingSimpleQuadDrawer.class, SimpleSceneLayer.class);
		this.connectionManager.connectReceiverToLayer(SwingAgentDrawer.class, SimpleSceneLayer.class);
	}

	@Override
	protected OTFDrawer createDrawer() {
		try {
			if(!hostControlBar.getOTFHostConnectionManager().isLiveHost()) {
				OTFTimeLine timeLine = new OTFTimeLine("time", hostControlBar.getOTFHostControl());
				frame.getContentPane().add(timeLine, BorderLayout.SOUTH);
			} else  {
				throw new IllegalStateException("Server in live mode!");
			}
			OTFSwingDrawerContainer mainDrawer = new OTFSwingDrawerContainer(createNewView("swing", connectionManager, hostControlBar.getOTFHostConnectionManager()), hostControlBar);
			return mainDrawer;
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}

	@Override
	protected OTFVisConfigGroup createOTFVisConfig() {
	    saver = new SettingsSaver(this.url);
	    OTFVisConfigGroup visconf = new OTFVisConfigGroup();
	    return visconf;
	}

}
