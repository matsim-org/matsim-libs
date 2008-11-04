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

package org.matsim.utils.vis.otfvis.opengl;

import java.awt.BorderLayout;
import java.awt.GraphicsDevice;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;

import org.matsim.gbl.Gbl;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.utils.vis.otfvis.data.OTFClientQuad;
import org.matsim.utils.vis.otfvis.data.OTFConnectionManager;
import org.matsim.utils.vis.otfvis.data.OTFDefaultNetWriterFactoryImpl;
import org.matsim.utils.vis.otfvis.data.OTFWriterFactory;
import org.matsim.utils.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.utils.vis.otfvis.gui.OTFQueryControlBar;
import org.matsim.utils.vis.otfvis.gui.OTFVisConfig;
import org.matsim.utils.vis.otfvis.gui.PreferencesDialog;
import org.matsim.utils.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.utils.vis.otfvis.handler.OTFDefaultLinkHandler;
import org.matsim.utils.vis.otfvis.handler.OTFDefaultNodeHandler;
import org.matsim.utils.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.utils.vis.otfvis.handler.OTFLinkAgentsNoParkingHandler;
import org.matsim.utils.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.utils.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.utils.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.utils.vis.otfvis.opengl.layer.SimpleStaticNetLayer;
import org.matsim.utils.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;

public class OnTheFlyClientQuad extends Thread {
	private final String url;
	private OTFConnectionManager connect = new OTFConnectionManager();
	private final boolean isMac;
	
	public OnTheFlyClientQuad(String url) {
		this.url = url;
		
		isMac = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
		if (isMac) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		}

		connect.add(OTFDefaultLinkHandler.Writer.class, OTFDefaultLinkHandler.class);
		connect.add(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
		connect.add(OTFLinkLanesAgentsNoParkingHandler.Writer.class, OTFLinkLanesAgentsNoParkingHandler.class);
		connect.add(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
		connect.add(QueueLink.class, OTFLinkLanesAgentsNoParkingHandler.Writer.class);
		connect.add(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
		connect.add(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
		connect.add(OTFLinkAgentsHandler.class, SimpleStaticNetLayer.SimpleQuadDrawer.class);
		connect.add(OTFLinkLanesAgentsNoParkingHandler.class, SimpleStaticNetLayer.SimpleQuadDrawer.class);
		connect.add(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);
		connect.add(OTFLinkAgentsHandler.class,  AgentPointDrawer.class);
		connect.add(OTFAgentsListHandler.Writer.class,  OTFAgentsListHandler.class);
		connect.add(AgentPointDrawer.class, OGLAgentPointLayer.class);
		connect.add(OTFAgentsListHandler.class,  AgentPointDrawer.class);
	}

	public OnTheFlyClientQuad(String filename2, OTFConnectionManager connect) {
		this(filename2);
		this.connect = connect;
	}
	
	@Override
	public void run() {
		String id1 = "test1";
		boolean fullscreen = false;

		OTFVisConfig visconf = new OTFVisConfig();
		if (Gbl.getConfig() == null) Gbl.createConfig(null);
		Gbl.getConfig().addModule(OTFVisConfig.GROUP_NAME, visconf);
		
		JFrame frame = new JFrame("MATSIM OTFVis");
		if (isMac) {
			frame.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
		}
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JFrame.setDefaultLookAndFeelDecorated(true);
		
		if (fullscreen) {
			GraphicsDevice gd = frame.getGraphicsConfiguration().getDevice();
			frame.setUndecorated(true);
			gd.setFullScreenWindow(frame);
		}

		JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		pane.setContinuousLayout(true);
		pane.setOneTouchExpandable(true);
		frame.add(pane);

		try {
			//Make sure menus appear above JOGL Layer
			JPopupMenu.setDefaultLightWeightPopupEnabled(false); 
			
			OTFHostControlBar hostControl = new OTFHostControlBar(url);
			hostControl.frame = frame;
			frame.getContentPane().add(hostControl, BorderLayout.NORTH);
			PreferencesDialog.buildMenu(frame, visconf, hostControl);

			OTFDefaultNetWriterFactoryImpl factory = new OTFDefaultNetWriterFactoryImpl();
			if (connect.getEntries(QueueLink.class).isEmpty())	factory.setLinkWriterFac(new OTFLinkLanesAgentsNoParkingHandler.Writer());
			else {
				Class linkhandler = connect.getEntries(QueueLink.class).iterator().next();
				factory.setLinkWriterFac((OTFWriterFactory<QueueLink>)linkhandler.newInstance());
			}
			OTFClientQuad clientQ = hostControl.createNewView(id1, factory, connect);

			OTFDrawer drawer = new OTFOGLDrawer(frame, clientQ);

			pane.setLeftComponent(drawer.getComponent());
//			pane.getContentPane().add(drawer.getComponent());
			pane.validate();
			if(hostControl.isLiveHost()) {
				OTFQueryControlBar queryControl = new OTFQueryControlBar("test", hostControl, visconf);
				frame.getContentPane().add(queryControl, BorderLayout.SOUTH);
				((OTFOGLDrawer)drawer).setQueryHandler(queryControl);
			}
			
			if (!fullscreen) {
				// only set custom frame size if not in fullscreen mode
				frame.setSize(1024, 600);
			}
			drawer.invalidate((int)hostControl.getTime());
			frame.setVisible(true);
			hostControl.addHandler(id1, drawer);

		} catch (RemoteException e) {
			// TODO DS Handle with grace!
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		OnTheFlyClientQuad client = new OnTheFlyClientQuad("rmi:127.0.0.1:4019");
		client.start();
	}

}
