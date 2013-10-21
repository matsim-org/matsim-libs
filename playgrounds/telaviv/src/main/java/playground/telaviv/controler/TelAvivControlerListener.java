/* *********************************************************************** *
 * project: org.matsim.*
 * TelAvivControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.telaviv.controler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.facilities.algorithms.WorldConnectLocations;

public class TelAvivControlerListener implements StartupListener {

	@Override
	public void notifyStartup(StartupEvent event) {
		
		Controler controler = event.getControler();
		Scenario scenario = controler.getScenario();
		Config config = controler.getConfig();
		
		// connect facilities to links
		new WorldConnectLocations(config).connectFacilitiesWithLinks(scenario.getActivityFacilities(), 
				(NetworkImpl) scenario.getNetwork());
	}

}
