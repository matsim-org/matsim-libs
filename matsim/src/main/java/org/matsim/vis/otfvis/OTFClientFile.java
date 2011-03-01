/* *********************************************************************** *
 * project: org.matsim.*
 * OnTheFlyClientFileQuad.java
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

import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultLinkHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultNodeHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsNoParkingHandler;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gui.OTFTimeLine;
import org.matsim.vis.otfvis.opengl.gui.SettingsSaver;
import org.matsim.vis.otfvis.opengl.layer.AgentPointDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleQuadDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleStaticNetLayer;

/**
 * This file starts OTFVis using a .mvi file.
 *
 * This class is still a bit dirty as it is using tons of code to stay compatible
 * to older versions of OTFVis. dg dez 09
 *
 * @author dstrippgen
 * @author dgrether
 */
public class OTFClientFile extends OTFClient {

	protected OTFConnectionManager connect = new OTFConnectionManager();

	public OTFClientFile( String filename) {
		super("file:" + filename);

		this.connect.connectQLinkToWriter(OTFLinkLanesAgentsNoParkingHandler.Writer.class);
		this.connect.connectQueueLinkToWriter(OTFLinkAgentsHandler.Writer.class);
		
		this.connect.connectWriterToReader(OTFLinkLanesAgentsNoParkingHandler.Writer.class, OTFLinkLanesAgentsNoParkingHandler.class);
		this.connect.connectWriterToReader(OTFAgentsListHandler.Writer.class, OTFAgentsListHandler.class);
		this.connect.connectWriterToReader(OTFDefaultLinkHandler.Writer.class, OTFDefaultLinkHandler.class);
		this.connect.connectWriterToReader(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
		this.connect.connectWriterToReader(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
		this.connect.connectWriterToReader(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
		
		this.connect.connectReaderToReceiver(OTFLinkLanesAgentsNoParkingHandler.class, OGLSimpleQuadDrawer.class);
		this.connect.connectReaderToReceiver(OTFLinkLanesAgentsNoParkingHandler.class, AgentPointDrawer.class);
		this.connect.connectReaderToReceiver(OTFAgentsListHandler.class, AgentPointDrawer.class);
		this.connect.connectReaderToReceiver(OTFLinkAgentsHandler.class, OGLSimpleQuadDrawer.class);
		this.connect.connectReaderToReceiver(OTFLinkAgentsHandler.class, AgentPointDrawer.class);
		
		this.connect.connectReceiverToLayer(OGLSimpleQuadDrawer.class, OGLSimpleStaticNetLayer.class);		
		this.connect.connectReceiverToLayer(AgentPointDrawer.class, OGLAgentPointLayer.class);
	}

	@Override
	protected OTFDrawer createDrawer(){
		try {
			OTFTimeLine timeLine = new OTFTimeLine("time", hostControlBar.getOTFHostControl());
			frame.getContentPane().add(timeLine, BorderLayout.SOUTH);
			hostControlBar.addDrawer("timeline", timeLine);
			OTFClientQuad clientQ2 = createNewView(null, this.connect, this.hostControlBar.getOTFHostConnectionManager());
			OTFDrawer mainDrawer = new OTFOGLDrawer(clientQ2, hostControlBar);
			return mainDrawer;
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}

	@Override
	protected OTFVisConfigGroup createOTFVisConfig() {
		try {
			saver = new SettingsSaver(this.url);
			return this.masterHostControl.getOTFServer().getOTFVisConfig();
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
	}

}
