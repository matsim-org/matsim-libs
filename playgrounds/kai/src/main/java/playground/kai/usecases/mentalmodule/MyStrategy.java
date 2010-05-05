/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.kai.usecases.mentalmodule;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.selectors.RandomPlanSelector;


public class MyStrategy extends PlanStrategy {



	public MyStrategy(Controler controler) {
		super(new RandomPlanSelector());

		MyModule mod = new MyModule( controler ) ;

		addStrategyModule(mod) ;

		EventsManager events = controler.getEvents() ;
		events.addHandler( mod ) ;

	}

	public static void main(final String[] args) {

		Config config;
		if ( args.length==0 ) {
			config = Gbl.createConfig(new String[] {"./examples/equil/myconfig.xml"});
		} else {
			config = Gbl.createConfig(args) ;
		}

		final Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);
		controler.run();

	}

}
