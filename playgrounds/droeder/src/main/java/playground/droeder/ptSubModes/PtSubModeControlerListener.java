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


/**
 * @author droeder
 *
 */
public class PtSubModeControlerListener {}//implements StartupListener{
//
//	@SuppressWarnings("unused")
//	private static final Logger log = Logger
//			.getLogger(PtSubModeControlerListener.class);
//	private boolean routeOnSameMode;
//	private PtSubModeRouterFactory transitRouterFactory;
//
//	/**
//	 *  Registers the main classes of the package ptSubModes to the controler. The package provides the functionality
//	 *  to route pt-plans on a previously choosen submode (e.g. an agent plans to use a bus, but not
//	 *  any other part of the pt-system), will definitely use a bus  (if there is one) and not any other vehicle.
//	 *  
//	 * @param routeOnSameMode, allow to use the default behavior when false
//	 */
//	public PtSubModeControlerListener(boolean routeOnSameMode) {
//		this.routeOnSameMode = routeOnSameMode;
//	}
//
//	@Override
//	public void notifyStartup(StartupEvent event) {
//		Controler c = event.getServices();
//		c.setMobsimFactory(new TransitSubModeQSimFactory(this.routeOnSameMode));
//		this.transitRouterFactory = new PtSubModeRouterFactory(c, this.routeOnSameMode);
//		c.setTripRouterFactory(new PtSubModeTripRouterFactory(c, transitRouterFactory));
//		c.addControlerListener((PtSubModeRouterFactory) transitRouterFactory);
//	}
//
//	/**
//	 * @return
//	 */
//	public PtSubModeRouterFactory getTransitRouterFactory() {
//		return (PtSubModeRouterFactory) transitRouterFactory;
//	}
//}

