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

package org.matsim.vis.otfvis.opengl;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.filechooser.FileFilter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.gui.PreferencesDialog;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultLinkHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultNodeHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsNoParkingHandler;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFSettingsSaver;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gui.OTFFileSettingsSaver;
import org.matsim.vis.otfvis.opengl.gui.OTFTimeLine;
import org.matsim.vis.otfvis.opengl.layer.ColoredStaticNetLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.SimpleStaticNetLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;

import de.schlichtherle.io.ArchiveDetector;
import de.schlichtherle.io.DefaultArchiveDetector;

class OTFFrame extends JFrame {
	   public OTFFrame(String string) {
		super(string);
	}

	protected void processWindowEvent(WindowEvent e) {

	        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
	        	OnTheFlyClientFileQuad.endProgram(0);
	        } else {
		        super.processWindowEvent(e);
	        }
	    }
}


public class OnTheFlyClientFileQuad extends Thread {
	
	public static void endProgram(int code) {
		OTFVisConfig config = (OTFVisConfig)Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME);
		if(config.isModified()) {
			final JDialog dialog = new JDialog((JFrame)null, "Preferences are unsaved and modified...", true);
			final JOptionPane optionPane = new JOptionPane(
				    "There are potenially unsaved changes in Preferences.\nQuit anyway?",
				    JOptionPane.QUESTION_MESSAGE,
				    JOptionPane.YES_NO_OPTION);
			dialog.setContentPane(optionPane);
			dialog.setDefaultCloseOperation(
				    JDialog.DO_NOTHING_ON_CLOSE);
				dialog.addWindowListener(new WindowAdapter() {
				    public void windowClosing(WindowEvent we) {
				        
				    }
				});
				optionPane.addPropertyChangeListener(
				    new PropertyChangeListener() {
				        public void propertyChange(PropertyChangeEvent e) {
				            String prop = e.getPropertyName();

				            if (dialog.isVisible() 
				             && (e.getSource() == optionPane)
				             && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
				                //If you were going to check something
				                //before closing the window, you'd do
				                //it here.
				                dialog.setVisible(false);
				            }
				        }
				    });
			dialog.pack();
			dialog.setVisible(true);
			int value = ((Integer)optionPane.getValue()).intValue();
			if (value == JOptionPane.NO_OPTION) {
				return; // do not quit
			}

		}
		System.exit(code);
	}
	
	protected OTFHostControlBar hostControl = null;

	private final String filename;
	private boolean splitLayout = true;
	protected JSplitPane pane = null;

	protected OTFConnectionManager connect = new OTFConnectionManager();

	//NetworkLayer network = new NetworkLayer();


	public OTFDrawer getLeftDrawerComponent(JFrame frame) throws RemoteException {
		OTFConnectionManager connectL = this.connect.clone();
		connectL.remove(OTFLinkAgentsHandler.class);

		connectL.add(OTFLinkAgentsHandler.class, ColoredStaticNetLayer.QuadDrawer.class);
		connectL.add(ColoredStaticNetLayer.QuadDrawer.class, ColoredStaticNetLayer.class);
		OTFClientQuad clientQ = this.hostControl.createNewView(null, connectL);

		OTFDrawer drawer = new OTFOGLDrawer(frame, clientQ);
		// DS TODO Repair painting of this colored net drawer.isActiveNet = true;
		return drawer;
	}

	public OTFDrawer getRightDrawerComponent(JFrame frame) throws RemoteException {
		OTFConnectionManager connectR = this.connect.clone();
		connectR.remove(OTFLinkAgentsHandler.class);

		connectR.add(OTFLinkAgentsHandler.class,  SimpleStaticNetLayer.SimpleQuadDrawer.class);
		connectR.add(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);
		connectR.add(OTFLinkAgentsHandler.class,  AgentPointDrawer.class);
		connectR.add(OTFLinkLanesAgentsNoParkingHandler.class,  AgentPointDrawer.class);
		connectR.add(OGLAgentPointLayer.AgentPointDrawer.class, OGLAgentPointLayer.class);


		OTFClientQuad clientQ2 = this.hostControl.createNewView(null, connectR);

		OTFDrawer drawer2 = new OTFOGLDrawer(frame, clientQ2);

		return drawer2;
	}


	protected JFrame prepareRun() {
		System.setProperty("javax.net.ssl.keyStore", "input/keystore");
		System.setProperty("javax.net.ssl.keyStorePassword", "vspVSP");
		System.setProperty("javax.net.ssl.trustStore", "input/truststore");
		System.setProperty("javax.net.ssl.trustStorePassword", "vspVSP");
		
		boolean isMac = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
		if (isMac) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		}

		JFrame frame = new OTFFrame("MATSim OTFVis");
	    frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE ); 

		if (isMac) {
			frame.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
		}
		JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		pane.setContinuousLayout(true);
		pane.setOneTouchExpandable(true);
		frame.getContentPane().add(pane);
		this.pane = pane;

		//Make sure menus appear above JOGL Layer
		JPopupMenu.setDefaultLightWeightPopupEnabled(false); 
		
		if (Gbl.getConfig() == null) Gbl.createConfig(null);
		OTFFileSettingsSaver saver = new OTFFileSettingsSaver(this.filename);
		
		OTFVisConfig visconf = (OTFVisConfig) Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME);
		if (visconf == null) {
			visconf = saver.openAndReadConfig();
		} else {
			System.out.println("OTFVisConfig already defined, cant read settings from file");
		}

		try {

			
			System.out.println("Loading file " + this.filename + " ....");
			this.hostControl = new OTFHostControlBar("file:" + this.filename);
			hostControl.frame = frame;

			frame.getContentPane().add(this.hostControl, BorderLayout.NORTH);
			PreferencesDialog.buildMenu(frame, visconf, this.hostControl, saver);

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
		return frame;
	}
	
	@Override
	public void run() {
		prepareRun();
		hostControl.finishedInitialisition();
	}

	public static void main( String[] args) {

		String filename;

		if (args.length == 1) {
			filename = args[0];
		} else {
//			filename = "../MatsimJ/output/OTFQuadfileNoParking10p_wip.mvi.gz";
			filename = "output/OTFQuadfile10p.mvi";
//			filename = "../../tmp/1000.events.mvi";
//			filename = "/TU Berlin/workspace/MatsimJ/output/OTFQuadfileNoParking10p_wip.mvi";
//			filename = "/TU Berlin/workspace/MatsimJ/otfvisSwitzerland10p.mvi";
			filename = "testCUDA10p.mvi";
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


	public OnTheFlyClientFileQuad( String filename2,  boolean split) {
		this(filename2);
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
