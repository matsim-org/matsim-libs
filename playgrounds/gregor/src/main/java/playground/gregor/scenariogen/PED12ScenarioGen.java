/* *********************************************************************** *
 * project: org.matsim.*
 * PED12ScenarioGen.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.scenariogen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.SignalSystemsConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.signalsystems.SignalUtils;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioWriter;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalControlData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signalsystems.model.DefaultPlanbasedSignalSystemController;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

import playground.gregor.sim2d_v3.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v3.helper.gisdebug.GisDebugger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class PED12ScenarioGen {

	private static final double EPSILON = 0.0001;
	private static GeometryFactory geofac = new GeometryFactory();
	private static int nodeID = 0;
	private static int linkID = 0;
	private static int persID = 0;
	private static String idMarker = "ped_";
	private static String parkingMarker = "parking_";
	
	/*package*/ enum SC {Helbing,Zanlungo,vanDenBerg};
	/*package*/ static final SC model = SC.Zanlungo;
	
	/*package*/ static List<Link> shoppingShelfs = new ArrayList<Link>();
	/*private*/ static List<Link> cashDesks = new ArrayList<Link>();

	private static List<Link> pedCrossing = new ArrayList<Link>();

	private static List<Link> parkingLots = new ArrayList<Link>();
	private static Link carStart;
	private static Link carTrafficLight;
	private static Link carStop;

	private static Set<String> walkModes = CollectionUtils.stringToSet("walk,walk2d");
	private static Set<String> carModes = CollectionUtils.stringToSet("walk,walk2d,car");
	
	/*package*/ static Id carInNode;
	/*package*/ static Id carOutNode;
	/*package*/ static Id walkSubwayInNode;
	/*package*/ static Id walkInNode;
	
	public static void main(String [] args) {
//		String scDir = "/home/cdobler/workspace/matsim/mysimulations/ped2012/";
		String scDir = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/";
		String inputDir = scDir + "input_2d";
//		String scDir = "/Users/laemmel/devel/ped12_dobLaem/";
//		String inputDir = scDir + "/input/";

		Config c = ConfigUtils.createConfig();
		c.scenario().setUseSignalSystems(true);
		Scenario sc = ScenarioUtils.createScenario(c);

		createAndSaveEnvironment(inputDir);

		createAndSaveNetwork(sc,inputDir);

		createAndSavePopulation(sc,inputDir);

		new NetworkWriter(sc.getNetwork()).write(inputDir+"/network.xml");
		new PopulationWriter(sc.getPopulation(), sc.getNetwork()).writeV5(inputDir+"/plans.xml");
		
		c.plans().setInputFile(inputDir+"/plans.xml");
		c.network().setInputFile(inputDir+"/network.xml");

		c.controler().setLastIteration(0);
		c.controler().setOutputDirectory(scDir + "output/");
		c.controler().setMobsim("hybridQ2D");

		c.strategy().setMaxAgentPlanMemorySize(3);

		c.strategy().addParam("maxAgentPlanMemorySize", "3");
		c.strategy().addParam("Module_1", "ReRoute");
		c.strategy().addParam("ModuleProbability_1", "0.1");
		c.strategy().addParam("Module_2", "ChangeExpBeta");
		c.strategy().addParam("ModuleProbability_2", "0.9");
		//
		Sim2DConfigGroup s2d = new Sim2DConfigGroup();
		s2d.setFloorShapeFile(inputDir +"/environment.shp");

		if (model == SC.Helbing) {
			s2d.setEnableCircularAgentInterActionModule("true");
			s2d.setEnableEnvironmentForceModule("true");
			s2d.setEnableCollisionPredictionAgentInteractionModule("false");
			s2d.setEnableCollisionPredictionEnvironmentForceModule("false");
			s2d.setEnablePathForceModule("true");
			s2d.setEnableDrivingForceModule("true");
			s2d.setEnableVelocityObstacleModule("false");
			s2d.setEnablePhysicalEnvironmentForceModule("false");
		} else if (model == SC.Zanlungo) {
			s2d.setEnableCircularAgentInterActionModule("false");
			s2d.setEnableEnvironmentForceModule("false");
			s2d.setEnableCollisionPredictionAgentInteractionModule("true");
			s2d.setEnableCollisionPredictionEnvironmentForceModule("true");
			s2d.setEnablePathForceModule("true");
			s2d.setEnableDrivingForceModule("true");
			s2d.setEnableVelocityObstacleModule("false");
			s2d.setEnablePhysicalEnvironmentForceModule("false");			
		} else if (model == SC.vanDenBerg) {
			s2d.setEnableCircularAgentInterActionModule("false");
			s2d.setEnableEnvironmentForceModule("false");
			s2d.setEnableCollisionPredictionAgentInteractionModule("false");
			s2d.setEnableCollisionPredictionEnvironmentForceModule("false");
			s2d.setEnablePathForceModule("false");
			s2d.setEnableDrivingForceModule("false");
			s2d.setEnableVelocityObstacleModule("true");
			s2d.setEnablePhysicalEnvironmentForceModule("false");			
		}
		s2d.setEnableMentalLinkSwitch("true");

		
//		//Helbing
//		s2d.setEnableCircularAgentInterActionModule("true");
//		s2d.setEnableEnvironmentForceModule("true");
//		s2d.setEnableCollisionPredictionAgentInteractionModule("false");
//		s2d.setEnableCollisionPredictionEnvironmentForceModule("false");
//		s2d.setEnablePathForceModule("true");
//		s2d.setEnableDrivingForceModule("true");
//		s2d.setEnableVelocityObstacleModule("false");
//		s2d.setEnablePhysicalEnvironmentForceModule("false");
//		s2d.setEnableMentalLinkSwitch("true");

		//		s2d.setTimeStepSize(""+0.1);
		QSimConfigGroup qsim = new QSimConfigGroup();
		qsim.setEndTime(86400);
		qsim.setRemoveStuckVehicles(false);
		qsim.setNumberOfThreads(2);
		//				qsim.setTimeStepSize(1./25.);
		c.addModule(qsim);

		c.addModule(s2d);

		ActivityParams pre = new ActivityParams("h");
		pre.setTypicalDuration(49); // needs to be geq 49, otherwise when running a simulation one gets "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in ActivityUtilityParameters.java (gl)
		pre.setMinimalDuration(49);
		pre.setClosingTime(49);
		pre.setEarliestEndTime(49);
		pre.setLatestStartTime(49);
		pre.setOpeningTime(49);

		sc.getConfig().planCalcScore().addActivityParams(pre);

		createTrafficLights(sc,inputDir);

		new ConfigWriter(c).write(inputDir + "/config.xml");
	}

	private static void createTrafficLights(Scenario sc, String inputDir) {
		SignalsData signalsData = sc.getScenarioElement(SignalsData.class);
		createSignalSystemsAndGroups(sc, signalsData);
		createSignalControl(sc, signalsData);

		//write to file
		SignalsScenarioWriter signalsWriter = new SignalsScenarioWriter();
		String signalSystemsFile = inputDir + "/signal_systems.xml";
		signalsWriter.setSignalSystemsOutputFilename(signalSystemsFile);
		String signalGroupsFile = inputDir + "/signal_groups.xml";
		signalsWriter.setSignalGroupsOutputFilename(signalGroupsFile);

		String signalControlFile = inputDir + "/signal_control.xml";
		signalsWriter.setSignalControlOutputFilename(signalControlFile);
		signalsWriter.writeSignalsData(signalsData);

		Config c = sc.getConfig();
		SignalSystemsConfigGroup signalsConfig = c.signalSystems();
		signalsConfig.setSignalSystemFile(signalSystemsFile);
		signalsConfig.setSignalGroupsFile(signalGroupsFile);
		signalsConfig.setSignalControlFile(signalControlFile);
	}
	
	private static void createSignalControl(Scenario scenario, SignalsData sd) {
		int cycle = 10;
		SignalControlData control = sd.getSignalControlData();

		//signal system 3, 4 control
		for (Link l  : cashDesks) {
			Id id = l.getId();
			SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(id);
			control.addSignalSystemControllerData(controller);
			controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			SignalPlanData plan = control.getFactory().createSignalPlanData(id);
			controller.addSignalPlanData(plan);
			plan.setCycleTime(cycle);
			plan.setOffset(MatsimRandom.getRandom().nextInt(10));
			SignalGroupSettingsData settings1 = control.getFactory().createSignalGroupSettingsData(id);
			plan.addSignalGroupSettings(settings1);
			settings1.setOnset(8);
			settings1.setDropping(9);
		}

		for (Link l  : pedCrossing) {
			Id id = l.getId();
			SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(id);
			control.addSignalSystemControllerData(controller);
			controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			SignalPlanData plan = control.getFactory().createSignalPlanData(id);
			controller.addSignalPlanData(plan);
			plan.setCycleTime(60);
			plan.setOffset(0);
			SignalGroupSettingsData settings1 = control.getFactory().createSignalGroupSettingsData(id);
			plan.addSignalGroupSettings(settings1);
			settings1.setOnset(0);
			settings1.setDropping(20);
		}
		
		Id id = carTrafficLight.getId();
		SignalSystemControllerData controller = control.getFactory().createSignalSystemControllerData(id);
		control.addSignalSystemControllerData(controller);
		controller.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
		SignalPlanData plan = control.getFactory().createSignalPlanData(id);
		controller.addSignalPlanData(plan);
		plan.setCycleTime(60);
		plan.setOffset(0);
		SignalGroupSettingsData settings1 = control.getFactory().createSignalGroupSettingsData(id);
		plan.addSignalGroupSettings(settings1);
		settings1.setOnset(30);
		settings1.setDropping(55);

	}
	private static void createSignalSystemsAndGroups(Scenario scenario,
			SignalsData signalsData) {

		SignalSystemsData systems = signalsData.getSignalSystemsData();
		SignalGroupsData groups = signalsData.getSignalGroupsData();

		for (Link l : cashDesks) {

			Id id = l.getId();
			SignalSystemData sys = systems.getFactory().createSignalSystemData(id);
			systems.addSignalSystemData(sys);
			SignalData signal = systems.getFactory().createSignalData(id);
			sys.addSignalData(signal);
			signal.setLinkId(l.getId());
			SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
		}

		for (Link l : pedCrossing) {

			Id id = l.getId();
			SignalSystemData sys = systems.getFactory().createSignalSystemData(id);
			systems.addSignalSystemData(sys);
			SignalData signal = systems.getFactory().createSignalData(id);
			sys.addSignalData(signal);
			signal.setLinkId(l.getId());
			SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
		}
		
		Id id = carTrafficLight.getId();
		SignalSystemData sys = systems.getFactory().createSignalSystemData(id);
		systems.addSignalSystemData(sys);
		SignalData signal = systems.getFactory().createSignalData(id);
		sys.addSignalData(signal);
		signal.setLinkId(carTrafficLight.getId());
		SignalUtils.createAndAddSignalGroups4Signals(groups, sys);
	}

	private static void createAndSavePopulation(Scenario sc, String inputDir) {

		Network network = sc.getNetwork();
		FreeSpeedTravelTime fs = new FreeSpeedTravelTime();
		TravelDisutility cost = new TravelCostCalculatorFactoryImpl().createTravelDisutility(fs,sc.getConfig().planCalcScore());
		Dijkstra dijkstra = new Dijkstra(network, cost, fs);
		Coord coord;
		CoordinateTransformer transformer = new CoordinateTransformer(dx, dy, da);
		
		//subway
		coord = transformer.transformCoord(9, 1);
		Node n = ((NetworkImpl)sc.getNetwork()).getNearestNode(coord);
		Link start = n.getOutLinks().values().iterator().next();
		Link end = n.getInLinks().values().iterator().next();

		double depStartTime = 0;
		double depEndTime = 180;
		double platformWidth = 2;
		double maxPersPerSecond = platformWidth * 1.33; //weidmann

		for (double time = depStartTime; time <= depEndTime; time++) {
			double personsThisTimeStep = maxPersPerSecond * MatsimRandom.getRandom().nextDouble();
			int numPers = (int)(personsThisTimeStep+0.5);
			for (int p = 0; p < numPers; p++) {
				double persTime = time + MatsimRandom.getRandom().nextDouble();
				double x = -2 + 3*(MatsimRandom.getRandom().nextDouble()-0.5);
				double y = 1;
				Coord actCoord = coord = transformer.transformCoord(x, y);
				createSubwayPerson(persTime,start,end,actCoord,sc,dijkstra);
			}
		}
		
		double time = depStartTime;
		for (Link parkingLot : parkingLots) {
			createCarPerson(time, sc, dijkstra, parkingLot);
			
			// every 30 to 90 seconds a new car
			time += 30 + MatsimRandom.getRandom().nextInt(60);
		}
	}

	private static void createSubwayPerson(double persTime, Link start,
			Link end, Coord actCoord, Scenario sc, Dijkstra dijkstra) {

		PopulationFactory pb = sc.getPopulation().getFactory();
		Id id = sc.createId(Integer.toString(persID++));
		Person pers = pb.createPerson(id);
		Plan plan = pb.createPlan();
		ActivityImpl act = (ActivityImpl) pb.createActivityFromLinkId("h", start.getId());
		act.setCoord(actCoord);
		act.setEndTime(persTime);
		plan.addActivity(act);
		Leg leg = pb.createLeg("walk2d");

		dijkstra.setModeRestriction(walkModes);
		Route route = createRandomShoppingRoute(start,end,dijkstra);
		leg.setRoute(route);
		dijkstra.setModeRestriction(null);

		plan.addLeg(leg);
		Activity act2 = pb.createActivityFromLinkId("h",end.getId());
		act2.setEndTime(0);
		plan.addActivity(act2);
		pers.addPlan(plan);

		sc.getPopulation().addPerson(pers);
	}

	private static void createCarPerson(double time, Scenario sc,
			Dijkstra dijkstra, Link parkingLot) {

		PopulationFactory pb = sc.getPopulation().getFactory();
		Id id = sc.createId(Integer.toString(persID++));
		Person pers = pb.createPerson(id);
		Plan plan = pb.createPlan();
		ActivityImpl act = (ActivityImpl) pb.createActivityFromLinkId("h", carStart.getId());
		act.setEndTime(time);
		plan.addActivity(act);
		
		Route routeCar = createSimpleCarRoute(carStart,parkingLot,dijkstra);
		Leg legCar = pb.createLeg("car");
		legCar.setRoute(routeCar);
		plan.addLeg(legCar);
		ActivityImpl park = (ActivityImpl) pb.createActivityFromLinkId("h", parkingLot.getId());
		park.setEndTime(0);
		plan.addActivity(park);
		Leg shopping = pb.createLeg("walk2d");
		
		Route route = createRandomShoppingRoute(parkingLot,parkingLot,dijkstra);
		shopping.setRoute(route);

		plan.addLeg(shopping);
		Activity intoTheCar = pb.createActivityFromLinkId("h",parkingLot.getId());
		intoTheCar.setEndTime(0);
		plan.addActivity(intoTheCar);
		
		
		Route routeCar2 = createSimpleCarRoute(parkingLot,carStop,dijkstra);
		Leg driveHome = pb.createLeg("car");
		driveHome.setRoute(routeCar2);
		plan.addLeg(driveHome);
		ActivityImpl done = (ActivityImpl) pb.createActivityFromLinkId("h",carStop.getId());
		plan.addActivity(done);
		pers.addPlan(plan);
		sc.getPopulation().addPerson(pers);
	}

	private static Route createSimpleCarRoute(Link from, Link to, Dijkstra dijkstra) {
		
		dijkstra.setModeRestriction(CollectionUtils.stringToSet(TransportMode.car));
		Path r = dijkstra.calcLeastCostPath(from.getToNode(), to.getFromNode(), 0, null, null);
		
		List<Id> linkIds = new ArrayList<Id>();
		for (Link l : r.links) {
			linkIds.add(l.getId());
		}

		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(from.getId(), to.getId());
		route.setLinkIds(from.getId(), linkIds, to.getId());

		dijkstra.setModeRestriction(null);
		return route;
	}


	private static Route createRandomShoppingRoute(Link start, Link end, Dijkstra dijkstra) {

		dijkstra.setModeRestriction(walkModes);
		
		int stopsAtShoppingShelfs = MatsimRandom.getRandom().nextInt(3)+1; // three stops max

		Link current = start;

		List<Link> links = new ArrayList<Link>();
		for (int stop = 0; stop < stopsAtShoppingShelfs; stop++) {

			Link next = shoppingShelfs.get(MatsimRandom.getRandom().nextInt(shoppingShelfs.size()));
			Node from = current.getToNode();
			Node to = next.getFromNode();

			Path r = dijkstra.calcLeastCostPath(from, to, 0, null, null);
			links.addAll(r.links);
			links.add(next);
			current = next;
		}

		Link cashDesk = cashDesks.get(MatsimRandom.getRandom().nextInt(cashDesks.size()));
		Node from = current.getToNode();
		Node to = cashDesk.getFromNode();

		Path r = dijkstra.calcLeastCostPath(from, to, 0, null, null);
		links.addAll(r.links);
		links.add(cashDesk);
		current = cashDesk;		

		from = current.getToNode();
		to = end.getFromNode();

		r = dijkstra.calcLeastCostPath(from, to, 0,null,null);
		links.addAll(r.links);

		List<Id> linkIds = new ArrayList<Id>();
		for (Link l : links) {
			linkIds.add(l.getId());
		}

		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(start.getId(), end.getId());
		route.setLinkIds(start.getId(), linkIds, end.getId());
		
		dijkstra.setModeRestriction(null);
		return route;
	}

	private static void createAndSaveNetwork(Scenario sc, String inputDir) {

		NetworkImpl net = (NetworkImpl) sc.getNetwork();
		Link l; 
		//path from subway to mall entry

		//stairs
		//subway platform
		l = createPedLink(9.,1.,9.,3.,1.34,false,4,net);
		walkSubwayInNode = l.getFromNode().getId();
		//up VF,h = 0.610 m/s
		createPedLink(9.,3.,9.,12.,0.61,true,4,net);
		//down VF,h = 0.694 m/s
		createPedLink(9.,12.,9.,3.,0.694,true,4,net);

		createPedLink(9.,12.,9.,22.,1.34,false,4,net);
		
		l = createPedLink(9.,22.,-1.,22.,1.34,false,4,net);
		walkInNode = l.getFromNode().getId();
		
		pedCrossing.add(createPedLink(-1.,22.,-3.,22.,1.34,false,4,net));

		// crossing the street
		createPedLink(-3.,22.,-12.,22.,1.34,false,4,net);

		pedCrossing.add(createPedLink(-14.,22.,-12.,22.,1.34,false,4,net));

		createPedLink(-14.,22.,-45.5,22.,1.34,false,4,net);

		//parking lot
		double vParking = 20/3.6;
		//1.row
		parkingLots.add(createParkingLink(-14.,15.,-14.,8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-18.,15.,-18.,8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-22.,15.,-22.,8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-26.,15.,-26.,8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-30.,15.,-30.,8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-34.,15.,-34.,8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-38.,15.,-38.,8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-42.,15.,-42.,8.,vParking,false,4,net));
		//2. row
		parkingLots.add(createParkingLink(-14.,1.,-14.,8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-18.,1.,-18.,8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-22.,1.,-22.,8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-26.,1.,-26.,8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-30.,1.,-30.,8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-34.,1.,-34.,8.,vParking,false,4,net));
		//connections
		createCarLink(-14.,8.,-18.,8.,vParking,false,4,net);
		createCarLink(-18.,8.,-22.,8.,vParking,false,4,net);
		createCarLink(-22.,8.,-26.,8.,vParking,false,4,net);
		createCarLink(-26.,8.,-30.,8.,vParking,false,4,net);
		createCarLink(-30.,8.,-34.,8.,vParking,false,4,net);
		createCarLink(-34.,8.,-38.,8.,vParking,false,4,net);
		createCarLink(-38.,8.,-42.,8.,vParking,false,4,net);
		createCarLink(-42.,8.,-45.5,8.,vParking,false,4,net);
		//3. row
		parkingLots.add(createParkingLink(-14.,-1.,-14.,-8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-18.,-1.,-18.,-8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-22.,-1.,-22.,-8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-26.,-1.,-26.,-8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-30.,-1.,-30.,-8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-34.,-1.,-34.,-8.,vParking,false,4,net));
		//4. row
		parkingLots.add(createParkingLink(-14.,-15.,-14.,-8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-18.,-15.,-18.,-8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-22.,-15.,-22.,-8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-26.,-15.,-26.,-8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-30.,-15.,-30.,-8.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-34.,-15.,-34.,-8.,vParking,false,4,net));
		//connections
		createCarLink(-14.,-8.,-18.,-8.,vParking,false,4,net);
		createCarLink(-18.,-8.,-22.,-8.,vParking,false,4,net);
		createCarLink(-22.,-8.,-26.,-8.,vParking,false,4,net);
		createCarLink(-26.,-8.,-30.,-8.,vParking,false,4,net);
		createCarLink(-30.,-8.,-34.,-8.,vParking,false,4,net);
		createCarLink(-34.,-8.,-45.5,-8.,vParking,false,4,net);	
		//5. row
		parkingLots.add(createParkingLink(-14.,-17.,-14.,-24.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-18.,-17.,-18.,-24.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-22.,-17.,-22.,-24.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-26.,-17.,-26.,-24.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-30.,-17.,-30.,-24.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-34.,-17.,-34.,-24.,vParking,false,4,net));
		//6. row
		parkingLots.add(createParkingLink(-14.,-31.,-14.,-24.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-18.,-31.,-18.,-24.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-22.,-31.,-22.,-24.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-26.,-31.,-26.,-24.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-30.,-31.,-30.,-24.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-34.,-31.,-34.,-24.,vParking,false,4,net));
		//connections
		createCarLink(-14.,-24.,-18.,-24.,vParking,false,4,net);
		createCarLink(-18.,-24.,-22.,-24.,vParking,false,4,net);
		createCarLink(-22.,-24.,-26.,-24.,vParking,false,4,net);
		createCarLink(-26.,-24.,-30.,-24.,vParking,false,4,net);
		createCarLink(-30.,-24.,-34.,-24.,vParking,false,4,net);
		createCarLink(-34.,-24.,-45.5,-24.,vParking,false,4,net);	
		//7. row
		parkingLots.add(createParkingLink(-14.,-33.,-14.,-40.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-18.,-33.,-18.,-40.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-22.,-33.,-22.,-40.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-26.,-33.,-26.,-40.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-30.,-33.,-30.,-40.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-34.,-33.,-34.,-40.,vParking,false,4,net));
		//8. row
		parkingLots.add(createParkingLink(-14.,-47.,-14.,-40.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-18.,-47.,-18.,-40.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-22.,-47.,-22.,-40.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-26.,-47.,-26.,-40.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-30.,-47.,-30.,-40.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-34.,-47.,-34.,-40.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-38.,-47.,-38.,-40.,vParking,false,4,net));
		parkingLots.add(createParkingLink(-42.,-47.,-42.,-40.,vParking,false,4,net));
		//connections
		createCarLink(-14.,-40.,-18.,-40.,vParking,false,4,net);
		createCarLink(-18.,-40.,-22.,-40.,vParking,false,4,net);
		createCarLink(-22.,-40.,-26.,-40.,vParking,false,4,net);
		createCarLink(-26.,-40.,-30.,-40.,vParking,false,4,net);
		createCarLink(-30.,-40.,-34.,-40.,vParking,false,4,net);
		createCarLink(-34.,-40.,-38.,-40.,vParking,false,4,net);
		createCarLink(-38.,-40.,-42.,-40.,vParking,false,4,net);
		createCarLink(-42.,-40.,-45.5,-40.,vParking,false,4,net);

		//path parking lot entry mall
		createCarLink(-45.5,-40.,-45.5,-24.,vParking,false,4,net);
		createCarLink(-45.5,-24.,-45.5,-8.,vParking,false,4,net);
		createCarLink(-45.5,-8.,-45.5,8.,vParking,false,4,net);
		createPedLink(-45.5,8.,-45.5,22.,1.34,false,4,net);
		
		//into mall
		createPedLink(-45.5,22.,-47.5,22.,1.34,false,4,net);

		//inside mall

		//in front of cash desks
		createPedLink(-47.5,22.,-50,22.,1.34,false,2,net);
		createPedLink(-50,22.,-52.5,22.,1.34,false,2,net);
		createPedLink(-52.5,22.,-55,22.,1.34,false,2,net);
		createPedLink(-55,22.,-57.5,22.,1.34,false,2,net);
		createPedLink(-57.5,22.,-60.,22.,1.34,false,2,net);
		createPedLink(-47.5,22.,-47.5,18.,1.34,false,2,net);
		createPedLink(-50,22.,-50,18.,1.34,false,2,net);
		createPedLink(-52.5,22.,-52.5,18.,1.34,false,2,net);
		createPedLink(-55,22.,-55,18.,1.34,false,2,net);
		createPedLink(-57.5,22.,-57.5,18.,1.34,false,2,net);
		createPedLink(-60,22.,-60,18.,1.34,false,2,net);

		//cash desks
		createPedCashDeskLink(-47.5,8.,-47.5,16.,1.34,true,.1,net);
		cashDesks.add(createPedCashDeskLink(-47.5,16.,-47.5,17.,1.34,true,.5,net));
		createPedCashDeskLink(-47.5,17.,-47.5,18.,1.34,true,.5,net);

		createPedCashDeskLink(-50,8.,-50,16.,1.34,true,.5,net);
		cashDesks.add(createPedCashDeskLink(-50,16.,-50,17.,1.34,true,.5,net));
		createPedCashDeskLink(-50,17.,-50,18.,1.34,true,.5,net);

		createPedCashDeskLink(-52.5,8.,-52.5,16.,1.34,true,.5,net);
		cashDesks.add(createPedCashDeskLink(-52.5,16.,-52.5,17.,1.34,true,.5,net));
		createPedCashDeskLink(-52.5,17.,-52.5,18.,1.34,true,.5,net);

		createPedCashDeskLink(-55,8.,-55,16.,1.34,true,.5,net);
		cashDesks.add(createPedCashDeskLink(-55,16.,-55,17.,1.34,true,.5,net));
		createPedCashDeskLink(-55,17.,-55,18.,1.34,true,.5,net);

		createPedCashDeskLink(-57.5,8.,-57.5,16.,1.34,true,.5,net);
		cashDesks.add(createPedCashDeskLink(-57.5,16.,-57.5,17.,1.34,true,.5,net));
		createPedCashDeskLink(-57.5,17.,-57.5,18.,1.34,true,.5,net);

		createPedCashDeskLink(-60,8.,-60,16.,1.34,true,.1,net);
		cashDesks.add(createPedCashDeskLink(-60,16.,-60,17.,1.34,true,.5,net));
		createPedCashDeskLink(-60,17.,-60,18.,1.34,true,.5,net);

		//cash desks connectors
		//1. col
		createPedLink(-49,-1.,-47.5,8.,1.34,true,4,net);
		createPedLink(-49,-1.,-50,8.,1.34,true,4,net);
		createPedLink(-49,-1.,-52.5,8.,1.34,true,4,net);
		createPedLink(-49,-1.,-55,8.,1.34,true,4,net);
		createPedLink(-49,-1.,-57.5,8.,1.34,true,4,net);
		createPedLink(-49,-1.,-60,8.,1.34,true,4,net);
		//2. col
		createPedLink(-60,-1.,-47.5,8.,1.34,true,4,net);
		createPedLink(-60,-1.,-50,8.,1.34,true,4,net);
		createPedLink(-60,-1.,-52.5,8.,1.34,true,4,net);
		createPedLink(-60,-1.,-55,8.,1.34,true,4,net);
		createPedLink(-60,-1.,-57.5,8.,1.34,true,4,net);
		createPedLink(-60,-1.,-60,8.,1.34,true,4,net);


		//1. shelf col
		////express links
		for (double y = -1; y >= -36; y -= 5) {
			createPedLink(-49,y,-49,y-5,1.34,false,4,net);
		}
		////shopping links
		for (double y = -3.5; y >= -33.5; y -= 5) {
			createPedLink(-50,y,-49,y+2.5,1.34,false,4,net);
			createPedLink(-50,y,-49,y-2.5,1.34,false,4,net);
			shoppingShelfs.add(createPedLink(-50,y,-50,y-5,0.2,false,4,net));
		}
		createPedLink(-50,-38.5,-49,-41,1.34,false,4,net);

		//2. shelf col
		////express links
		for (double y = -1; y >= -36; y -= 5) {
			createPedLink(-60,y,-60,y-5,1.34,false,4,net);
		}
		////shopping links
		for (double y = -3.5; y >= -33.5; y -= 5) {
			createPedLink(-62,y,-60,y+2.5,1.34,false,4,net);
			createPedLink(-62,y,-60,y-2.5,1.34,false,4,net);
			shoppingShelfs.add(createPedLink(-62,y,-62,y-5,0.2,false,4,net));

			createPedLink(-58,y,-60,y+2.5,1.34,false,4,net);
			createPedLink(-58,y,-60,y-2.5,1.34,false,4,net);
			shoppingShelfs.add(createPedLink(-58,y,-58,y-5,0.2,false,4,net));
		}
		createPedLink(-62,-38.5,-60,-41,1.34,false,4,net);
		createPedLink(-58,-38.5,-60,-41,1.34,false,4,net);

		//3. shelf col
		////express links
		for (double y = 14; y >= -36; y -= 5) {
			createPedLink(-72,y,-72,y-5,1.34,false,4,net);
		}
		////shopping links
		for (double y = 11.5; y >= -33.5; y -= 5) {
			createPedLink(-74,y,-72,y+2.5,1.34,false,4,net);
			createPedLink(-74,y,-72,y-2.5,1.34,false,4,net);
			shoppingShelfs.add(createPedLink(-74,y,-74,y-5,0.2,false,4,net));

			createPedLink(-70,y,-72,y+2.5,1.34,false,4,net);
			createPedLink(-70,y,-72,y-2.5,1.34,false,4,net);
			shoppingShelfs.add(createPedLink(-70,y,-70,y-5,0.2,false,4,net));
		}
		createPedLink(-74,-38.5,-72,-41,1.34,false,4,net);
		createPedLink(-70,-38.5,-72,-41,1.34,false,4,net);

		//4. shelf col
		////express links
		for (double y = 14; y >= -36; y -= 5) {
			createPedLink(-84,y,-84,y-5,1.34,false,4,net);
		}
		////shopping links
		for (double y = 11.5; y >= -33.5; y -= 5) {
			createPedLink(-86,y,-84,y+2.5,1.34,false,4,net);
			createPedLink(-86,y,-84,y-2.5,1.34,false,4,net);
			shoppingShelfs.add(createPedLink(-86,y,-86,y-5,0.2,false,4,net));

			createPedLink(-82,y,-84,y+2.5,1.34,false,4,net);
			createPedLink(-82,y,-84,y-2.5,1.34,false,4,net);
			shoppingShelfs.add(createPedLink(-82,y,-82,y-5,0.2,false,4,net));
		}
		createPedLink(-86,-38.5,-84,-41,1.34,false,4,net);
		createPedLink(-82,-38.5,-84,-41,1.34,false,4,net);

		createPedLink(-49,-41,-60,-41,1.34,false,4,net);
		createPedLink(-60,-41,-72,-41,1.34,false,4,net);
		createPedLink(-72,-41,-84,-41,1.34,false,4,net);

		createPedLink(-82,-13.5,-74,-13.5,1.34,false,4,net);
		createPedLink(-74,-13.5,-70,-13.5,1.34,false,4,net);
		createPedLink(-70,-13.5,-62,-13.5,1.34,false,4,net);

		createPedLink(-84,14,-72,14,1.34,false,4,net);
		createPedLink(-81.5,22,-81.5,16,1.34,true,4,net);
		createPedLink(-60,22,-81.5,22,1.34,true,4,net);
		createPedLink(-81.5,16,-84,14,1.34,true,4,net);
		createPedLink(-81.5,16,-72,14,1.34,true,4,net);

		
		carStart = createCarLink(-8, 110, -8, 100, 50/3.6, true, 1, net);
		createCarLink(-8, 100, -8, 50, 50/3.6, true, 1, net);
		carTrafficLight = createCarLink(-8,50,-8,32,50/3.6,true,1,net);
		createCarLink(-8,32,-8,8,50/3.6,true,1,net);
		createCarLink(-8,8,-14,8,1.34,true,1,net);
		createCarLink(-8,8,-8,-40,50/3.6,true,1,net);
		createCarLink(-14,-40,-8,-40,1.34,true,1,net);
		carStop = createCarLink(-8,-40,-8,-70,50/36,true,1,net);
		
		carInNode = carStart.getFromNode().getId();
		carOutNode = carStop.getToNode().getId();

		//for debugging only
		dumpNetworkAsShapeFile(sc,inputDir);
	}

	//pedestrian cash desk link 
	private static Link createPedCashDeskLink(double fromX, double fromY, double toX, double toY,
			double v,boolean oneWay,double width,NetworkImpl net) {

		NetworkFactoryImpl nf = net.getFactory();
		CoordinateTransformer transformer = new CoordinateTransformer(dx, dy, da);
		
		Coord from = transformer.transformCoord(fromX, fromY);
		Coord to = transformer.transformCoord(toX, toY);
		double length = CoordUtils.calcDistance(from, to);

		Node n0 = net.getNearestNode(from);
		if (n0 == null || CoordUtils.calcDistance(from, n0.getCoord()) > EPSILON) {
			n0 = nf.createNode(new IdImpl(idMarker + nodeID++), from);
			net.addNode(n0);
		}

		Node n1 = net.getNearestNode(to);
		if (CoordUtils.calcDistance(to, n1.getCoord()) > EPSILON) {
			n1 = nf.createNode(new IdImpl(idMarker + nodeID++), to);
			net.addNode(n1);
		}		

		double cap  = 1./20;//every 20 sec one person
		double lanes = 5.4*0.26 * width;
		Link l = nf.createLink(new IdImpl(idMarker + linkID++), n0, n1, net, length, v,cap , lanes);
		l.setAllowedModes(walkModes);
		net.addLink(l);
		if (!oneWay) {
			Link lr = nf.createLink(new IdImpl(idMarker + linkID++), n1, n0, net, length, v,cap , lanes);
			lr.setAllowedModes(walkModes);
			net.addLink(lr);	
		}

		return l;

	}
	
	//car link 
	private static Link createCarLink(double fromX, double fromY, double toX, double toY,
			double v, boolean oneWay, double width, NetworkImpl net) {

		NetworkFactoryImpl nf = net.getFactory();
		CoordinateTransformer transformer = new CoordinateTransformer(dx, dy, da);
		
		Coord from = transformer.transformCoord(fromX, fromY);
		Coord to = transformer.transformCoord(toX, toY);
		double length = CoordUtils.calcDistance(from, to);

		Node n0 = net.getNearestNode(from);
		if (n0 == null || CoordUtils.calcDistance(from, n0.getCoord()) > EPSILON) {
			n0 = nf.createNode(new IdImpl(idMarker + nodeID++), from);
			net.addNode(n0);
		}

		Node n1 = net.getNearestNode(to);
		if (CoordUtils.calcDistance(to, n1.getCoord()) > EPSILON) {
			n1 = nf.createNode(new IdImpl(idMarker + nodeID++), to);
			net.addNode(n1);
		}		

		double cap  = 600;
		double lanes = 1;
		Link l = nf.createLink(new IdImpl(idMarker + linkID++), n0, n1, net, length, v, cap, lanes);
		l.setAllowedModes(carModes);
		net.addLink(l);
		if (!oneWay) {
			Link lr = nf.createLink(new IdImpl(idMarker + linkID++), n1, n0, net, length, v, cap, lanes);
			lr.setAllowedModes(carModes);
			net.addLink(lr);
		}

		return l;
	}

	// pedestrian link 
	private static Link createParkingLink(double fromX, double fromY, double toX, double toY,
			double v, boolean oneWay, double width, NetworkImpl net) {

		NetworkFactoryImpl nf = net.getFactory();
		CoordinateTransformer transformer = new CoordinateTransformer(dx, dy, da);

		Coord from = transformer.transformCoord(fromX, fromY);
		Coord to = transformer.transformCoord(toX, toY);
		double length = CoordUtils.calcDistance(from, to);

		Node n0 = net.getNearestNode(from);
		if (n0 == null || CoordUtils.calcDistance(from, n0.getCoord()) > EPSILON) {
			n0 = nf.createNode(new IdImpl(idMarker + nodeID++), from);
			net.addNode(n0);
		}

		Node n1 = net.getNearestNode(to);
		if (CoordUtils.calcDistance(to, n1.getCoord()) > EPSILON) {
			n1 = nf.createNode(new IdImpl(idMarker + nodeID++), to);
			net.addNode(n1);
		}		

		double cap  = width * 1.33;
		double lanes = 5.4*0.26 * width;
		Link l = nf.createLink(new IdImpl(idMarker + parkingMarker + linkID++), n0, n1, net, length, v, cap, lanes);
		l.setAllowedModes(carModes);
		net.addLink(l);
		if (!oneWay) {
			Link lr = nf.createLink(new IdImpl(idMarker + linkID++), n1, n0, net, length, v, cap , lanes);
			lr.setAllowedModes(carModes);
			net.addLink(lr);	
		}

		return l;
	}
	
	// pedestrian link 
	private static Link createPedLink(double fromX, double fromY, double toX, double toY,
			double v, boolean oneWay, double width, NetworkImpl net) {

		NetworkFactoryImpl nf = net.getFactory();
		CoordinateTransformer transformer = new CoordinateTransformer(dx, dy, da);
		
		Coord from = transformer.transformCoord(fromX, fromY);
		Coord to = transformer.transformCoord(toX, toY);
		double length = CoordUtils.calcDistance(from, to);

		Node n0 = net.getNearestNode(from);
		if (n0 == null || CoordUtils.calcDistance(from, n0.getCoord()) > EPSILON) {
			n0 = nf.createNode(new IdImpl(idMarker + nodeID++), from);
			net.addNode(n0);
		}

		Node n1 = net.getNearestNode(to);
		if (CoordUtils.calcDistance(to, n1.getCoord()) > EPSILON) {
			n1 = nf.createNode(new IdImpl(idMarker + nodeID++), to);
			net.addNode(n1);
		}		

		double cap  = width * 1.33;
		double lanes = 5.4*0.26 * width;
		Link l = nf.createLink(new IdImpl(idMarker + linkID++), n0, n1, net, length, v,cap , lanes);
		l.setAllowedModes(walkModes);
		net.addLink(l);
		if (!oneWay) {
			Link lr = nf.createLink(new IdImpl(idMarker + linkID++), n1, n0, net, length, v,cap , lanes);
			lr.setAllowedModes(walkModes);
			net.addLink(lr);	
		}

		return l;
	}

	private static void createAndSaveEnvironment(String inputDir) {

		//stairway boundary from subway station
		List<Coordinate> stair = new ArrayList<Coordinate>();
		stair.add(getC(11,12));
		stair.add(getC(11,0));
		stair.add(getC(7,0));
		stair.add(getC(7,12));
		crLs(stair,"stairway");

		//curbside
		List<Coordinate> curbs = new ArrayList<Coordinate>();
		curbs.add(getC(7,12));
		curbs.add(getC(7,20));
		crLs(curbs,"curbside01");

		//parking lot
		List<Coordinate> parking = new ArrayList<Coordinate>();
		parking.add(getC(-12,16));
		parking.add(getC(-44,16));
		crLs(parking,"parking01");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-44,16));
		parking.add(getC(-44,11));
		crLs(parking,"parking02");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-40,16));
		parking.add(getC(-40,11));
		crLs(parking,"parking03");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-36,16));
		parking.add(getC(-36,11));
		crLs(parking,"parking04");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-32,16));
		parking.add(getC(-32,11));
		crLs(parking,"parking05");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-28,16));
		parking.add(getC(-28,11));
		crLs(parking,"parking06");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-24,16));
		parking.add(getC(-24,11));
		crLs(parking,"parking07");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-20,16));
		parking.add(getC(-20,11));
		crLs(parking,"parking08");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-16,16));
		parking.add(getC(-16,11));
		crLs(parking,"parking09");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-12,16));
		parking.add(getC(-12,11));
		crLs(parking,"parking10");

		parking = new ArrayList<Coordinate>();
		parking.add(getC(-12,0));
		parking.add(getC(-36,0));
		crLs(parking,"parking11");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-12,5));
		parking.add(getC(-12,-5));
		crLs(parking,"parking12");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-16,5));
		parking.add(getC(-16,-5));
		crLs(parking,"parking13");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-20,5));
		parking.add(getC(-20,-5));
		crLs(parking,"parking14");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-24,5));
		parking.add(getC(-24,-5));
		crLs(parking,"parking15");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-28,5));
		parking.add(getC(-28,-5));
		crLs(parking,"parking16");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-32,5));
		parking.add(getC(-32,-5));
		crLs(parking,"parking17");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-36,5));
		parking.add(getC(-36,-5));
		crLs(parking,"parking18");

		parking = new ArrayList<Coordinate>();
		parking.add(getC(-12,-16));
		parking.add(getC(-36,-16));
		crLs(parking,"parking19");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-12,-11));
		parking.add(getC(-12,-21));
		crLs(parking,"parking20");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-16,-11));
		parking.add(getC(-16,-21));
		crLs(parking,"parking21");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-20,-11));
		parking.add(getC(-20,-21));
		crLs(parking,"parking22");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-24,-11));
		parking.add(getC(-24,-21));
		crLs(parking,"parking23");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-28,-11));
		parking.add(getC(-28,-21));
		crLs(parking,"parking24");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-32,-11));
		parking.add(getC(-32,-21));
		crLs(parking,"parking25");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-36,-11));
		parking.add(getC(-36,-21));
		crLs(parking,"parking26");

		parking = new ArrayList<Coordinate>();
		parking.add(getC(-12,-32));
		parking.add(getC(-36,-32));
		crLs(parking,"parking27");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-12,-27));
		parking.add(getC(-12,-37));
		crLs(parking,"parking28");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-16,-27));
		parking.add(getC(-16,-37));
		crLs(parking,"parking29");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-20,-27));
		parking.add(getC(-20,-37));
		crLs(parking,"parking30");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-24,-27));
		parking.add(getC(-24,-37));
		crLs(parking,"parking31");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-28,-27));
		parking.add(getC(-28,-37));
		crLs(parking,"parking32");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-32,-27));
		parking.add(getC(-32,-37));
		crLs(parking,"parking33");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-36,-27));
		parking.add(getC(-36,-37));
		crLs(parking,"parking34");
		
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-12,-48));
		parking.add(getC(-44,-48));
		crLs(parking,"parking35");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-12,-48));
		parking.add(getC(-12,-43));
		crLs(parking,"parking36");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-16,-48));
		parking.add(getC(-16,-43));
		crLs(parking,"parking37");		
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-20,-48));
		parking.add(getC(-20,-43));
		crLs(parking,"parking38");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-24,-48));
		parking.add(getC(-24,-43));
		crLs(parking,"parking39");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-28,-48));
		parking.add(getC(-28,-43));
		crLs(parking,"parking40");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-32,-48));
		parking.add(getC(-32,-43));
		crLs(parking,"parking41");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-36,-48));
		parking.add(getC(-36,-43));
		crLs(parking,"parking42");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-40,-48));
		parking.add(getC(-40,-43));
		crLs(parking,"parking43");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-44,-48));
		parking.add(getC(-44,-43));
		crLs(parking,"parking44");

		//mall
		List<Coordinate> mall = new ArrayList<Coordinate>();
		mall.add(getC(-47,20));
		mall.add(getC(-47,-44));
		mall.add(getC(-87,-44));
		mall.add(getC(-87,36));
		mall.add(getC(-47,36));
		mall.add(getC(-47,24));
		crLs(mall,"mall01");

		//// cash desks
		mall = new ArrayList<Coordinate>();
		mall.add(getC(-48,18));
		mall.add(getC(-48,13));
		mall.add(getC(-49.5,13));
		mall.add(getC(-49.5,18));
		mall.add(getC(-48,18));
		crLs(mall,"counter01");
		mall = new ArrayList<Coordinate>();
		mall.add(getC(-50.5,18));
		mall.add(getC(-50.5,13));
		mall.add(getC(-52,13));
		mall.add(getC(-52,18));
		mall.add(getC(-50.5,18));
		crLs(mall,"counter02");		
		mall = new ArrayList<Coordinate>();
		mall.add(getC(-53,18));
		mall.add(getC(-53,13));
		mall.add(getC(-54.5,13));
		mall.add(getC(-54.5,18));
		mall.add(getC(-53,18));
		crLs(mall,"counter03");
		mall = new ArrayList<Coordinate>();
		mall.add(getC(-55.5,18));
		mall.add(getC(-55.5,13));
		mall.add(getC(-57,13));
		mall.add(getC(-57,18));
		mall.add(getC(-55.5,18));
		crLs(mall,"counter04");		
		mall = new ArrayList<Coordinate>();
		mall.add(getC(-58,18));
		mall.add(getC(-58,13));
		mall.add(getC(-59.5,13));
		mall.add(getC(-59.5,18));
		mall.add(getC(-58,18));
		crLs(mall,"counter05");

		/////walls
		mall = new ArrayList<Coordinate>();
		mall.add(getC(-60.5,13));
		mall.add(getC(-60.5,18));
		mall.add(getC(-80,18));
		crLs(mall,"wall01");
		mall = new ArrayList<Coordinate>();
		mall.add(getC(-83,18));
		mall.add(getC(-87,18));
		crLs(mall,"wall02");

		//// shelfs
		mall = new ArrayList<Coordinate>();
		mall.add(getC(-81,12));
		mall.add(getC(-75,12));
		mall.add(getC(-75,-10));
		mall.add(getC(-81,-10));
		mall.add(getC(-81,12));
		crLs(mall,"shelf01");
		mall = new ArrayList<Coordinate>();
		mall.add(getC(-81,-16));
		mall.add(getC(-75,-16));
		mall.add(getC(-75,-38));
		mall.add(getC(-81,-38));
		mall.add(getC(-81,-16));
		crLs(mall,"shelf02");
		mall = new ArrayList<Coordinate>();
		mall.add(getC(-69,12));
		mall.add(getC(-63,12));
		mall.add(getC(-63,-10));
		mall.add(getC(-69,-10));
		mall.add(getC(-69,12));
		crLs(mall,"shelf03");
		mall = new ArrayList<Coordinate>();
		mall.add(getC(-69,-16));
		mall.add(getC(-63,-16));
		mall.add(getC(-63,-38));
		mall.add(getC(-69,-38));
		mall.add(getC(-69,-16));
		crLs(mall,"shelf04");
		mall = new ArrayList<Coordinate>();
		mall.add(getC(-57,-4));
		mall.add(getC(-51,-4));
		mall.add(getC(-51,-38));
		mall.add(getC(-57,-38));
		mall.add(getC(-57,-4));
		crLs(mall,"shelf05");

		GisDebugger.dump(inputDir + "/environment.shp");

	}

	private static void crLs(List<Coordinate> stair, String string) {
		Coordinate [] coords = new Coordinate[stair.size()];
		for (int i = 0; i < stair.size(); i++) {
			coords[i] = stair.get(i);
		}
//		Coordinate [] coords = new Coordinate[2*stair.size()];
//		for (int i = 0; i < stair.size(); i++) {
//			coords[i] = stair.get(i);
//			coords[2*stair.size() - 1 - i] = stair.get(i);
//		}
//		
		LineString ls = geofac.createLineString(coords);
		GisDebugger.addGeometry(ls, string);

	}

	private static Coordinate getC(double x, double y) {
//		return new Coordinate(x,y);
		return new CoordinateTransformer(dx, dy, da).transformCoordinate(x, y);
	}

	/*package*/ static String EPSG = "EPSG: 32632";	
	/*package*/ static void dumpNetworkAsShapeFile(Scenario sc, String inputDir) {
		final Network network = sc.getNetwork();
		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, EPSG);
		builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
		builder.setWidthCoefficient(0.5);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		new Links2ESRIShape(network,inputDir+"/links_ls.shp", builder).write();
	}

	
	/*
	 * To be overwritten to transform coordinates when
	 * merging with zurich network.
	 */
	public static double dx = 0;
	public static double dy = 0;
	public static double da = 0;
	
	private static class CoordinateTransformer implements NetworkRunnable {

		private final double dx;
		private final double dy;
		private final double da;
		private final double cosA;
		private final double sinA;
		
		public CoordinateTransformer(double dx, double dy, double da) {
			this.dx = dx;
			this.dy = dy;
			this.da = da;
			
			this.cosA = Math.cos(da * Math.PI / 180);
			this.sinA = Math.sin(da * Math.PI / 180);
		}
		
		public Coord transformCoord(double x, double y) {
			
			// rotate coordinate
			double X = (x * this.cosA - y * this.sinA);
			double Y = (x * this.sinA + y * this.cosA);
			
			// move coordinate
			X += this.dx;
			Y += this.dy;
			
			return new CoordImpl(X,Y);
		}
		
		public Coordinate transformCoordinate(double x, double y) {

			Coordinate coordinate = new Coordinate(x,y);
			
			// rotate coordinate
			coordinate.x = (x * this.cosA - y * this.sinA);
			coordinate.y = (x * this.sinA + y * this.cosA);
			
			// move coordinate
			coordinate.x += this.dx;
			coordinate.y += this.dy;
			
			return coordinate;
		}
		
		@Override
		public void run(Network network) {
			
//			// rotate network
//			double cosA = Math.cos(da * Math.PI / 180);
//			double sinA = Math.sin(da * Math.PI / 180);
//			for (Node node : network.getNodes().values()) {
//				Coord coord = node.getCoord();
//				double x = coord.getX();
//				double y = coord.getY();
//				coord.setX(x * cosA - y * sinA);
//				coord.setY(x * sinA + y * cosA);
//			}
//			
//			// move network
//			for (Node node : network.getNodes().values()) {
//				Coord coord = node.getCoord();
//				coord.setX(coord.getX() + dx);
//				coord.setY(coord.getY() + dy);
//			}
		}
	}
}
