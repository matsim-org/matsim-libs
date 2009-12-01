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

package org.matsim.vis.otfvis.opengl;

import java.awt.BorderLayout;
import java.awt.GraphicsDevice;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.swing.JSplitPane;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.gui.OTFFrame;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.OTFQueryControlBar;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.gui.PreferencesDialog;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultLinkHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultNodeHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsNoParkingHandler;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gui.OTFFileSettingsSaver;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.SimpleStaticNetLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;

public class OTFClient extends Thread {
	private static final Logger log = Logger.getLogger(OTFClient.class);
	
	private final String url;
	private OTFConnectionManager connect = new OTFConnectionManager();
	private final boolean isMac;
	protected String id1 = "test1";
	public static OTFHostControlBar hostControl2;
	
	public OTFClient(String url) {
		this.url = url;
		
		isMac = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
		if (isMac) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		}
		/*
		 * If I got it right: The next four entries to the connection manager are really needed to 
		 * get otfvis running with the current matsim version. The other entries added
		 * below are needed in terms of backward compatibility to older versions only. (dg, nov 09)
		 */
		connect.add(QueueLink.class, OTFLinkLanesAgentsNoParkingHandler.Writer.class);
		connect.add(OTFLinkLanesAgentsNoParkingHandler.Writer.class, OTFLinkLanesAgentsNoParkingHandler.class);
		connect.add(OTFLinkLanesAgentsNoParkingHandler.class,  AgentPointDrawer.class);
		connect.add(OTFLinkLanesAgentsNoParkingHandler.class, SimpleStaticNetLayer.SimpleQuadDrawer.class);
		/*
		 * Only needed for backward compatibility, see comment above (dg, nov 09)
		 */
		connect.add(OTFDefaultLinkHandler.Writer.class, OTFDefaultLinkHandler.class);
		connect.add(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
		connect.add(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
		connect.add(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
		connect.add(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
		connect.add(OTFLinkAgentsHandler.class, SimpleStaticNetLayer.SimpleQuadDrawer.class);
		connect.add(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);
		connect.add(OTFLinkAgentsHandler.class,  AgentPointDrawer.class);
		connect.add(OTFAgentsListHandler.Writer.class,  OTFAgentsListHandler.class);
		connect.add(AgentPointDrawer.class, OGLAgentPointLayer.class);
		connect.add(OTFAgentsListHandler.class,  AgentPointDrawer.class);
	}

	public OTFClient(String filename2, OTFConnectionManager connect) {
		this(filename2);
		this.connect = connect;
	}
	
	@Override
	public void run() {
		boolean fullscreen = false;

//		OTFVisConfig visconf = new OTFVisConfig();
//		if (Gbl.getConfig() == null) Gbl.createConfig(null);
//		Gbl.getConfig().addModule(OTFVisConfig.GROUP_NAME, visconf);
		
		OTFVisConfig visconf = Gbl.getConfig().otfVis() ;
		
		OTFFrame frame = new OTFFrame("MATSIM OTFVis", isMac);
		
		if (fullscreen) {
			GraphicsDevice gd = frame.getGraphicsConfiguration().getDevice();
			frame.setUndecorated(true);
			gd.setFullScreenWindow(frame);
		}

		JSplitPane pane = frame.getSplitPane();

		try {
			
			OTFHostControlBar hostControl = new OTFHostControlBar(url);
			hostControl.frame = frame;
			if (hostControl.isLiveHost()) {
				visconf.setCachingAllowed(false); // no use to cache in live mode
			}
			frame.getContentPane().add(hostControl, BorderLayout.NORTH);
			String netName= Gbl.getConfig().network().getInputFile();
			OTFFileSettingsSaver saver = new OTFFileSettingsSaver(netName);
			saver.readDefaultSettings();
			
			PreferencesDialog.buildMenu(frame, visconf, hostControl, saver);

			OTFClientQuad clientQ = hostControl.createNewView(id1, connect);

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
//			hostControl2 = hostControl;
//			Gbl.startMeasurement();
//			hostControl2.countto(3600);
//			Gbl.printElapsedTime();
			//new Thread(){public void run(){ hostControl2.countto(7200);};}.start();
			frame.setVisible(true);
			drawer.invalidate((int)hostControl.getTime());
			hostControl.addHandler(id1, drawer);

		} catch (RemoteException e) {
			// TODO DS Handle with grace!
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		if(args.length==0) {args = new String[] {"rmi:127.0.0.1:4019"};};
		OTFClient client = new OTFClient(args[0]);
		client.start();
	}

}
