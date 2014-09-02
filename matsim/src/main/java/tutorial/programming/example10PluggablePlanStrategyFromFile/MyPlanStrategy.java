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

package tutorial.programming.example10PluggablePlanStrategyFromFile;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;


public class MyPlanStrategy implements PlanStrategy {
	// the reason why this class needs to be here is that this is defined in the config file
	
	PlanStrategyImpl planStrategyDelegate = null ;

	public MyPlanStrategy(Controler controler) {
		// also possible: MyStrategy( Scenario scenario ).  But then I do not have events.  kai, aug'10
		
		// A PlanStrategy is something that can be applied to a person(!).  
		
		// It first selects one of the plans:
		MyPlanSelector planSelector = new MyPlanSelector( controler.getScenario() );
		planStrategyDelegate = new PlanStrategyImpl( planSelector );
		
		// the plan selector may, at the same time, collect events:
		controler.getEvents().addHandler( planSelector ) ;
		
		// if you just want to select plans, you can stop here.  
		
		// Otherwise, to do something with that plan, one needs to add modules into the strategy.  If there is at least 
		// one module added here, then the plan is copied and then modified.
		MyPlanStrategyModule mod = new MyPlanStrategyModule( controler ) ;
		planStrategyDelegate.addStrategyModule(mod) ;

		// these modules may, at the same time, be events listeners (so that they can collect information):
		controler.getEvents().addHandler( mod ) ;
		
	}

	@Override
	public void finish() {
		planStrategyDelegate.finish();
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		planStrategyDelegate.init(replanningContext);
	}

	@Override
	public void run(HasPlansAndId<Plan, Person> person) {
		planStrategyDelegate.run(person);
	}

}
