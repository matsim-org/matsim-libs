/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.policies;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.router.BikeTimeDistanceTravelDisutilityFactory;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;

/**
 * Trying to check if an agent is routed on multi-mode network.
 * 
 * a) only bike track
 * b) bike track with one car link
 * 
 * @author amit
 */

public class PatnaBikeTestControler {

	public static void main(String[] args) {

		String net = PatnaUtils.INPUT_FILES_DIR + "/simulationInputs/network/shpNetwork/bikeTrack.xml.gz";
		String outputDir = "../../../../repos/runs-svn/patnaIndia/run108/jointDemand/policies/testBikeTrack/";

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(net);
		config.vspExperimental().setWritingOutputEvents(true);
		
		Set<String> modes = new HashSet<>();
		modes.add("car");
		modes.add("bike");

		Scenario sc = ScenarioUtils.loadScenario(config);
		
		Link l_home = sc.getNetwork().getLinks().get(Id.createLinkId("256501411_link179"));

		Link l_intermediate_car = sc.getNetwork().getLinks().get(Id.createLinkId("256501411_link177"));
		Link l_work_car = sc.getNetwork().getLinks().get(Id.createLinkId("256501411_link175"));
		
		Link l_work_bike = sc.getNetwork().getLinks().get(Id.createLinkId("97952992_link20"));
		
		l_home.setAllowedModes(modes);
		l_intermediate_car.setAllowedModes(modes);
		l_work_car.setAllowedModes(modes);
		
		for(int i=0;i<2;i++){
			
			String mode = i%2==0 ? "bike" : "car";
			
			PopulationFactory popFact = sc.getPopulation().getFactory();
			Person p = popFact.createPerson(Id.createPersonId(i));
			Plan plan = popFact.createPlan();

			Activity home = popFact.createActivityFromLinkId("home", l_home.getId());
			home.setEndTime(9.*3600.+5*i);
			plan.addActivity(home);

			Leg leg = popFact.createLeg(mode);
			
			Link l =  null;
			if ( mode.equals("car") ) l = l_work_car;	
			else	l = l_work_bike;

			Activity work = popFact.createActivityFromLinkId("work",l.getId());

			plan.addLeg(leg);

			plan.addActivity(work);
			p.addPlan(plan);

			sc.getPopulation().addPerson(p);
		}

		ActivityParams workAct = new ActivityParams("work");
		workAct.setTypicalDuration(8*3600);
		sc.getConfig().planCalcScore().addActivityParams(workAct);

		ActivityParams homeAct = new ActivityParams("home");
		homeAct.setTypicalDuration(12*3600);
		sc.getConfig().planCalcScore().addActivityParams(homeAct);

		List<String> mainModes = Arrays.asList("car","bike");
		sc.getConfig().qsim().setMainModes(mainModes);

		ModeRoutingParams mrp = new ModeRoutingParams("walk");
		mrp.setTeleportedModeSpeed(5./3.6);
		mrp.setBeelineDistanceFactor(1.5);
		sc.getConfig().plansCalcRoute().addModeRoutingParams(mrp);

		sc.getConfig().controler().setLastIteration(10);
		sc.getConfig().controler().setOutputDirectory(outputDir);
		sc.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		ModeParams modeParam = new ModeParams("bike");
		modeParam.setConstant(0.);
		sc.getConfig().planCalcScore().addModeParams(modeParam);

		sc.getConfig().plansCalcRoute().setNetworkModes(mainModes);
		
		StrategySettings expChangeBeta = new StrategySettings();
		expChangeBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.name());
		expChangeBeta.setWeight(0.7);
		sc.getConfig().strategy().addStrategySettings(expChangeBeta);

		StrategySettings reRoute = new StrategySettings();
		reRoute.setStrategyName(DefaultStrategy.ReRoute.name());
		reRoute.setWeight(0.3);
		sc.getConfig().strategy().addStrategySettings(reRoute);
		sc.getConfig().strategy().setFractionOfIterationsToDisableInnovation(0.8);

		Controler controler = new Controler(sc);

		final BikeTimeDistanceTravelDisutilityFactory builder_bike =  new BikeTimeDistanceTravelDisutilityFactory("bike", config.planCalcScore());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding("bike").to(FreeSpeedTravelTimeForBike.class);
				addTravelDisutilityFactoryBinding("bike").toInstance(builder_bike);
			}
		});
		controler.run();
	}
}