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
import java.rmi.RemoteException;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.gui.OTFQueryControl;
import org.matsim.vis.otfvis.gui.OTFQueryControlToolBar;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gui.OTFLiveSettingsSaver;

public class OTFClientLive extends OTFClient {

	private static final Logger log = Logger.getLogger(OTFClientLive.class);

	private OTFConnectionManager connect = new OTFConnectionManager();


	public OTFClientLive(String url, OTFConnectionManager connect) {
		super(url);
		this.connect = connect;
	}

	@Override
	protected void getOTFVisConfig() {
		if (this.visconf == null) {
			log.warn("No otfvis config set, using defaults");
			this.visconf = new OTFVisConfig();
		}
		String netName = Gbl.getConfig().network().getInputFile();
		saver = new OTFLiveSettingsSaver(this.visconf, netName);
		((OTFLiveSettingsSaver)saver).readDefaultSettings();
		visconf.setCachingAllowed(false); // no use to cache in live mode
	}
	
	@Override
	protected void createDrawer(){
		try {		
			OTFClientQuad clientQ = createNewView(this.url, connect, this.hostControlBar.getOTFHostControl());
			mainDrawer = new OTFOGLDrawer(this.visconf, this.frame, clientQ);
			
			if (hostControlBar.getOTFHostControl().isLiveHost()) {
				OTFQueryControl queryControl = new OTFQueryControl(hostControlBar, visconf);
				OTFQueryControlToolBar queryControlBar = new OTFQueryControlToolBar(queryControl, visconf);
				queryControl.setQueryTextField(queryControlBar.getTextField());
				frame.getContentPane().add(queryControlBar, BorderLayout.SOUTH);
				((OTFOGLDrawer) mainDrawer).setQueryHandler(queryControl);
			}
			else {
				throw new IllegalStateException("Server not in live mode!");
			}
			

		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void setConfig(OTFVisConfig otfVisConfig) {
			this.visconf = otfVisConfig;
	}

	
}
