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

import javax.swing.SwingUtilities;

import org.matsim.core.gbl.Gbl;
import org.matsim.vis.otfvis.caching.SimpleSceneLayer;
import org.matsim.vis.otfvis.data.OTFClientQuadTree;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.data.fileio.OTFFileReader;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.OTFSwingDrawerContainer;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.gui.SwingAgentDrawer;
import org.matsim.vis.otfvis.gui.SwingSimpleQuadDrawer;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.opengl.gui.OTFTimeLine;
import org.matsim.vis.otfvis.opengl.gui.SettingsSaver;
import org.matsim.vis.otfvis2.LinkHandler;



/**
 * This Client is capable of running on SWING only computers. It does not need OpenGL acceleration.
 * But it does not feature the whole set of operations possible with the OpenGL client.
 * It is also very slow, but for small networks it should work.
 *
 * @author dstrippgen
 * @author dgrether
 */
public class OTFClientSwing implements Runnable {
	
	private OTFClient otfClient = new OTFClient();

	private final String url;
	
	/**
	 * @param url path to a file including a marker "file:" or "net:" at the very beginning.
	 */
	public OTFClientSwing(String filename) {
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
		OTFConnectionManager connectionManager = new OTFConnectionManager();
		OTFFileReader server = new OTFFileReader(url);
		otfClient.setServer(server);
		Gbl.printMemoryUsage();
		connectionManager.connectWriterToReader(LinkHandler.Writer.class, LinkHandler.class);
		connectionManager.connectWriterToReader(OTFAgentsListHandler.Writer.class, OTFAgentsListHandler.class);
		connectionManager.connectReaderToReceiver(LinkHandler.class, SwingSimpleQuadDrawer.class);
		connectionManager.connectReaderToReceiver(OTFAgentsListHandler.class, SwingAgentDrawer.class);
		connectionManager.connectReceiverToLayer(SwingSimpleQuadDrawer.class, SimpleSceneLayer.class);
		connectionManager.connectReceiverToLayer(SwingAgentDrawer.class, SimpleSceneLayer.class);
		OTFVisConfigGroup otfVisConfig = new OTFVisConfigGroup();
		OTFClientControl.getInstance().setOTFVisConfig(otfVisConfig);
		OTFHostControlBar hostControlBar = otfClient.getHostControlBar();
		OTFTimeLine timeLine = new OTFTimeLine("time", hostControlBar.getOTFHostControl());
		otfClient.getFrame().getContentPane().add(timeLine, BorderLayout.SOUTH);
		OTFServerQuadTree serverQuadTree = server.getQuad(connectionManager);
		OTFClientQuadTree clientQuadTree = serverQuadTree.convertToClient(server, connectionManager);
		clientQuadTree.createReceiver(connectionManager);
		clientQuadTree.getConstData();
		hostControlBar.updateTimeLabel();
		OTFSwingDrawerContainer mainDrawer = new OTFSwingDrawerContainer(clientQuadTree, hostControlBar);
		otfClient.addDrawerAndInitialize(mainDrawer, new SettingsSaver(url));
		otfClient.show();
	}

}
