/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.kai.conceptual.autosensingmargutls;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.ControlerUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * @author nagel
 *
 */
class Main {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig() ;
		
		ActivityParams params = new ActivityParams("h")  ;
		config.planCalcScore().addActivityParams(params);
		double typicalDuration = 4.*3600;
		params.setTypicalDuration(typicalDuration);

		Scenario scenario = ScenarioUtils.createScenario(config) ;

		PopulationFactory pf = scenario.getPopulation().getFactory() ;

		Person person = pf.createPerson(new IdImpl(1)) ;
		List<Double> scores = new ArrayList<Double>() ;
		for ( int ii=0 ; ii<=1 ; ii++ ) {
			Plan plan = pf.createPlan() ;
			person.addPlan(plan) ;

			ScoringFunction sf = ControlerUtils.createDefaultScoringFunctionFactory(scenario).createNewScoringFunction(plan) ;

			Leg testLeg = pf.createLeg(TransportMode.bike) ;
			testLeg.setDepartureTime(7.*3600);
			testLeg.setTravelTime(3600. * ii);

			sf.handleLeg( testLeg );

			Activity testAct = pf.createActivityFromCoord("h", new CoordImpl(0.,0.) ) ;
			testAct.setStartTime( testLeg.getDepartureTime() + testLeg.getTravelTime() );
			testAct.setEndTime( testLeg.getDepartureTime() + typicalDuration ) ;

			sf.handleActivity( testAct );
			
			double score = sf.getScore();
			scores.add( score ) ;
			
			sf.addMoney(1.);
			
			System.out.println( " margUtlOfMon: " + ( sf.getScore() - score ) );
			
		}
		System.out.println( " effMargDisutlOfTrav: " + ( scores.get(1) - scores.get(0) ) );
		

	}

}
