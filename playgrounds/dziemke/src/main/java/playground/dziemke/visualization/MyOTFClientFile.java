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

package playground.dziemke.visualization;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.SwingUtilities;

import org.matsim.vis.otfvis.gui.OTFVisFrame;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.data.OTFClientQuadTree;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.data.fileio.OTFFileReader;
import org.matsim.vis.otfvis.data.fileio.SettingsSaver;
import org.matsim.vis.otfvis.gui.OTFControlBar;
import org.matsim.vis.otfvis.gui.OTFHostControl;
import org.matsim.vis.otfvis.gui.OTFTimeLine;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;

/**
 * This file starts OTFVis using a .mvi file.
 * 
 * @author dstrippgen
 * @author dgrether
 */
public class MyOTFClientFile implements Runnable {

	private final String url;
	
	public MyOTFClientFile(String filename) {
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
		Component canvas = OTFOGLDrawer.createGLCanvas(new OTFVisConfigGroup());
		OTFFileReader otfServer = new OTFFileReader(url);
		final OTFHostControl hostControl = new OTFHostControl(otfServer, canvas);
		OTFVisConfigGroup otfVisConfig = otfServer.getOTFVisConfig();
		// new
		otfVisConfig.setRenderImages(true);
		// end new
//		otfVisConfig.setRenderImages(true);
//		otfVisConfig.addParam("agentSize", "60.f");
		OTFConnectionManager connect = new OTFConnectionManager();
		connect.connectWriterToReader(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
		connect.connectWriterToReader(OTFAgentsListHandler.Writer.class, OTFAgentsListHandler.class);
		OTFServerQuadTree servQ = otfServer.getQuad(connect);
		OTFClientQuadTree clientQ = servQ.convertToClient(otfServer, connect);
		// Change pane stat opens
//		clientQ.setMinEasting(110000);
//		clientQ.setMaxEasting(0);
//		clientQ.setMinNorthing(116000);
//		clientQ.setMaxNorthing(140000);
		OTFOGLDrawer mainDrawer = new OTFOGLDrawer(clientQ, otfVisConfig, canvas, hostControl);
//		mainDrawer.setIncludeLogo(false);
//		mainDrawer.setScreenshotInterval(3600);
//		mainDrawer.setTimeOfLastScreenshot(86400);
		OTFControlBar hostControlBar = new OTFControlBar(otfServer, hostControl, mainDrawer);
		OTFVisFrame otfVisFrame = new OTFVisFrame(canvas, otfServer, hostControlBar, mainDrawer, new SettingsSaver(url));
		OTFTimeLine timeLine = new OTFTimeLine("time", hostControl);
		otfVisFrame.getContentPane().add(timeLine, BorderLayout.SOUTH);
//		otfClient.setSize(800, 800); // does not seem to take effect
//		otfClient.show();
		otfVisFrame.pack();
        otfVisFrame.setVisible(true);
	}
}