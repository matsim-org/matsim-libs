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

import org.matsim.core.gbl.Gbl;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.fileio.OTFFileReader;
import org.matsim.vis.otfvis.gui.OTFHostConnectionManager;
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
 * This class is still a bit dirty as it is using tons of code to stay compatible
 * to older versions of OTFVis. dg dez 09
 *
 * @author dstrippgen
 * @author dgrether
 */
public class OTFClientFile implements Runnable {

	private OTFClient otfClient = new OTFClient();
	
	private OTFConnectionManager connect = new OTFConnectionManager();

	private final String url;

	private OTFHostConnectionManager masterHostControl;
	
	public OTFClientFile(String filename) {
		super();
		this.url = filename;
		masterHostControl = new OTFHostConnectionManager(this.url, new OTFFileReader(filename));
		otfClient.setHostConnectionManager(masterHostControl);
		Gbl.printMemoryUsage();

		this.connect.connectWriterToReader(LinkHandler.Writer.class, LinkHandler.class);
		this.connect.connectWriterToReader(OTFAgentsListHandler.Writer.class, OTFAgentsListHandler.class);
		this.connect.connectReaderToReceiver(OTFAgentsListHandler.class, AgentPointDrawer.class);
		this.connect.connectReaderToReceiver(LinkHandler.class,  OGLSimpleQuadDrawer.class);
		this.connect.connectReceiverToLayer(OGLSimpleQuadDrawer.class, OGLSimpleStaticNetLayer.class);		
		this.connect.connectReceiverToLayer(AgentPointDrawer.class, OGLAgentPointLayer.class);
	}

	private OTFDrawer createDrawer(){
		OTFTimeLine timeLine = new OTFTimeLine("time", otfClient.getHostControlBar().getOTFHostControl());
		otfClient.getFrame().getContentPane().add(timeLine, BorderLayout.SOUTH);
		otfClient.getHostControlBar().addDrawer(timeLine);
		OTFClientQuad clientQ2 = otfClient.createNewView(this.connect);
		OTFDrawer mainDrawer = new OTFOGLDrawer(clientQ2, otfClient.getHostControlBar());
		return mainDrawer;
	}

	private OTFVisConfigGroup createOTFVisConfig() {
		return this.masterHostControl.getOTFServer().getOTFVisConfig();
	}

	@Override
	public final void run() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				OTFClientControl.getInstance().setOTFVisConfig(createOTFVisConfig());
				otfClient.addDrawerAndInitialize(createDrawer(), new SettingsSaver(url));
				otfClient.show();
			}
		});
	}

}
