/* *********************************************************************** *
 * project: org.matsim.*
 * OnTheFlyClientQuad.java
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

import org.matsim.core.config.Config;
import org.matsim.lanes.otfvis.drawer.OTFLaneSignalDrawer;
import org.matsim.lanes.otfvis.io.OTFLaneReader;
import org.matsim.lanes.otfvis.io.OTFLaneWriter;
import org.matsim.pt.otfvis.FacilityDrawer;
import org.matsim.signalsystems.otfvis.io.OTFSignalReader;
import org.matsim.signalsystems.otfvis.io.OTFSignalWriter;
import org.matsim.vis.otfvis.caching.SimpleSceneLayer;
import org.matsim.vis.otfvis.data.OTFClientQuadTree;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.OTFQueryControl;
import org.matsim.vis.otfvis.gui.OTFQueryControlToolBar;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gui.SettingsSaver;
import org.matsim.vis.otfvis.opengl.layer.AgentPointDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleQuadDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleStaticNetLayer;

public class OTFClientLive {

	public static void run(final Config config, final OnTheFlyServer server) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				OTFConnectionManager connectionManager = new OTFConnectionManager();
				connectionManager.connectLinkToWriter(OTFLinkAgentsHandler.Writer.class);
				connectionManager.connectWriterToReader(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
				connectionManager.connectReaderToReceiver(OTFLinkAgentsHandler.class, OGLSimpleQuadDrawer.class);
				connectionManager.connectReceiverToLayer(OGLSimpleQuadDrawer.class, OGLSimpleStaticNetLayer.class);
				connectionManager.connectReaderToReceiver(OTFLinkAgentsHandler.class, AgentPointDrawer.class);
				connectionManager.connectReceiverToLayer(AgentPointDrawer.class, OGLAgentPointLayer.class);
				connectionManager.connectWriterToReader(OTFAgentsListHandler.Writer.class, OTFAgentsListHandler.class);
				connectionManager.connectReaderToReceiver(OTFAgentsListHandler.class, AgentPointDrawer.class);
				
				if (config.scenario().isUseTransit()) {
					connectionManager.connectWriterToReader(FacilityDrawer.Writer.class, FacilityDrawer.Reader.class);
					connectionManager.connectReaderToReceiver(FacilityDrawer.Reader.class, FacilityDrawer.DataDrawer.class);
					connectionManager.connectReceiverToLayer(FacilityDrawer.DataDrawer.class, SimpleSceneLayer.class);
				}
				
				if (config.scenario().isUseLanes() && (!config.scenario().isUseSignalSystems())) {
					connectionManager.connectWriterToReader(OTFLaneWriter.class, OTFLaneReader.class);
					connectionManager.connectReaderToReceiver(OTFLaneReader.class, OTFLaneSignalDrawer.class);
					connectionManager.connectReceiverToLayer(OTFLaneSignalDrawer.class, SimpleSceneLayer.class);
				} else if (config.scenario().isUseSignalSystems()) {
					connectionManager.connectWriterToReader(OTFSignalWriter.class, OTFSignalReader.class);
					connectionManager.connectReaderToReceiver(OTFSignalReader.class, OTFLaneSignalDrawer.class);
					connectionManager.connectReceiverToLayer(OTFLaneSignalDrawer.class, SimpleSceneLayer.class);
				}
				OTFClient otfClient = new OTFClient();
				otfClient.setServer(server);
				SettingsSaver saver = new SettingsSaver("otfsettings");
				OTFVisConfigGroup visconf = saver.tryToReadSettingsFile();
				if (visconf == null) {
					visconf = server.getOTFVisConfig();
				}
				visconf.setCachingAllowed(false); // no use to cache in live mode
				OTFClientControl.getInstance().setOTFVisConfig(visconf);
				OTFServerQuadTree serverQuadTree = server.getQuad(connectionManager);
				OTFClientQuadTree clientQuadTree = serverQuadTree.convertToClient(server, connectionManager);
				clientQuadTree.createReceiver(connectionManager);
				clientQuadTree.getConstData();
				OTFHostControlBar hostControlBar = otfClient.getHostControlBar();
				hostControlBar.updateTimeLabel();
				OTFOGLDrawer mainDrawer = new OTFOGLDrawer(clientQuadTree, hostControlBar);
				OTFQueryControl queryControl = new OTFQueryControl(server, hostControlBar, visconf);
				OTFQueryControlToolBar queryControlBar = new OTFQueryControlToolBar(queryControl, visconf);
				queryControl.setQueryTextField(queryControlBar.getTextField());
				otfClient.getFrame().getContentPane().add(queryControlBar, BorderLayout.SOUTH);
				mainDrawer.setQueryHandler(queryControl);
				otfClient.addDrawerAndInitialize(mainDrawer, saver);
				otfClient.show();
			}
		});
	}

}
