/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.andreas.mzilske.osm;

import java.awt.BorderLayout;

import javax.swing.SwingUtilities;

import org.jdesktop.swingx.mapviewer.TileFactory;
import org.jdesktop.swingx.mapviewer.wms.WMSService;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.vis.otfvis.OTFClient;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.data.OTFClientQuadTree;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.data.fileio.SettingsSaver;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.OTFTimeLine;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.interfaces.OTFServer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleQuadDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleStaticNetLayer;

public final class JXMapOTFVisClient {


	public static void run(final Config config, final OTFServer server) {
		OTFClientLive.run(config, server);
	}

	

	public static void run(final Config config, final OTFServer server, final WMSService wms) {
		final TileFactory tf = new MyWMSTileFactory(wms, ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).getMaximumZoom());
		run(config, server, tf);
	}

	private static void run(final Config config, final OTFServer server, final TileFactory tf) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				OTFClient otfClient = new OTFClient();
				otfClient.setServer(server);
				OTFConnectionManager connect = new OTFConnectionManager();
				OTFVisConfigGroup otfVisConfig = server.getOTFVisConfig();
				otfVisConfig.setMapOverlayMode(true);
				OTFClientControl.getInstance().setOTFVisConfig(otfVisConfig);
				connect.connectLinkToWriter(OTFLinkAgentsHandler.Writer.class);
				connect.connectWriterToReader(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
				connect.connectReaderToReceiver(OTFLinkAgentsHandler.class, OGLSimpleQuadDrawer.class);
				connect.connectReceiverToLayer(OGLSimpleQuadDrawer.class, OGLSimpleStaticNetLayer.class);		

				OTFHostControlBar hostControlBar = otfClient.getHostControlBar();
				OTFTimeLine timeLine = new OTFTimeLine("time", hostControlBar.getOTFHostControl());
				otfClient.getContentPane().add(timeLine, BorderLayout.SOUTH);
				OTFServerQuadTree servQ = server.getQuad(connect);
				OTFClientQuadTree clientQ = servQ.convertToClient(server, connect);
				clientQ.getConstData();

				final OTFOGLDrawer mainDrawer = new OTFOGLDrawer(clientQ, hostControlBar, ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class));
				otfClient.addDrawerAndInitialize(mainDrawer, new SettingsSaver("settings"));
				
				otfClient.show();
			}
		});
	}

}
