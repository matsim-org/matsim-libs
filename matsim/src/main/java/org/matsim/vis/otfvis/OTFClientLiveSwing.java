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

import javax.swing.SwingUtilities;

import org.matsim.core.config.Config;
import org.matsim.vis.otfvis.caching.SimpleSceneLayer;
import org.matsim.vis.otfvis.data.OTFClientQuadTree;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.data.fileio.SettingsSaver;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.OTFSwingDrawerContainer;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.gui.SwingAgentDrawer;
import org.matsim.vis.otfvis.gui.SwingSimpleQuadDrawer;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.interfaces.OTFServer;

public class OTFClientLiveSwing {

	public static void run(final Config config, final OTFServer server) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				OTFConnectionManager connectionManager = new OTFConnectionManager();
				connectionManager.connectLinkToWriter(OTFLinkAgentsHandler.Writer.class);
				connectionManager.connectWriterToReader(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
				connectionManager.connectReaderToReceiver(OTFLinkAgentsHandler.class, SwingSimpleQuadDrawer.class);
				connectionManager.connectReceiverToLayer(SwingSimpleQuadDrawer.class, SimpleSceneLayer.class);
				connectionManager.connectReaderToReceiver(OTFLinkAgentsHandler.class, SwingAgentDrawer.class);
				connectionManager.connectReceiverToLayer(SwingAgentDrawer.class, SimpleSceneLayer.class);
				if (config.scenario().isUseTransit()) {
					throw new RuntimeException("Transit not supported in Swing mode.");
				}
				if (config.scenario().isUseLanes() && (!config.scenario().isUseSignalSystems())) {
					throw new RuntimeException("Lanes not supported in Swing mode.");
				} else if (config.scenario().isUseSignalSystems()) {
					throw new RuntimeException("Signals not supported in Swing mode.");
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
				OTFSwingDrawerContainer mainDrawer = new OTFSwingDrawerContainer(clientQuadTree, hostControlBar);
				otfClient.addDrawerAndInitialize(mainDrawer, saver);
				otfClient.show();
			}
		});
	}

}
