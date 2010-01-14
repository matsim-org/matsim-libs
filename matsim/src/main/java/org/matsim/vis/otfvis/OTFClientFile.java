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

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.ptproject.qsim.QueueLink;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.fileio.queuesim.OTFQueueSimLinkAgentsWriter;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultLinkHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultNodeHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsNoParkingHandler;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gui.OTFFileSettingsSaver;
import org.matsim.vis.otfvis.opengl.gui.OTFLiveSettingsSaver;
import org.matsim.vis.otfvis.opengl.gui.OTFTimeLine;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.SimpleStaticNetLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;

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
	
	private static final Logger log = Logger.getLogger(OTFClientFile.class);
	
	protected OTFConnectionManager connect = new OTFConnectionManager();

  private OTFFileSettingsSaver fileSettingsSaver;

	public OTFClientFile( String filename) {
		super("file:" + filename);
		/*
		 * If I got it right: The following entries to the connection manager are really needed to 
		 * get otfvis running with the current matsim version. The other entries added
		 * below are needed in terms of backward compatibility to older versions only. (dg, nov 09)
		 */
		this.connect.add(QueueLink.class, OTFLinkLanesAgentsNoParkingHandler.Writer.class);
		this.connect.add(OTFLinkLanesAgentsNoParkingHandler.Writer.class, OTFLinkLanesAgentsNoParkingHandler.class);
		this.connect.add(OTFLinkLanesAgentsNoParkingHandler.class, SimpleStaticNetLayer.SimpleQuadDrawer.class);
		this.connect.add(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);
		this.connect.add(OTFAgentsListHandler.Writer.class,  OTFAgentsListHandler.class);
		this.connect.add(OTFAgentsListHandler.class,  OGLAgentPointLayer.AgentPointDrawer.class);
		this.connect.add(AgentPointDrawer.class, OGLAgentPointLayer.class);
		/*
		 * Only needed for backward compatibility, see comment above (dg, nov 09)
		 */
		this.connect.add(OTFDefaultLinkHandler.Writer.class, OTFDefaultLinkHandler.class);
		this.connect.add(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
		this.connect.add(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
		this.connect.add(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
		
		/*
		 * This entry is needed to couple the org.matsim.core.queuesim to the visualizer
		 */
		this.connect.add(OTFQueueSimLinkAgentsWriter.class, OTFLinkLanesAgentsNoParkingHandler.class);
		
	}

	protected OTFClientQuad getRightDrawerComponent() throws RemoteException {
		OTFConnectionManager connectR = this.connect.clone();
		//those lines are from my point of view not really needed dg dez 09
		connectR.remove(OTFLinkAgentsHandler.class);
		connectR.add(OTFLinkAgentsHandler.class,  SimpleStaticNetLayer.SimpleQuadDrawer.class);
		connectR.add(OTFLinkLanesAgentsNoParkingHandler.class, SimpleStaticNetLayer.SimpleQuadDrawer.class);
		connectR.add(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);
		connectR.add(OTFLinkAgentsHandler.class,  AgentPointDrawer.class);
		connectR.add(OTFLinkLanesAgentsNoParkingHandler.class,  AgentPointDrawer.class);
		connectR.add(OGLAgentPointLayer.AgentPointDrawer.class, OGLAgentPointLayer.class);
		//end dg dez 09
		OTFClientQuad clientQ2 = createNewView(null, connectR, this.hostControlBar.getOTFHostControl());
		return clientQ2;
	}

	@Override
	protected OTFDrawer createDrawer(){
		try {
			frame.getContentPane().add(new OTFTimeLine("time", hostControlBar), BorderLayout.SOUTH);
			mainDrawer = 	new OTFOGLDrawer(frame, this.getRightDrawerComponent());

		}catch (RemoteException e) {
			e.printStackTrace();
			throw new RuntimeException();
		}
		hostControlBar.finishedInitialisition();
		return mainDrawer;
	}
	
	@Override
	protected OTFVisConfig createOTFVisConfig() {
	  boolean gblConfigGiven = false;
		if (Gbl.getConfig() != null) {
			visconf = Gbl.getConfig().otfVis();
			gblConfigGiven = true;
		}
		else {
			Gbl.createConfig(null);
			visconf = Gbl.getConfig().otfVis();
		}
		fileSettingsSaver = new OTFFileSettingsSaver(visconf, this.url);
		
		if (!gblConfigGiven) {
			visconf = fileSettingsSaver.openAndReadConfig();
		} 
		else {
			log.warn("OTFVisConfig already defined, cant read settings from file");
		}
		saver = new OTFLiveSettingsSaver(visconf, this.url);
		return visconf;
	}


}
