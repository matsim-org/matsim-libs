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

package playground.vsp.parkAndRide.replanning;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.pt.replanning.TransitActsRemoverStrategy;

import playground.vsp.parkAndRide.PRConfigGroup;
import playground.vsp.parkAndRide.PRFacility;
import playground.vsp.parkAndRide.PRFileReader;

import javax.inject.Provider;

/**
 * A way of plugging park-and-ride strategy modules together. Via config file: <param name="Module_#" value="playground.vsp.parkAndRide.replanning.PRStrategyLocation" />
 * 
 * @author ikaddoura
 *
 */
public class PRStrategyLocation implements PlanStrategy {

	PlanStrategyImpl planStrategyDelegate = null ;
	
	public PRStrategyLocation(MatsimServices controler, Provider<TripRouter> tripRouterProvider) {
		
		PRConfigGroup prSettings = (PRConfigGroup) controler.getConfig().getModule(PRConfigGroup.GROUP_NAME);
		PRFileReader prReader = new PRFileReader(prSettings.getInputFile());
		Map<Id<PRFacility>, PRFacility> id2prFacility = prReader.getId2prFacility();
		
		RandomPlanSelector planSelector = new RandomPlanSelector();
		planStrategyDelegate = new PlanStrategyImpl( planSelector );
				
		TransitActsRemoverStrategy transitActsRemoveModule = new TransitActsRemoverStrategy(controler.getConfig());
		planStrategyDelegate.addStrategyModule(transitActsRemoveModule) ;

		PRLocationStrategyMod prLocationMod = new PRLocationStrategyMod(controler, id2prFacility, prSettings.getGravity(), prSettings.getTypicalDuration());
		planStrategyDelegate.addStrategyModule(prLocationMod);
		
		ReRoute reRouteModule = new ReRoute( controler.getScenario(), tripRouterProvider) ;
		planStrategyDelegate.addStrategyModule(reRouteModule) ;
		
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
