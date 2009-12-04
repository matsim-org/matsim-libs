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

package org.matsim.vis.otfvis;

import java.awt.BorderLayout;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.swing.JSplitPane;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.gui.OTFFrame;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.OTFQueryControlBar;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.gui.PreferencesDialog;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gui.OTFFileSettingsSaver;

public class OTFClient extends Thread {

	private static final Logger log = Logger.getLogger(OTFClient.class);

	private final String url;

	private OTFConnectionManager connect = new OTFConnectionManager();

	public static OTFHostControlBar hostControl2;

	public OTFClient(String url, OTFConnectionManager connect) {
		this.url = url;
		this.connect = connect;
	}

	@Override
	public void run() {
		String id1 = "test1";
		OTFVisConfig visconf = Gbl.getConfig().otfVis();
		OTFFrame frame = new OTFFrame("MATSIM OTFVis");
		JSplitPane pane = frame.getSplitPane();
		try {

			OTFHostControlBar hostControl = new OTFHostControlBar(url);
			hostControl.frame = frame;
			if (hostControl.isLiveHost()) {
				visconf.setCachingAllowed(false); // no use to cache in live mode
			}
			frame.getContentPane().add(hostControl, BorderLayout.NORTH);
			String netName = Gbl.getConfig().network().getInputFile();
			OTFFileSettingsSaver saver = new OTFFileSettingsSaver(netName);
			saver.readDefaultSettings();

			PreferencesDialog.buildMenu(frame, visconf, hostControl, saver);

			OTFClientQuad clientQ = hostControl.createNewView(id1, connect);

			OTFDrawer drawer = new OTFOGLDrawer(frame, clientQ);
			pane.setLeftComponent(drawer.getComponent());
			pane.validate();
			if (hostControl.isLiveHost()) {
				OTFQueryControlBar queryControl = new OTFQueryControlBar("test", hostControl, visconf);
				frame.getContentPane().add(queryControl, BorderLayout.SOUTH);
				((OTFOGLDrawer) drawer).setQueryHandler(queryControl);
			}
			frame.setSize(1024, 600);
			frame.setVisible(true);
			drawer.invalidate((int) hostControl.getTime());
			hostControl.addHandler(id1, drawer);

		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}

	}

}
