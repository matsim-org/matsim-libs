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

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.TileFactory;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;
import org.jdesktop.swingx.mapviewer.wms.WMSService;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.vis.otfvis.handler.FacilityDrawer;
import org.matsim.vis.otfvis.caching.SimpleSceneLayer;
import org.matsim.vis.otfvis.data.OTFClientQuadTree;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.data.fileio.SettingsSaver;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.OTFQueryControl;
import org.matsim.vis.otfvis.gui.OTFQueryControlToolBar;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.interfaces.OTFServer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleQuadDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleStaticNetLayer;

import javax.swing.*;
import java.awt.*;

public class OTFClientLive {

	public static void run(final Config config, final OTFServer server) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				SettingsSaver saver = new SettingsSaver("otfsettings");
				OTFVisConfigGroup visconf = saver.tryToReadSettingsFile();
				if (visconf == null) {
					visconf = server.getOTFVisConfig();
				}

				OTFConnectionManager connectionManager = new OTFConnectionManager();
				connectionManager.connectLinkToWriter(OTFLinkAgentsHandler.Writer.class);
				connectionManager.connectWriterToReader(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
				connectionManager.connectReaderToReceiver(OTFLinkAgentsHandler.class, OGLSimpleQuadDrawer.class);
				connectionManager.connectReceiverToLayer(OGLSimpleQuadDrawer.class, OGLSimpleStaticNetLayer.class);
				connectionManager.connectWriterToReader(OTFAgentsListHandler.Writer.class, OTFAgentsListHandler.class);
				
				if (config.transit().isUseTransit()) {
					connectionManager.connectWriterToReader(FacilityDrawer.Writer.class, FacilityDrawer.Reader.class);
					connectionManager.connectReaderToReceiver(FacilityDrawer.Reader.class, FacilityDrawer.DataDrawer.class);
					connectionManager.connectReceiverToLayer(FacilityDrawer.DataDrawer.class, SimpleSceneLayer.class);
				}

				GLAutoDrawable canvas = OTFOGLDrawer.createGLCanvas(visconf);
				OTFClient otfClient = new OTFClient(canvas);
				otfClient.setServer(server);
				OTFClientControl.getInstance().setOTFVisConfig(visconf); // has to be set before OTFClientQuadTree.getConstData() is invoked!
				OTFServerQuadTree serverQuadTree = server.getQuad(connectionManager);
				OTFClientQuadTree clientQuadTree = serverQuadTree.convertToClient(server, connectionManager);
				clientQuadTree.getConstData();
				OTFHostControlBar hostControlBar = otfClient.getHostControlBar();

				OTFOGLDrawer mainDrawer = new OTFOGLDrawer(clientQuadTree, hostControlBar, visconf, canvas);
				OTFQueryControl queryControl = new OTFQueryControl(server, hostControlBar, visconf);
				OTFQueryControlToolBar queryControlBar = new OTFQueryControlToolBar(queryControl, visconf);
				queryControl.setQueryTextField(queryControlBar.getTextField());
				otfClient.getContentPane().add(queryControlBar, BorderLayout.SOUTH);
				mainDrawer.setQueryHandler(queryControl);
				otfClient.addDrawerAndInitialize(mainDrawer, saver);
				if (visconf.isMapOverlayMode()) {
					TileFactory tf;
					if (visconf.getMapBaseURL().isEmpty()) {
						assertZoomLevel17(config);
						tf = osmTileFactory();
					} else {
						WMSService wms = new WMSService(visconf.getMapBaseURL(), visconf.getMapLayer());
						tf = new OTFVisWMSTileFactory(wms, visconf.getMaximumZoom());
					}
					otfClient.addMapViewer(tf);
				}
                otfClient.pack();
				otfClient.setVisible(true);
			}
		});
	}
	
	private static void assertZoomLevel17(Config config) {
		if(ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).getMaximumZoom() != 17) {
			throw new RuntimeException("The OSM layer only works with maximumZoomLevel = 17. Please adjust your config.");
		}
	}
	
	private static TileFactory osmTileFactory() {
		final int max=17;
		TileFactoryInfo info = new TileFactoryInfo(0, 17, 17,
				256, true, true,
				"http://tile.openstreetmap.org",
				"x","y","z") {
			@Override
			public String getTileUrl(int x, int y, int zoom) {
				zoom = max-zoom;
				String url = this.baseURL +"/"+zoom+"/"+x+"/"+y+".png";
				return url;
			}

		};
		TileFactory tf = new DefaultTileFactory(info);
		return tf;
	}
	
	private static class OTFVisWMSTileFactory extends DefaultTileFactory {
		public OTFVisWMSTileFactory(final WMSService wms, final int maxZoom) {
			super(new TileFactoryInfo(0, maxZoom, maxZoom, 
					256, true, true, // tile size and x/y orientation is r2l & t2b
					"","x","y","zoom") {
				@Override
				public String getTileUrl(int x, int y, int zoom) {
					int zz = maxZoom - zoom;
					int z = (int)Math.pow(2,(double)zz-1);
					return wms.toWMSURL(x-z, z-1-y, zz, getTileSize(zoom));
				}

			});
		}
	}

}
