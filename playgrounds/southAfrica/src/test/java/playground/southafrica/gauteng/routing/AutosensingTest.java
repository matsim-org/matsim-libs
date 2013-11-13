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
package playground.southafrica.gauteng.routing;

import static org.matsim.core.config.groups.VspExperimentalConfigGroup.VspExperimentalConfigKey.vspDefaultsCheckingLevel;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.VspConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.controler.PlanStrategyRegistrar;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.southafrica.gauteng.routing.EffectiveMarginalUtilitiesContainer;
import playground.southafrica.gauteng.routing.RouterUtils;

/**
 * @author nagel
 *
 */
public class AutosensingTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
    
	@Test
	public final void testOne() {
		// === CONFIG: ===
		Config config = ConfigUtils.createConfig() ;

		ActivityParams params = new ActivityParams("h") ;
		params.setTypicalDuration(12.*3600.);
		config.planCalcScore().addActivityParams(params);
		
		final double monetaryDistanceCostRateCarCONFIG = -0.21/1000.;
		config.planCalcScore().setMonetaryDistanceCostRateCar(monetaryDistanceCostRateCarCONFIG); // utils per meter!
		
		final double marginalUtilityOfMoneyCONFIG = 2. ;
		config.planCalcScore().setMarginalUtilityOfMoney(marginalUtilityOfMoneyCONFIG);
		
		StrategySettings stratSets = new StrategySettings( new IdImpl(1) ) ;
		stratSets.setModuleName( PlanStrategyRegistrar.Selector.ChangeExpBeta.toString() );
		stratSets.setProbability(1.);
		config.strategy().addStrategySettings(stratSets);
		
		config.timeAllocationMutator().setMutationRange(7200.);
		
		config.vspExperimental().setRemovingUnneccessaryPlanAttributes(true);
		config.vspExperimental().setActivityDurationInterpretation( ActivityDurationInterpretation.tryEndTimeThenDuration );
		
		config.vspExperimental().addParam( vspDefaultsCheckingLevel, VspExperimentalConfigGroup.WARN );
		
		config.addConfigConsistencyChecker(new VspConfigConsistencyCheckerImpl());
		config.checkConsistency(); 
	
		// === SCENARIO: ===
		Scenario scenario = ScenarioUtils.createScenario(config) ;
	
		Population pop = scenario.getPopulation() ;
		PopulationFactory pf = pop.getFactory() ;
	
		Person person = pf.createPerson(new IdImpl(1)) ;
		pop.addPerson(person); 
	
		Plan plan = pf.createPlan() ;
		person.addPlan(plan) ;
	
		Activity act = pf.createActivityFromCoord("h", new CoordImpl(0.,0.) ) ;
		plan.addActivity(act); 
	
		// === CONTROLER INFRASTRUCTURE (without actually running the controler): ===
		TravelTime tt = new FreeSpeedTravelTime() ;
		ScoringFunctionFactory scoringFunctionFactory = ControlerDefaults.createDefaultScoringFunctionFactory(scenario) ;
		EffectiveMarginalUtilitiesContainer muc = RouterUtils.createMarginalUtilitiesContrainer(scenario, scoringFunctionFactory) ;
		
		Assert.assertEquals(-12.0/3600., muc.getEffectiveMarginalUtilityOfTravelTime().get(person), 0.01 ) ;

		Assert.assertEquals(marginalUtilityOfMoneyCONFIG, muc.getMarginalUtilityOfMoney().get(person), 0.0001 ) ;

		Assert.assertEquals(monetaryDistanceCostRateCarCONFIG*marginalUtilityOfMoneyCONFIG, muc.getMarginalUtilityOfDistance().get(person) , 0.001) ;
		
//		TravelDisutility td = new PersonIndividualTimeDistanceDisutility(tt, muc ) ;
	}


}
