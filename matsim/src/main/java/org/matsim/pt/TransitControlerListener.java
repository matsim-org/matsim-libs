/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.pt;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImplFactory;

/**
 * @author mrieser
 */
public class TransitControlerListener implements StartupListener {

	@Override
	public void notifyStartup(final StartupEvent event) {
		final Scenario scenario = event.getControler().getScenario();
		if (event.getControler().getTransitRouterFactory() == null) {
			
			TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig().planCalcScore(),
					scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(),
					scenario.getConfig().vspExperimental());
			
			event.getControler().setTransitRouterFactory(new TransitRouterImplFactory(
					scenario.getTransitSchedule(), transitRouterConfig ));
		}

	}

}