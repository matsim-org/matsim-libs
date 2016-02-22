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
package playground.agarwalamit.mixedTraffic.patnaIndia.evac;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.evacuation.scenariogenerator.EvacuationNetworkGenerator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class EvacuationPatnaScenarioGenerator {

	private final String dir = "../../../repos/runs-svn/patnaIndia/";

	private final String networkFile = dir+"/inputs/networkUniModal.xml";
	private final String outNetworkFile = dir+"/run105/input/evac_network.xml.gz";

	private final String popFile = dir+"/run105/input/patna_evac_plans_100Pct.xml.gz";
	private final String outPopFile = dir+"/run105/input/patna_evac_plans_100Pct_filtered.xml.gz";
	
	private final String outConfigFile = dir+"/run105/input/patna_evac_config.xml.gz";

	private final String areShapeFile = dir+"/run105/input/area_epsg24345.shp";
	private final Id<Link> safeLinkId = Id.createLinkId("safeLink_Patna");

	private Scenario scenario;
	private Geometry evavcuationArea;

	public static void main(String[] args) {
		new EvacuationPatnaScenarioGenerator().run();
	}

	void run(){
		scenario =  ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		createEvacNetwork();
		scenario.getConfig().network().setInputFile(outNetworkFile);

		// population
		ScenarioUtils.loadScenario(scenario);
		createEvacPopulation();
		scenario.getConfig().plans().setInputFile(outPopFile);
		
		createConfig();
	}
	
	private void createConfig(){
		Config config = scenario.getConfig();
		config.network().setInputFile(outNetworkFile);
		config.plans().setInputFile(outPopFile);
		config.controler().setOutputDirectory(dir+"/run105/100pct/");

		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(100);
		config.controler().setMobsim("qsim");
		config.controler().setWriteEventsInterval(20);
		config.controler().setWritePlansInterval(20);

		config.global().setCoordinateSystem("EPSG:24345");
		config.travelTimeCalculator().setTraveltimeBinSize(900);

		config.qsim().setSnapshotPeriod(5*60);
		config.qsim().setEndTime(30*3600);
		config.qsim().setStuckTime(100000);
		config.qsim().setLinkDynamics(LinkDynamics.PassingQ.name());
		config.qsim().setMainModes(PatnaUtils.URBAN_MAIN_MODES);
		config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);

		StrategySettings expChangeBeta = new StrategySettings();
		expChangeBeta.setStrategyName("ChangeExpBeta");
		expChangeBeta.setWeight(0.9);

		StrategySettings reRoute = new StrategySettings();
		reRoute.setStrategyName("ReRoute");
		reRoute.setWeight(0.1);

		config.strategy().setMaxAgentPlanMemorySize(5);
		config.strategy().addStrategySettings(expChangeBeta);
		config.strategy().addStrategySettings(reRoute);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.75);

		//vsp default
		config.vspExperimental().addParam("vspDefaultsCheckingLevel", "abort");
		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.setParam("TimeAllocationMutator", "mutationAffectsDuration", "false");
		config.setParam("TimeAllocationMutator", "mutationRange", "7200.0");
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration);
		//vsp default

		ActivityParams homeAct = new ActivityParams("home");
		homeAct.setTypicalDuration(1*3600);
		config.planCalcScore().addActivityParams(homeAct);

		ActivityParams evacAct = new ActivityParams("evac");
		evacAct.setTypicalDuration(1*3600);
		config.planCalcScore().addActivityParams(evacAct);

		config.plansCalcRoute().setNetworkModes(PatnaUtils.URBAN_MAIN_MODES);
		
		{
			ModeRoutingParams mrp = new ModeRoutingParams("walk");
			mrp.setTeleportedModeSpeed(4./3.6);
			mrp.setBeelineDistanceFactor(1.0);
			config.plansCalcRoute().addModeRoutingParams(mrp);
		}
		{
			ModeRoutingParams mrp = new ModeRoutingParams("pt");
			mrp.setTeleportedModeSpeed(20./3.6);
			mrp.setBeelineDistanceFactor(1.3);
			config.plansCalcRoute().addModeRoutingParams(mrp);
		}
		new ConfigWriter(config).write(outConfigFile);
	}

	private Scenario createEvacNetwork(){
		Scenario sc = LoadMyScenarios.loadScenarioFromPlansAndNetwork(popFile, networkFile);
		//read shape file and get area
		ShapeFileReader reader = new ShapeFileReader();
		Collection<SimpleFeature> features = reader.readFileAndInitialize(areShapeFile);
		evavcuationArea = (Geometry) features.iterator().next().getDefaultGeometry();

		// will create a network connecting with safe node.
		EvacuationNetworkGenerator net = new EvacuationNetworkGenerator(sc, evavcuationArea, safeLinkId);
		net.run();

		new NetworkWriter(sc.getNetwork()).write(outNetworkFile);
		return sc;
	}

	private void createEvacPopulation() {
		// population, (home - evac)
		Population popOut = scenario.getPopulation();
		PopulationFactory popFact = popOut.getFactory();

		Scenario scIn = LoadMyScenarios.loadScenarioFromPlans(popFile);
		
		for(Person p : scIn.getPopulation().getPersons().values()){

			PlanElement actPe = p.getSelectedPlan().getPlanElements().get(0); // first plan element is of activity
			Activity home = popFact.createActivityFromLinkId(((Activity)actPe).getType(), ((Activity)actPe).getLinkId());

			//check if the person is in the area shape, if not leave them out
			Coord actCoord = ((Activity)actPe).getCoord();
			if(actCoord!=null && !evavcuationArea.contains(MGC.coord2Point(actCoord)) ){
				continue;
			}

			// also exclude any home activity starting on link which is not included in evac network
			if(! scenario.getNetwork().getLinks().containsKey(home.getLinkId())){
				continue;
			}

			Person pOut = popFact.createPerson(p.getId());
			Plan planOut = popFact.createPlan();
			pOut.addPlan(planOut);

			planOut.addActivity(home);
			home.setEndTime(9*3600);

			PlanElement legPe = p.getSelectedPlan().getPlanElements().get(1);
			Leg leg = popFact.createLeg(((Leg)legPe).getMode());
			planOut.addLeg(leg);

			Activity evacAct = popFact.createActivityFromLinkId("evac", safeLinkId);
			planOut.addActivity(evacAct);

			if(PatnaUtils.URBAN_MAIN_MODES.contains(leg.getMode())){
				TripRouter router = new TripRouter();
				router.setRoutingModule(leg.getMode(), DefaultRoutingModules.createPureNetworkRouter(leg.getMode(), popFact, scenario.getNetwork(), new Dijkstra(scenario.getNetwork(), new OnlyTimeDependentTravelDisutility(new FreeSpeedTravelTime()) , new FreeSpeedTravelTime())));
				List<? extends PlanElement> routeInfo = router.calcRoute(leg.getMode(), new ActivityWrapperFacility(home), new ActivityWrapperFacility(evacAct), home.getEndTime(), pOut);

				Route route = ((Leg)routeInfo.get(0)).getRoute();
				route.setStartLinkId(home.getLinkId());
				route.setEndLinkId(evacAct.getLinkId());

				leg.setRoute(route);
				leg.setTravelTime(((Leg)routeInfo.get(0)).getTravelTime());

			} else {
				continue;
				//probably, re-create home and evac activities with coord here to include them in simulation.
			//				ModeRouteFactory routeFactory = new ModeRouteFactory();
			//				routeFactory.setRouteFactory(leg.getMode(), new GenericRouteFactory());
			//				
			//				TripRouter router = new TripRouter();
			//				router.setRoutingModule(leg.getMode(), DefaultRoutingModules.createTeleportationRouter(leg.getMode(), popFact, scOut.getConfig().plansCalcRoute().getModeRoutingParams().get(leg.getMode())));
			//				List<? extends PlanElement> routeInfo = router.calcRoute(leg.getMode(), new ActivityWrapperFacility(home), new ActivityWrapperFacility(evacAct), home.getEndTime(), pOut);
			//				
			//				Route route = ((Leg)routeInfo.get(0)).getRoute();
			////				Route route = routeFactory.createRoute(leg.getMode(), home.getLinkId(), evacAct.getLinkId());
			//				leg.setRoute(route);
			//				leg.setTravelTime(((Leg)routeInfo.get(0)).getTravelTime());
			}
			popOut.addPerson(pOut);
		}
		new PopulationWriter(popOut).write(outPopFile);		
	}
}