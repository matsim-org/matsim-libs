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

package playground.kai.usecases.basicmentalmodule;
/*
 * $Id: MyControler1.java,v 1.1 2007/11/14 12:00:28 nagel Exp $
 */

import org.matsim.api.basic.v01.BasicScenario;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.Events;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.selectors.RandomPlanSelector;


public class MyStrategy extends PlanStrategy {
	
	public MyStrategy(Controler controler) {
		super(new RandomPlanSelector());
		
		BasicScenario sc = controler.getScenarioData() ;
		
		MyModule mod = new MyModule( sc ) ;
		
		addStrategyModule(mod) ;
		
		Events events = controler.getEvents() ;
		events.addHandler( mod ) ;
		
	}

}
