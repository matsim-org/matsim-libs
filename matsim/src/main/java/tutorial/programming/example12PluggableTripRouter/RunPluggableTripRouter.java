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

package tutorial.programming.example12PluggableTripRouter;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripRouterFactory;

public class RunPluggableTripRouter {

	public static void main(final String[] args) {

		Config config;
		if ( args.length==0 ) {
			config = ConfigUtils.loadConfig("examples/equil/config.xml") ;
		} else {
			config = ConfigUtils.loadConfig(args[0]);
		}
		
		final Controler controler = new Controler(config);
		
		MySimulationObserver observer = new MySimulationObserver();
		controler.getEvents().addHandler(observer) ; 
		
		TripRouterFactory factory = new MyTripRouterFactory(observer) ; 
		controler.setTripRouterFactory(factory) ;

		controler.run();

	}

}
