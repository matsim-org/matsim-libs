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
/*
 * $Id: MyControler1.java,v 1.1 2007/11/14 12:00:28 nagel Exp $
 */

import org.matsim.controler.Controler;
import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.PlanStrategyModule;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.NetworkLayer;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.selectors.PlanSelector;
import org.matsim.replanning.selectors.RandomPlanSelector;


public class MyStrategy extends PlanStrategy {
	
	

	public MyStrategy(Controler controler) {
		super(new RandomPlanSelector());
		
		MyModule mod = new MyModule( controler ) ;
		
		addStrategyModule(mod) ;
		
		Events events = controler.getEvents() ;
		events.addHandler( mod ) ;
		
	}

	public static void main(final String[] args) {

		if ( args.length==0 ) {
			Gbl.createConfig(new String[] {"./examples/equil/myconfig.xml"});
		} else {
			Gbl.createConfig(args) ;
		}

		final Controler controler = new Controler(Gbl.getConfig());
		controler.setOverwriteFiles(true);
		controler.run();

	}

}
