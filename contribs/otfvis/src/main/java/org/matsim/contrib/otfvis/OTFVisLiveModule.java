/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * OTFVisModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.contrib.otfvis;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

public class OTFVisLiveModule extends AbstractModule {

	@Override
	public void install() {
		this.addMobsimListenerBinding().to( OTFVisMobsimListener.class ) ;
	}

	private static class OTFVisMobsimListener implements MobsimInitializedListener{
		@Inject Scenario scenario ;
		@Inject EventsManager events ;
		@Override 
		public void notifyMobsimInitialized(MobsimInitializedEvent e) {
			QSim qsim = (QSim) e.getQueueSimulation() ; 
			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim( scenario.getConfig(), scenario, events, qsim);
			OTFClientLive.run(scenario.getConfig(), server);
		}
	}
	
}
