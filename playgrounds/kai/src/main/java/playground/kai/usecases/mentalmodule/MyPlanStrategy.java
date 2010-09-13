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

import org.matsim.core.controler.Controler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.replanning.PlanStrategy;


public class MyPlanStrategy extends PlanStrategy {

	public MyPlanStrategy(Controler controler) {
		// also possible: MyStrategy( Scenario scenario ).  But then I do not have events.  kai, aug'10
		
		// A PlanStrategy is something that can be applied to a person(!).  
		
		// It first selects one of the plans:
		super( new MyPlanSelector() );
		
		// the plan selector may, at the same time, collect events:
		controler.getEvents().addHandler( (EventHandler) this.getPlanSelector() ) ;
		
		// if you just want to select plans, you can stop here.  
		
		// Otherwise, to do something with that plan, one needs to add modules into the strategy.  If there is at least 
		// one module added here, then the plan is copied and then modified.
		MyPlanStrategyModule mod = new MyPlanStrategyModule( controler ) ;
		addStrategyModule(mod) ;

		// these modules may, at the same time, be events listeners (so that they can collect information):
		controler.getEvents().addHandler( mod ) ;
		
	}

}
