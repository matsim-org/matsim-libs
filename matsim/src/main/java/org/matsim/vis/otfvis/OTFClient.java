/* *********************************************************************** *
 * project: org.matsim.*
 * OTFClient
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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadI;
import org.matsim.vis.otfvis.data.fileio.OTFFileWriter;
import org.matsim.vis.otfvis.gui.OTFFrame;
import org.matsim.vis.otfvis.gui.OTFHostConnectionManager;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.gui.PreferencesDialog;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.opengl.gui.OTFAbstractSettingsSaver;


/**
 * @author dgrether
 *
 */
public abstract class OTFClient extends Thread {

  private static final Logger log = Logger.getLogger(OTFClient.class);
	
  protected OTFVisConfig visconf;

	protected String url;
	
	protected OTFFrame frame;
	
	protected JSplitPane pane = null;

	protected OTFHostControlBar hostControlBar = null;
	
	protected OTFAbstractSettingsSaver saver;

	protected OTFDrawer mainDrawer;
	
	public OTFClient(String url) {
		this.url = url;
	}

	@Override
	public void run() {
		getOTFVisConfig();
		log.info("got OTFVis config");
		createMainFrame();
		log.info("created MainFrame");
		createHostControlBar();
		log.info("created HostControlBar");
		createDrawer();
		log.info("created drawer");
		addDrawerToSplitPane(this.url);
		this.hostControlBar.addDrawer(this.url, mainDrawer);
		try {
			mainDrawer.invalidate((int)hostControlBar.getOTFHostControl().getTime());
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		frame.setVisible(true);
		log.info("OTFVis finished init");
	}

	private void addDrawerToSplitPane(String url2) {
	  pane.setRightComponent(this.createDrawerPanel(url2, mainDrawer));
		pane.validate();
	}
	
	protected JPanel createDrawerPanel(String url, OTFDrawer drawer){
    JPanel panel = new JPanel(new BorderLayout());
    JLabel label = new JLabel();
    label.setText(url);
    panel.add(drawer.getComponent(), BorderLayout.CENTER);
    panel.add(label, BorderLayout.NORTH);
    return panel;
	}

	public OTFClientQuad createNewView(String id, OTFConnectionManager connect, OTFHostConnectionManager hostControl) throws RemoteException {
		OTFVisConfig config = (OTFVisConfig)Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME);

		if((config.getFileVersion() < OTFFileWriter.VERSION) || (config.getFileMinorVersion() < OTFFileWriter.MINORVERSION)) {
			// go through every reader class and look for the appropriate Reader Version for this fileformat
			connect.adoptFileFormat(OTFDataReader.getVersionString(config.getFileVersion(), config.getFileMinorVersion()));
		}

		log.info("Getting Quad id " + id);
		OTFServerQuadI servQ = hostControl.getOTFServer().getQuad(id, connect);
//    log.error("");
//    log.error("connection manager used...");
//    log.error("");
//    for (Entry e : connect.getEntries()){
//      log.error("entry from: " + e.getFrom() + " to " + e.getTo());
//    }
		log.info("Converting Quad");
		OTFClientQuad clientQ = servQ.convertToClient(id, hostControl.getOTFServer(), connect);
		log.info("Creating receivers...");
		clientQ.createReceiver(connect);
		log.info("Reading data...");
		clientQ.getConstData();
		this.hostControlBar.updateTimeLabel();
		hostControl.getQuads().put(id, clientQ);
		log.info("Created OTFClientQuad!");
		return clientQ;
	}
	
	protected void createHostControlBar() {
		try {
			this.hostControlBar = new OTFHostControlBar(this.url);
			hostControlBar.frame = frame;
			frame.getContentPane().add(this.hostControlBar, BorderLayout.NORTH);
			PreferencesDialog preferencesDialog = new PreferencesDialog(frame, visconf, hostControlBar);
			preferencesDialog.buildMenu(frame, preferencesDialog, saver);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
	}

	protected void createMainFrame(){
		this.frame = new OTFFrame("MATSim OTFVis");
		this.pane = frame.getSplitPane();
		this.pane.setDividerLocation(0.5);
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.frame.setSize(screenSize.width/2,screenSize.height/2);
	}

	protected abstract void createDrawer();

	protected abstract void getOTFVisConfig();

}
