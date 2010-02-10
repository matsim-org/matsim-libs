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

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.gui.OTFHostConnectionManager;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.layer.ColoredStaticNetLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.SimpleStaticNetLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;

/**
 * OTFDoubleMVI displays two movies in different areas of a split screen application.
 *
 * @author dstrippgen
 *
 */
public class OTFDoubleMVI extends OTFClientFile {
	protected String filename2;
	protected OTFDrawer leftComp = null;
	public OTFDoubleMVI(String filename, String filename2) {
		super(filename);
		this.filename2 = filename2;
	}

	protected OTFClientQuad getLeftDrawerComponent() throws RemoteException {
		OTFConnectionManager connectL = this.connect.clone();
		connectL.remove(OTFLinkAgentsHandler.class);

		connectL.add(OTFLinkAgentsHandler.class, ColoredStaticNetLayer.QuadDrawer.class);
		connectL.add(ColoredStaticNetLayer.QuadDrawer.class, ColoredStaticNetLayer.class);
		OTFClientQuad clientQ = createNewView(null, connectL, this.hostControlBar.getOTFHostControl());
		return clientQ;
	}

	@Override
	protected OTFDrawer createDrawer() {
		OTFDrawer superDrawer = super.createDrawer();
		OTFDrawer drawer;
		try {
			drawer = new OTFOGLDrawer(this.getLeftDrawerComponent());
			this.leftComp = drawer;

			drawer.invalidate((int)hostControlBar.getOTFHostControl().getTime());
			this.hostControlBar.addDrawer("test", drawer);
			pane.setLeftComponent(drawer.getComponent());
			OTFHostConnectionManager hostControl2;
			hostControl2 = new OTFHostConnectionManager("file:" + this.filename2);
			this.hostControlBar.addSlave(hostControl2);

			OTFConnectionManager connectR = this.connect.clone();
			connectR.remove(OTFLinkAgentsHandler.class);
			connectR.add(OTFLinkAgentsHandler.class,  SimpleStaticNetLayer.SimpleQuadDrawer.class);
			connectR.add(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);
			connectR.add(OTFLinkAgentsHandler.class,  AgentPointDrawer.class);
			connectR.add(OTFLinkLanesAgentsNoParkingHandler.class,  AgentPointDrawer.class);
			connectR.add(OGLAgentPointLayer.AgentPointDrawer.class, OGLAgentPointLayer.class);

			OTFClientQuad clientQ2 = createNewView(null, connectR, hostControl2);
			OTFOGLDrawer drawer2 = new OTFOGLDrawer(clientQ2);
			drawer2.invalidate((int)hostControlBar.getOTFHostControl().getTime());
			drawer2.replaceMouseHandler(((OTFOGLDrawer) this.mainDrawer).getMouseHandler());
			hostControlBar.addDrawer("test", drawer2);
			this.pane.setLeftComponent(this.createDrawerPanel(filename2, drawer2));
			pane.setResizeWeight(0.5);

		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
		return superDrawer;
	}

	public static void main( String[] args) {
		String filename = null;
		String filename2 = null;
		if (args.length == 2) {
			filename = args[0];
			filename2 = args[1];
		}

		OTFDoubleMVI client = new OTFDoubleMVI(filename, filename2);
		client.start();
	}

}

