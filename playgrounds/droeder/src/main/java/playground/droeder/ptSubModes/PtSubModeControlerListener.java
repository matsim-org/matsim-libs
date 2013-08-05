/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.droeder.ptSubModes;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import playground.droeder.ptSubModes.qSimHook.TransitSubModeQSimFactory;
import playground.droeder.ptSubModes.routing.PtSubModeRouterFactory;
import playground.droeder.ptSubModes.routing.PtSubModeTripRouterFactory;

/**
 * @author droeder
 *
 */
public class PtSubModeControlerListener implements StartupListener{

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(PtSubModeControlerListener.class);
	private boolean routeOnSameMode;
	private PtSubModeRouterFactory transitRouterFactory;

	public PtSubModeControlerListener(boolean routeOnSameMode) {
		this.routeOnSameMode = routeOnSameMode;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		Controler c = event.getControler();
		c.setMobsimFactory(new TransitSubModeQSimFactory(this.routeOnSameMode));
		this.transitRouterFactory = new PtSubModeRouterFactory(c, this.routeOnSameMode);
		c.setTripRouterFactory(new PtSubModeTripRouterFactory(c, transitRouterFactory));
		c.addControlerListener((PtSubModeRouterFactory) transitRouterFactory);
	}

	/**
	 * @return
	 */
	public PtSubModeRouterFactory getTransitRouterFactory() {
		return (PtSubModeRouterFactory) transitRouterFactory;
	}
}

