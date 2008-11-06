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

package org.matsim.utils.vis.otfvis.opengl;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;

import org.matsim.gbl.Gbl;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.utils.vis.otfvis.data.OTFClientQuad;
import org.matsim.utils.vis.otfvis.data.OTFConnectionManager;
import org.matsim.utils.vis.otfvis.gui.OTFHostControlBar;
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
import org.matsim.utils.vis.otfvis.opengl.gui.OTFTimeLine;
import org.matsim.utils.vis.otfvis.opengl.layer.ColoredStaticNetLayer;
import org.matsim.utils.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.utils.vis.otfvis.opengl.layer.SimpleStaticNetLayer;
import org.matsim.utils.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;



public class OnTheFlyClientFileQuad extends Thread {
	protected OTFHostControlBar hostControl = null;

	private final String filename;
	private boolean splitLayout = true;

	OTFConnectionManager connect = new OTFConnectionManager();

	//NetworkLayer network = new NetworkLayer();


	public OTFDrawer getLeftDrawerComponent( JFrame frame) throws RemoteException {
		OTFConnectionManager connectL = this.connect.clone();
		connectL.remove(OTFLinkAgentsHandler.class);

		connectL.add(OTFLinkAgentsHandler.class, ColoredStaticNetLayer.QuadDrawer.class);
		connectL.add(ColoredStaticNetLayer.QuadDrawer.class, ColoredStaticNetLayer.class);
		OTFClientQuad clientQ = this.hostControl.createNewView(null, null, connectL);

		OTFDrawer drawer = new OTFOGLDrawer(frame, clientQ);
		// DS TODO Repair painting of this colored net drawer.isActiveNet = true;
		return drawer;
	}

	public OTFDrawer getRightDrawerComponent( JFrame frame) throws RemoteException {
		OTFConnectionManager connectR = this.connect.clone();
		connectR.remove(OTFLinkAgentsHandler.class);

		connectR.add(OTFLinkAgentsHandler.class,  SimpleStaticNetLayer.SimpleQuadDrawer.class);
		connectR.add(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);
		connectR.add(OTFLinkAgentsHandler.class,  AgentPointDrawer.class);
		connectR.add(OGLAgentPointLayer.AgentPointDrawer.class, OGLAgentPointLayer.class);


		OTFClientQuad clientQ2 = this.hostControl.createNewView(null, null, connectR);

		OTFDrawer drawer2 = new OTFOGLDrawer(frame, clientQ2);

		return drawer2;
	}

	@Override
	public void run() {
		System.setProperty("javax.net.ssl.keyStore", "input/keystore");
		System.setProperty("javax.net.ssl.keyStorePassword", "vspVSP");
		System.setProperty("javax.net.ssl.trustStore", "input/truststore");
		System.setProperty("javax.net.ssl.trustStorePassword", "vspVSP");
		
		boolean isMac = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
		if (isMac) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		}

		try {

			//Make sure menus appear above JOGL Layer
			JPopupMenu.setDefaultLightWeightPopupEnabled(false); 
			
			if (Gbl.getConfig() == null) Gbl.createConfig(null);
			
			OTFVisConfig visconf = (OTFVisConfig) Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME);
			if (visconf == null) {
				visconf = new OTFVisConfig();
				Gbl.getConfig().addModule(OTFVisConfig.GROUP_NAME, visconf);
			}
			
			
			
			



//			hostControl = new OTFHostControlBar("file:../MatsimJ/output/OTFQuadfile10p.mvi.gz");
			System.out.println("Loading file " + this.filename + " ....");
			this.hostControl = new OTFHostControlBar("file:" + this.filename);
			JFrame frame = new JFrame("MATSim OTFVis");
			if (isMac) {
				frame.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
			}
			hostControl.frame = frame;

			frame.getContentPane().add(this.hostControl, BorderLayout.NORTH);
			JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			pane.setContinuousLayout(true);
			pane.setOneTouchExpandable(true);
			frame.getContentPane().add(pane);
			PreferencesDialog.buildMenu(frame, visconf, this.hostControl);

