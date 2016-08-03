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
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.input.combined.router.BikeTimeDistanceTravelDisutilityFactory;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.combined.router.FreeSpeedTravelTimeForBike;
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
		
		String mode = "bike";
		String net = PatnaUtils.INPUT_FILES_DIR + "/simulationInputs/network/shpNetwork/bikeTrack.xml.gz";
		String outputDir = "../../../../repos/runs-svn/patnaIndia/run108/jointDemand/policies/testBikeTrack/";
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(net);
		
		//== allowing car on home and work activity locations
		
		//==
		Scenario sc = ScenarioUtils.loadScenario(config);
		
		PopulationFactory popFact = sc.getPopulation().getFactory();
		Person p = popFact.createPerson(Id.createPersonId("1"));
		Plan plan = popFact.createPlan();
		
		Link l = sc.getNetwork().getLinks().get(Id.createLinkId("256501411_link178"));
		
		Set<String> modes = new HashSet<>();
		modes.add("car");
		modes.add("bike");
		modes.add("bike_ext");
		l.setAllowedModes(modes );
		sc.getNetwork().addLink(l);
		
		Activity home = popFact.createActivityFromLinkId("home", l.getId());
		home.setEndTime(9.*3600.);
		plan.addActivity(home);
		
		Leg leg = popFact.createLeg(mode);

		l = sc.getNetwork().getLinks().get(Id.createLinkId("97952992_link21"));
		Activity work = popFact.createActivityFromLinkId("work",l.getId());
		
//		TripRouter router = new TripRouter();
//		router.setRoutingModule(
//				leg.getMode(), 
//				DefaultRoutingModules.createPureNetworkRouter(
//						leg.getMode(), 
//						popFact, 
//						sc.getNetwork(), 
//						new Dijkstra( sc.getNetwork(), 
//								new OnlyTimeDependentTravelDisutility(new FreeSpeedTravelTime()), 
//								new FreeSpeedTravelTime())
//						)
//				);
//		List<? extends PlanElement> routeInfo = router.calcRoute(
//				leg.getMode(), 
//				new ActivityWrapperFacility(home), 
//				new ActivityWrapperFacility(work), 
//				home.getEndTime(), 
//				p);
//
//		Route route = ((Leg)routeInfo.get(0)).getRoute();
//		route.setStartLinkId(home.getLinkId());
//		route.setEndLinkId(work.getLinkId());
//
//		leg.setRoute(route);
//		leg.setTravelTime(((Leg)routeInfo.get(0)).getTravelTime());

		plan.addLeg(leg);
		
		plan.addActivity(work);
		p.addPlan(plan);
		
		sc.getPopulation().addPerson(p);
		
		ActivityParams workAct = new ActivityParams("work");
		workAct.setTypicalDuration(8*3600);
		sc.getConfig().planCalcScore().addActivityParams(workAct);

		ActivityParams homeAct = new ActivityParams("home");
		homeAct.setTypicalDuration(12*3600);
		sc.getConfig().planCalcScore().addActivityParams(homeAct);
		
		sc.getConfig().qsim().setMainModes(Arrays.asList(mode));
		
		ModeRoutingParams mrp = new ModeRoutingParams("walk");
		mrp.setTeleportedModeSpeed(5./3.6);
		mrp.setBeelineDistanceFactor(1.5);
		sc.getConfig().plansCalcRoute().addModeRoutingParams(mrp);
		
		sc.getConfig().controler().setLastIteration(0);
		sc.getConfig().controler().setOutputDirectory(outputDir);
		sc.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		ModeParams modeParam = new ModeParams(mode);
		modeParam.setConstant(0.);
		sc.getConfig().planCalcScore().addModeParams(modeParam);
		
		
		sc.getConfig().plansCalcRoute().setNetworkModes(Arrays.asList(mode));

		Controler controler = new Controler(sc);
		
		final BikeTimeDistanceTravelDisutilityFactory builder_bike =  new BikeTimeDistanceTravelDisutilityFactory(mode, config.planCalcScore());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding(mode).to(FreeSpeedTravelTimeForBike.class);
				addTravelDisutilityFactoryBinding(mode).toInstance(builder_bike);
			}
		});
		controler.run();
	}
}