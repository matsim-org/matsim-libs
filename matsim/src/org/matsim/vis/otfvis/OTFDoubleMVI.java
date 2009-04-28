/* *********************************************************************** *
 * project: org.matsim.*
 * OTFDoubleMVI.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
import java.awt.Dimension;
import java.awt.Toolkit;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;

import org.matsim.core.gbl.Gbl;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.OTFSlaveHost;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.gui.PreferencesDialog;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.opengl.OnTheFlyClientFileQuad;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gui.OTFFileSettingsSaver;
import org.matsim.vis.otfvis.opengl.gui.OTFTimeLine;
import org.matsim.vis.otfvis.opengl.layer.ColoredStaticNetLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.SimpleStaticNetLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;

public class OTFDoubleMVI extends OnTheFlyClientFileQuad {
	protected String filename2;
	
	public OTFDoubleMVI(String filename, String filename2) {
		super(filename, false);
		this.filename2 = filename2;
	}


	@Override
	public void run() {
		JFrame frame = prepareRun();

		OTFSlaveHost hostControl2;
		try {
			hostControl2 = new OTFSlaveHost("file:" + this.filename2);
			hostControl2.frame = frame;
			this.hostControl.addSlave(hostControl2);

			OTFConnectionManager connectR = this.connect.clone();
			connectR.remove(OTFLinkAgentsHandler.class);
			connectR.add(OTFLinkAgentsHandler.class,  SimpleStaticNetLayer.SimpleQuadDrawer.class);
			connectR.add(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);
			connectR.add(OTFLinkAgentsHandler.class,  AgentPointDrawer.class);
			connectR.add(OTFLinkLanesAgentsNoParkingHandler.class,  AgentPointDrawer.class);
			connectR.add(OGLAgentPointLayer.AgentPointDrawer.class, OGLAgentPointLayer.class);

			OTFClientQuad clientQ2 = hostControl2.createNewView(null, connectR);
			OTFDrawer drawer2 = new OTFOGLDrawer(frame, clientQ2);
			drawer2.invalidate((int)hostControl.getTime());
			this.hostControl.addHandler("test", drawer2);
			this.pane.setLeftComponent(drawer2.getComponent());
			pane.setDividerLocation(0.5);
			//do not call for slave hosts!
			
			hostControl.finishedInitialisition();
			
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main( String[] args) {

		String filename;
		String filename2;
		
		if (args.length == 2) {
			filename = args[0];
			filename2 = args[1];
		} else {
//			filename = "../MatsimJ/output/OTFQuadfileNoParking10p_wip.mvi.gz";
			filename2 = "output/OTFQuadfile10p.mvi";
			filename = "testCUDA10p.mvi";
//			filename = "../../tmp/1000.events.mvi";
//			filename = "/TU Berlin/workspace/MatsimJ/output/OTFQuadfileNoParking10p_wip.mvi";
//			filename = "/TU Berlin/workspace/MatsimJ/otfvisSwitzerland10p.mvi";
//			filename = "testCUDA10p.mvi";
		}

		
		OTFDoubleMVI client = new OTFDoubleMVI(filename, filename2);
		client.run();
	}

}