			if(!hostControl.isLiveHost()) frame.getContentPane().add(new OTFTimeLine("time", hostControl), BorderLayout.SOUTH);


			// Maybe later: connect.add(QueueLink.class, OTFDefaultLinkHandler.Writer.class);
			// connect.add(QueueNode.class, OTFDefaultNodeHandler.Writer.class);

			if (this.splitLayout) {
				OTFDrawer drawer = getLeftDrawerComponent(frame);
				drawer.invalidate((int)hostControl.getTime());
				this.hostControl.addHandler("test", drawer);
				pane.setLeftComponent(drawer.getComponent());
			}

			OTFDrawer drawer2 = getRightDrawerComponent(frame);
			pane.setRightComponent(drawer2.getComponent());
			this.hostControl.addHandler("test2", drawer2);
			drawer2.invalidate((int)hostControl.getTime());

			System.out.println("Finished init");
			pane.setDividerLocation(0.5);
	        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			frame.setSize(screenSize.width/2,screenSize.height/2);
			frame.setVisible(true);

			//InfoText.showText("Loaded...");

			hostControl.finishedInitialisition();


		}catch (RemoteException e) {
			e.printStackTrace();
			throw new RuntimeException();
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

		if (args.length == 1) {
			filename = args[0];
		} else {
//			filename = "../MatsimJ/output/OTFQuadfileNoParking10p_wip.mvi.gz";
//			filename = "output/OTFQuadfile.mvi";
//			filename = "../../tmp/1000.events.mvi";
//			filename = "/TU Berlin/workspace/MatsimJ/output/OTFQuadfileNoParking10p_wip.mvi";
			filename = "/TU Berlin/workspace/MatsimJ/otfvisSwitzerland10p.mvi";
		}

		
		OnTheFlyClientFileQuad client = new OnTheFlyClientFileQuad(filename);
		client.run();
	}

	public OnTheFlyClientFileQuad( String filename) {
		super();
		this.filename = filename;

		this.connect.add(OTFDefaultLinkHandler.Writer.class, OTFDefaultLinkHandler.class);
		this.connect.add(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
		this.connect.add(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
		this.connect.add(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
		this.connect.add(OTFAgentsListHandler.Writer.class,  OTFAgentsListHandler.class);
		this.connect.add(OTFAgentsListHandler.class,  OGLAgentPointLayer.AgentPointDrawer.class);
		this.connect.add(AgentPointDrawer.class, OGLAgentPointLayer.class);
		this.connect.add(OTFLinkLanesAgentsNoParkingHandler.Writer.class, OTFLinkLanesAgentsNoParkingHandler.class);
		this.connect.add(QueueLink.class, OTFLinkLanesAgentsNoParkingHandler.Writer.class);
		this.connect.add(OTFLinkLanesAgentsNoParkingHandler.class, SimpleStaticNetLayer.SimpleQuadDrawer.class);
		splitLayout = false;
	}

	public OnTheFlyClientFileQuad( String filename2,  OTFConnectionManager connect) {
		this(filename2);
		this.connect = connect;
	}

	public OnTheFlyClientFileQuad( String filename2,  OTFConnectionManager connect,  boolean split) {
		this(filename2);
		this.connect = connect;
		this.splitLayout = split;
	}



//	private static DisplayableNetI prepareNet() {
//	String fileName = "E:\\Development\\tmp\\studies\\berlin-wip\\network\\wip_net.xml";
//	NetReaderI netReader = new MatsimNetReader_PLAIN();
//	NetBuffer buffer = new NetBuffer();
//	netReader.readNetwork(buffer, fileName);

//	DisplayNet network = new DisplayNet();
//	NetComposerI netComposer = new TrafficNetComposer();
//	netComposer.compose(network, buffer);
//	return network;

//	}
}
