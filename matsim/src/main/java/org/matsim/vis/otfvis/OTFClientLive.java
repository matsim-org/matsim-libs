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

import javax.swing.SwingUtilities;

import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.gui.OTFHostConnectionManager;
import org.matsim.vis.otfvis.gui.OTFQueryControl;
import org.matsim.vis.otfvis.gui.OTFQueryControlToolBar;
import org.matsim.vis.otfvis.gui.OTFVisConfigGroup;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gui.SettingsSaver;

public class OTFClientLive implements Runnable {

	private OTFClient otfClient = new OTFClient();

	private OTFConnectionManager connect = new OTFConnectionManager();

	private SettingsSaver saver;

	private OTFHostConnectionManager masterHostControl;

	public OTFClientLive(OnTheFlyServer otfServer, OTFConnectionManager connectionManager) {
		super();
		masterHostControl = new OTFHostConnectionManager("live", otfServer);
		otfClient.setHostConnectionManager(masterHostControl);
		this.connect = connectionManager;
	}

	private OTFVisConfigGroup createOTFVisConfig() {
		saver = new SettingsSaver("otfsettings");
		OTFVisConfigGroup visconf = saver.tryToReadSettingsFile();
		if (visconf == null) {
			visconf = this.masterHostControl.getOTFServer().getOTFVisConfig();
		}
		visconf.setCachingAllowed(false); // no use to cache in live mode
		return visconf;
	}

	private OTFDrawer createDrawer(){
		OTFClientQuad clientQ = otfClient.createNewView(connect);
		OTFOGLDrawer mainDrawer = new OTFOGLDrawer(clientQ, otfClient.getHostControlBar());
		OTFQueryControl queryControl = new OTFQueryControl(otfClient.getHostControlBar(), OTFClientControl.getInstance().getOTFVisConfig());
		OTFQueryControlToolBar queryControlBar = new OTFQueryControlToolBar(queryControl, OTFClientControl.getInstance().getOTFVisConfig());
		queryControl.setQueryTextField(queryControlBar.getTextField());
		otfClient.getFrame().getContentPane().add(queryControlBar, BorderLayout.SOUTH);
		mainDrawer.setQueryHandler(queryControl);
		return mainDrawer;
	}
	
	@Override
	public final void run() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				OTFClientControl.getInstance().setOTFVisConfig(createOTFVisConfig());
				otfClient.addDrawerAndInitialize(createDrawer(), saver);
				otfClient.show();
			}
		});
	}

}
