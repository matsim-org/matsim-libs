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

import javax.swing.JSplitPane;

import org.matsim.vis.otfvis.gui.OTFFrame;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.gui.PreferencesDialog;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.opengl.gui.OTFFileSettingsSaver;


/**
 * @author dgrether
 *
 */
public abstract class OTFClient extends Thread {

	protected OTFVisConfig visconf;

	protected String url;
	
	protected OTFFrame frame;
	
	protected JSplitPane pane = null;

	protected OTFHostControlBar hostControl = null;
	
	protected OTFFileSettingsSaver saver;

	protected OTFDrawer mainDrawer;
	
	public OTFClient(String url) {
		this.url = url;
	}

	@Override
	public void run() {
		getOTFVisConfig();
		createMainFrame();
		createHostControlBar();
		createDrawer();
		addDrawerToSplitPane();
		this.hostControl.addHandler(this.url, mainDrawer);
		try {
			mainDrawer.invalidate((int)hostControl.getOTFHostControl().getTime());
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		frame.setVisible(true);
		System.out.println("Finished init");
	}

	private void addDrawerToSplitPane() {
		pane.setRightComponent(mainDrawer.getComponent());
		pane.validate();

	}

	protected void createHostControlBar() {
		try {
			this.hostControl = new OTFHostControlBar(this.url);
			hostControl.frame = frame;
			frame.getContentPane().add(this.hostControl, BorderLayout.NORTH);
			PreferencesDialog.buildMenu(frame, visconf, hostControl, saver);
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
