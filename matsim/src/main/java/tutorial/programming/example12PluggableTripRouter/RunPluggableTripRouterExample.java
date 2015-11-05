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
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;

public class RunPluggableTripRouterExample {

	public static void main(final String[] args) {

		Config config;
		if ( args.length==0 ) {
			config = ConfigUtils.loadConfig("examples/equil/config.xml") ;
		} else {
			config = ConfigUtils.loadConfig(args[0]);
		}
		
		final Controler controler = new Controler(config);
		
		final MySimulationObserver observer = new MySimulationObserver();
		controler.getEvents().addHandler(observer);
		// My observer is an EventHandler. I can ask it what it thinks the world currently looks like,
		// based on the last observed iteration, and pass that into my routing module to make decisions.
		//
		// Do not plug a routing module itself into the EventsManager! Trip routers are short lived,
		// they are recreated as needed (per iteration, per thread...), and factory methods
		// such as this should normally not have visible side effects (such as plugging something
		// into the EventsManager).
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addRoutingModuleBinding("car").toInstance(new MyRoutingModule(observer.getIterationData()));
			}
		});

		controler.run();

	}

}
