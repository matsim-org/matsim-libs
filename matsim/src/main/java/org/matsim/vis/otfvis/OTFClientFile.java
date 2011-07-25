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

import javax.swing.SwingUtilities;

import org.matsim.vis.otfvis.data.OTFClientQuadTree;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.data.fileio.OTFFileReader;
import org.matsim.vis.otfvis.gui.OTFHostControl;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gui.OTFTimeLine;
import org.matsim.vis.otfvis.opengl.gui.SettingsSaver;
import org.matsim.vis.otfvis.opengl.layer.AgentPointDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleQuadDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleStaticNetLayer;
import org.matsim.vis.otfvis2.LinkHandler;

/**
 * This file starts OTFVis using a .mvi file.
 * 
 * @author dstrippgen
 * @author dgrether
 */
public class OTFClientFile implements Runnable {

	private final String url;
	
	public OTFClientFile(String filename) {
		super();
		this.url = filename;
		
	}

	@Override
	public final void run() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				createDrawer();
			}
		});
	}

	private void createDrawer() {
		OTFClient otfClient = new OTFClient();
		OTFFileReader otfServer = new OTFFileReader(url);
		otfClient.setServer(otfServer);
		OTFConnectionManager connect = new OTFConnectionManager();
		connect.connectWriterToReader(LinkHandler.Writer.class, LinkHandler.class);
		connect.connectWriterToReader(OTFAgentsListHandler.Writer.class, OTFAgentsListHandler.class);
		connect.connectReaderToReceiver(OTFAgentsListHandler.class, AgentPointDrawer.class);
		connect.connectReaderToReceiver(LinkHandler.class, OGLSimpleQuadDrawer.class);
		connect.connectReceiverToLayer(OGLSimpleQuadDrawer.class, OGLSimpleStaticNetLayer.class);		
		connect.connectReceiverToLayer(AgentPointDrawer.class, OGLAgentPointLayer.class);
		OTFVisConfigGroup createOTFVisConfig = otfServer.getOTFVisConfig();
		OTFClientControl.getInstance().setOTFVisConfig(createOTFVisConfig);
		OTFHostControlBar hostControlBar = otfClient.getHostControlBar();
		OTFHostControl otfHostControl = hostControlBar.getOTFHostControl();
		OTFTimeLine timeLine = new OTFTimeLine("time", otfHostControl);
		otfClient.getFrame().getContentPane().add(timeLine, BorderLayout.SOUTH);
		hostControlBar.addDrawer(timeLine);
		OTFServerQuadTree servQ = otfServer.getQuad(connect);
		OTFClientQuadTree clientQ = servQ.convertToClient(otfServer, connect);
		clientQ.createReceiver(connect);
		clientQ.getConstData();
		hostControlBar.updateTimeLabel();
		OTFClientQuadTree clientQuadTree = clientQ;
		OTFDrawer mainDrawer = new OTFOGLDrawer(clientQuadTree, hostControlBar);
		otfClient.addDrawerAndInitialize(mainDrawer, new SettingsSaver(url));
		otfClient.show();
	}

}
