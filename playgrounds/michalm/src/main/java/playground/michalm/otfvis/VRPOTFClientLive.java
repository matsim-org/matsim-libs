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

package playground.michalm.otfvis;

import java.awt.*;

import javax.swing.*;

import org.matsim.core.config.*;
import org.matsim.lanes.otfvis.drawer.*;
import org.matsim.lanes.otfvis.io.*;
import org.matsim.pt.otfvis.*;
import org.matsim.signalsystems.otfvis.io.*;
import org.matsim.vis.otfvis.*;
import org.matsim.vis.otfvis.caching.*;
import org.matsim.vis.otfvis.data.*;
import org.matsim.vis.otfvis.data.fileio.*;
import org.matsim.vis.otfvis.gui.*;
import org.matsim.vis.otfvis.handler.*;
import org.matsim.vis.otfvis.interfaces.*;
import org.matsim.vis.otfvis.opengl.drawer.*;
import org.matsim.vis.otfvis.opengl.layer.*;

public class VRPOTFClientLive {

    public static OTFQueryControl queryControl;
    
	public static void run(final Config config, final OTFServer server) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				OTFConnectionManager connectionManager = new OTFConnectionManager();
				connectionManager.connectLinkToWriter(OTFLinkAgentsHandler.Writer.class);
				connectionManager.connectWriterToReader(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
				connectionManager.connectReaderToReceiver(OTFLinkAgentsHandler.class, OGLSimpleQuadDrawer.class);
				connectionManager.connectReceiverToLayer(OGLSimpleQuadDrawer.class, OGLSimpleStaticNetLayer.class);
				connectionManager.connectReaderToReceiver(OTFLinkAgentsHandler.class, VRPAgentPointDrawer.class);
				connectionManager.connectReceiverToLayer(VRPAgentPointDrawer.class, OGLAgentPointLayer.class);
				connectionManager.connectWriterToReader(OTFAgentsListHandler.Writer.class, OTFAgentsListHandler.class);
				connectionManager.connectReaderToReceiver(OTFAgentsListHandler.class, VRPAgentPointDrawer.class);
				
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
				OTFClientControl.getInstance().setOTFVisConfig(visconf);
				OTFServerQuadTree serverQuadTree = server.getQuad(connectionManager);
				OTFClientQuadTree clientQuadTree = serverQuadTree.convertToClient(server, connectionManager);
				clientQuadTree.setConnectionManager(connectionManager);
				clientQuadTree.getConstData();
				OTFHostControlBar hostControlBar = otfClient.getHostControlBar();
				OTFOGLDrawer mainDrawer = new OTFOGLDrawer(clientQuadTree, hostControlBar, config.otfVis());
				queryControl = new OTFQueryControl(server, hostControlBar, visconf);
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
