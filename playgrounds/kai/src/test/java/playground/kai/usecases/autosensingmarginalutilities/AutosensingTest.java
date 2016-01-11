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
package playground.kai.usecases.autosensingmarginalutilities;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.VspConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class AutosensingTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
    
	@SuppressWarnings("static-method")
	@Test
	public final void testOne() {
		// === CONFIG: ===
		Config config = ConfigUtils.createConfig() ;

		ActivityParams params = new ActivityParams("h") ;
		params.setTypicalDuration(12.*3600.);
		config.planCalcScore().addActivityParams(params);
		
		final double monetaryDistanceCostRateCarCONFIG = -0.21/1000.;
		config.planCalcScore().getModes().get(TransportMode.car).setMonetaryDistanceRate(monetaryDistanceCostRateCarCONFIG);

		final double marginalUtilityOfMoneyCONFIG = 2. ;
		config.planCalcScore().setMarginalUtilityOfMoney(marginalUtilityOfMoneyCONFIG);
		
		StrategySettings stratSets = new StrategySettings( Id.create(1, StrategySettings.class) ) ;
		stratSets.setStrategyName( DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString() );
		stratSets.setWeight(1.);
		config.strategy().addStrategySettings(stratSets);
		
		config.timeAllocationMutator().setMutationRange(7200.);
		
		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.plans().setActivityDurationInterpretation( PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );
		
		config.vspExperimental().setVspDefaultsCheckingLevel( VspDefaultsCheckingLevel.warn );
		
		config.addConfigConsistencyChecker(new VspConfigConsistencyCheckerImpl());
		config.checkConsistency(); 
	
		// === SCENARIO: ===
		Scenario scenario = ScenarioUtils.createScenario(config) ;
	
		Population pop = scenario.getPopulation() ;
		PopulationFactory pf = pop.getFactory() ;
	
		Person person = pf.createPerson(Id.create(1, Person.class)) ;
		pop.addPerson(person); 
	
		Plan plan = pf.createPlan() ;
		person.addPlan(plan) ;

		Activity act = pf.createActivityFromCoord("h", new Coord(0., 0.)) ;
		plan.addActivity(act); 
	
		// === CONTROLER INFRASTRUCTURE (without actually running the controler): ===
//		TravelTime tt = new FreeSpeedTravelTime() ;
		ScoringFunctionFactory scoringFunctionFactory = ControlerDefaults.createDefaultScoringFunctionFactory(scenario) ;
		EffectiveMarginalUtilitiesContainer muc = TravelDisutilityUtils.createAutoSensingMarginalUtilitiesContainer(scenario, scoringFunctionFactory) ;
		
		Assert.assertEquals(-12.0/3600., muc.getEffectiveMarginalUtilityOfTtime(person.getId()), 0.01/3600. ) ;
		Assert.assertTrue(muc.getEffectiveMarginalUtilityOfTtime(person.getId()) < 0. );
		// (the first condition is not very precise ... since the autosensing is not that exact.  So the second condition tests
		// in addition for the exact sign.)

		Assert.assertEquals(marginalUtilityOfMoneyCONFIG, muc.getMarginalUtilityOfMoney(person.getId()), 1.e-15 ) ;

		Assert.assertEquals(monetaryDistanceCostRateCarCONFIG*marginalUtilityOfMoneyCONFIG, 
				muc.getMarginalUtilityOfDistance(person.getId()) , 1.e-15 ) ;
		// (for the time being, the utilities of distance are linear in the distance, thus autosensing is exact.)
		
//		TravelDisutility td = new PersonIndividualTimeDistanceDisutility(tt, muc ) ;
	}


}
