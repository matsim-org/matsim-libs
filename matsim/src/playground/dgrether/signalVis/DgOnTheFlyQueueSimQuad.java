/* *********************************************************************** *
 * project: org.matsim.*
 * DgOnTheFlyQueueSimQuad
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
package playground.dgrether.signalVis;

import java.rmi.RemoteException;
import java.util.UUID;

import org.matsim.core.api.experimental.ScenarioImpl;
import org.matsim.core.events.Events;
import org.matsim.vis.otfvis.gui.PreferencesDialog;
import org.matsim.vis.otfvis.opengl.OnTheFlyClientQuad;
import org.matsim.vis.otfvis.opengl.OnTheFlyQueueSimQuad;
import org.matsim.vis.otfvis.opengl.gui.PreferencesDialog2;
import org.matsim.vis.otfvis.server.OnTheFlyServer;

/**
 * @author dgrether
 * 
 */
public class DgOnTheFlyQueueSimQuad extends OnTheFlyQueueSimQuad {

	/**
	 * @param net
	 * @param plans
	 * @param events
	 */
	public DgOnTheFlyQueueSimQuad(ScenarioImpl scenario, Events events) {
		super(scenario, events);
	}

	@Override
	protected void prepareSim() {

		UUID idOne = UUID.randomUUID();
		this.myOTFServer = OnTheFlyServer.createInstance("OTFServer_" + idOne.toString(), this.network, this.plans,
				getEvents(), false);

		DgOTFConnectionManager connectionManager = new DgOTFConnectionManager();
		
		
		// FOR TESTING ONLY!
		PreferencesDialog.preDialogClass = PreferencesDialog2.class;
		OnTheFlyClientQuad client = new OnTheFlyClientQuad("rmi:127.0.0.1:4019:OTFServer_" + idOne.toString(), connectionManager);
		client.start();

		try {
			this.myOTFServer.pause();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
