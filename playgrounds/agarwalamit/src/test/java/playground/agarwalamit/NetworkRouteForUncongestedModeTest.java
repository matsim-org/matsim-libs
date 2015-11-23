/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author amit
 */

public class NetworkRouteForUncongestedModeTest {

	@Rule public MatsimTestUtils helper = new MatsimTestUtils();

	final static String EQUIL_NETWORK = "../../matsim/examples/equil/network.xml";
	final static String CONFIG = "../../matsim/examples/tutorial/programming/MultipleSubpopulations/config.xml";

	/**
	 * Every link must allow car and ride mode if networkModes are car and ride. 
	 * Using overriding modules to get network route for ride mode.  
	 */
	@Test
	public void testWithAllowedModesOnLink(){

		String OUTPUT = helper.getOutputDirectory();

		Scenario sc = createSceanrio();
		sc.getConfig().controler().setOutputDirectory(OUTPUT);

		// set allowed modes on each link
		for (Link l : sc.getNetwork().getLinks().values()) {
			Set<String> modes = new HashSet<>(Arrays.asList("car","ride"));
			l.setAllowedModes(modes);
		}
		
		Controler controler = new Controler(sc);
		controler.getConfig().controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
		
		//overriding module to get network route for ride mode
		controler.addOverridingModule(new AbstractModule() { 
			@Override
			public void install() {
				addTravelTimeBinding("ride").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("ride").to(carTravelDisutilityFactoryKey());				
			}
		});
		controler.run();
	}

	private Scenario createSceanrio () {
		Config config = ConfigUtils.createConfig();

		config.network().setInputFile(EQUIL_NETWORK);
		config.controler().setLastIteration(1);

		{
			ActivityParams ap = new ActivityParams();
			ap.setActivityType("h");
			ap.setTypicalDuration(12*3600);
			config.planCalcScore().addActivityParams(ap);
		}
		{
			ActivityParams ap = new ActivityParams();
			ap.setActivityType("w");
			ap.setTypicalDuration(8*3600);
			config.planCalcScore().addActivityParams(ap);
		}
		config.qsim().setMainModes(Arrays.asList("car"));
		config.plansCalcRoute().setNetworkModes(Arrays.asList("car","ride"));

		config.plansCalcRoute().getOrCreateModeRoutingParams("PT").setTeleportedModeSpeed (20/3.6);

		{
			StrategySettings reRoute = new StrategySettings();
			reRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.toString());
			reRoute.setWeight(0.2);
			config.strategy().addStrategySettings(reRoute);

			StrategySettings changeExpBetaStrategySettings = new StrategySettings();
			changeExpBetaStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
			changeExpBetaStrategySettings.setWeight(0.8);
			config.strategy().addStrategySettings(changeExpBetaStrategySettings);
		}

		Scenario sc = ScenarioUtils.loadScenario(config);

		//create plans with car and ride mode
		for ( int ii = 1; ii<5; ii++) {
			Id<Person> personId = Id.createPersonId(ii);
			Person p = sc.getPopulation().getFactory().createPerson(personId);
			Plan plan = sc.getPopulation().getFactory().createPlan();
			p.addPlan(plan);

			Activity home = sc.getPopulation().getFactory().createActivityFromLinkId("h", Id.createLinkId("1"));
			home.setEndTime(06*3600);
			Leg leg ;

			if ( ii%2==0 ) leg = sc.getPopulation().getFactory().createLeg("car");
			else leg = sc.getPopulation().getFactory().createLeg("ride");

			plan.addActivity(home);
			plan.addLeg(leg);

			Activity work = sc.getPopulation().getFactory().createActivityFromLinkId("w", Id.createLinkId("20"));
			work.setEndTime(16*3600);

			plan.addActivity(work);
			plan.addLeg(leg);

			sc.getPopulation().addPerson(p);
		}
		return sc;
	}
}
