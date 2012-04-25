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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
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
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTimeCalculator;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.LineStringBasedFeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.helper.gisdebug.GisDebugger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;


public class PED12ScenarioGen {
	
	private static final double EPSILON = 0.0001;
	private static GeometryFactory geofac = new GeometryFactory();
	private static int nodeID = 0;
	private static int linkID = 0;
	
	private static int persID = 0;
	
	
	private static List<Link> shoppingShelfs = new ArrayList<Link>();
	private static List<Link> cashDesks = new ArrayList<Link>();
	
	public static void main(String [] args) {
		String scDir = "/Users/laemmel/devel/ped12_dobLaem/";
		String inputDir = scDir + "/input/";

		Config c = ConfigUtils.createConfig();
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

//		if (model == SC.Helbing) {
			s2d.setEnableCircularAgentInterActionModule("true");
			s2d.setEnableEnvironmentForceModule("true");
			s2d.setEnableCollisionPredictionAgentInteractionModule("false");
			s2d.setEnableCollisionPredictionEnvironmentForceModule("false");
			s2d.setEnablePathForceModule("true");
			s2d.setEnableDrivingForceModule("true");
			s2d.setEnableVelocityObstacleModule("false");
			s2d.setEnablePhysicalEnvironmentForceModule("false");

//		s2d.setTimeStepSize(""+0.1);
		QSimConfigGroup qsim = new QSimConfigGroup();
		qsim.setEndTime(600);
		//				qsim.setTimeStepSize(1./25.);
		c.addModule("qsim", qsim);
		
		c.addModule("sim2d", s2d);
		
		
		
		ActivityParams pre = new ActivityParams("h");
		pre.setTypicalDuration(49); // needs to be geq 49, otherwise when running a simulation one gets "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in ActivityUtilityParameters.java (gl)
		pre.setMinimalDuration(49);
		pre.setClosingTime(49);
		pre.setEarliestEndTime(49);
		pre.setLatestStartTime(49);
		pre.setOpeningTime(49);

		sc.getConfig().planCalcScore().addActivityParams(pre);
		
		new ConfigWriter(c).write(inputDir + "/config.xml");

		
	}

	private static void createAndSavePopulation(Scenario sc, String inputDir) {
		
		Network network = sc.getNetwork();
		FreeSpeedTravelTimeCalculator fs = new FreeSpeedTravelTimeCalculator();
		TravelDisutility cost = new TravelCostCalculatorFactoryImpl().createTravelDisutility(fs,sc.getConfig().planCalcScore() );
		Dijkstra dijkstra = new Dijkstra(network, cost, fs);
		
		//subway
		Node n = ((NetworkImpl)sc.getNetwork()).getNearestNode(new CoordImpl(-2,1));
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
				Coord actCoord = new CoordImpl(x,y);
				createSubwayPerson(persTime,start,end,actCoord,sc,dijkstra);
			}
			
			
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
		
		Route route = createRandomShoppingRoute(start,end,dijkstra);
		leg.setRoute(route );
		
		plan.addLeg(leg);
		Activity act2 = pb.createActivityFromLinkId("h",end.getId());
		act2.setEndTime(0);
		plan.addActivity(act2);
		pers.addPlan(plan);
		
		sc.getPopulation().addPerson(pers);
	}


	private static Route createRandomShoppingRoute(Link start, Link end, Dijkstra dijkstra) {
		
		int stopsAtShoppingShelfs = MatsimRandom.getRandom().nextInt(3)+1; // three stops max
		
		Link current = start;
		
		List<Link> links = new ArrayList<Link>();
		for (int stop = 0; stop < stopsAtShoppingShelfs; stop++) {
			
			Link next = shoppingShelfs.get(MatsimRandom.getRandom().nextInt(shoppingShelfs.size()));
			Node from = current.getToNode();
			Node to = next.getFromNode();
			
			Path r = dijkstra.calcLeastCostPath(from, to, 0,null,null);
			links.addAll(r.links);
			links.add(next);
			current = next;
		}
		
		
		Link cashDesk = cashDesks.get(MatsimRandom.getRandom().nextInt(cashDesks.size()));
		Node from = current.getToNode();
		Node to = cashDesk.getFromNode();
		
		Path r = dijkstra.calcLeastCostPath(from, to, 0,null,null);
		links.addAll(r.links);
		links.add(cashDesk);
		current = cashDesk;		
		
		from = current.getToNode();
		to = end.getFromNode();
		
		r = dijkstra.calcLeastCostPath(from, to, 0,null,null);
		links.addAll(r.links);
		current = cashDesk;
		
		List<Id> linkIds = new ArrayList<Id>();
		for (Link l : links) {
			linkIds.add(l.getId());
		}
		
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(links.get(0).getId(), links.get(links.size()-1).getId());
		route.setLinkIds(links.get(0).getId(),linkIds ,links.get(links.size()-1).getId());
		
		return route;
	}

	private static void createAndSaveNetwork(Scenario sc, String inputDir) {
		
		NetworkImpl net = (NetworkImpl) sc.getNetwork();
		//path from subway to mall entry
		
		////stairs
		//subway platform
		createPedLink(-2.,1.,-2.,3.,1.34,false,4,net);
		//up VF,h = 0.610 m/s
		createPedLink(-2.,3.,-2.,12.,0.61,true,4,net);
		//down VF,h = 0.694 m/s
		createPedLink(-2.,12.,-2.,3.,0.694,true,4,net);
		
		createPedLink(-2.,12.,-2.,22.,1.34,false,4,net);
		createPedLink(-2.,22.,-45.5,22.,1.34,false,4,net);
		
		//parking lot
		//1.row
		createPedLink(-14.,19.,-14.,8.,1.34,false,4,net);
		createPedLink(-18.,19.,-18.,8.,1.34,false,4,net);
		createPedLink(-22.,19.,-22.,8.,1.34,false,4,net);
		createPedLink(-26.,19.,-26.,8.,1.34,false,4,net);
		createPedLink(-30.,19.,-30.,8.,1.34,false,4,net);
		createPedLink(-34.,19.,-34.,8.,1.34,false,4,net);
		createPedLink(-38.,19.,-38.,8.,1.34,false,4,net);
		createPedLink(-42.,19.,-42.,8.,1.34,false,4,net);
		//2. row
		createPedLink(-14.,-4.,-14.,8.,1.34,false,4,net);
		createPedLink(-18.,-4.,-18.,8.,1.34,false,4,net);
		createPedLink(-22.,-4.,-22.,8.,1.34,false,4,net);
		createPedLink(-26.,-4.,-26.,8.,1.34,false,4,net);
		createPedLink(-30.,-4.,-30.,8.,1.34,false,4,net);
		createPedLink(-34.,-4.,-34.,8.,1.34,false,4,net);
		//connections
		createPedLink(-14.,8.,-18.,8.,1.34,false,4,net);
		createPedLink(-18.,8.,-22.,8.,1.34,false,4,net);
		createPedLink(-22.,8.,-26.,8.,1.34,false,4,net);
		createPedLink(-26.,8.,-30.,8.,1.34,false,4,net);
		createPedLink(-30.,8.,-34.,8.,1.34,false,4,net);
		createPedLink(-34.,8.,-38.,8.,1.34,false,4,net);
		createPedLink(-38.,8.,-42.,8.,1.34,false,4,net);
		createPedLink(-42.,8.,-45.5,8.,1.34,false,4,net);
		//3. row
		createPedLink(-14.,-6.,-14.,-18.,1.34,false,4,net);
		createPedLink(-18.,-6.,-18.,-18.,1.34,false,4,net);
		createPedLink(-22.,-6.,-22.,-18.,1.34,false,4,net);
		createPedLink(-26.,-6.,-26.,-18.,1.34,false,4,net);
		createPedLink(-30.,-6.,-30.,-18.,1.34,false,4,net);
		createPedLink(-34.,-6.,-34.,-18.,1.34,false,4,net);
		//4. row
		createPedLink(-14.,-29.,-14.,-18.,1.34,false,4,net);
		createPedLink(-18.,-29.,-18.,-18.,1.34,false,4,net);
		createPedLink(-22.,-29.,-22.,-18.,1.34,false,4,net);
		createPedLink(-26.,-29.,-26.,-18.,1.34,false,4,net);
		createPedLink(-30.,-29.,-30.,-18.,1.34,false,4,net);
		createPedLink(-34.,-29.,-34.,-18.,1.34,false,4,net);
		//connections
		createPedLink(-14.,-18.,-18.,-18.,1.34,false,4,net);
		createPedLink(-18.,-18.,-22.,-18.,1.34,false,4,net);
		createPedLink(-22.,-18.,-26.,-18.,1.34,false,4,net);
		createPedLink(-26.,-18.,-30.,-18.,1.34,false,4,net);
		createPedLink(-30.,-18.,-34.,-18.,1.34,false,4,net);
		createPedLink(-34.,-18.,-45.5,-18.,1.34,false,4,net);
		//5. row
		createPedLink(-14.,-31.,-14.,-43.,1.34,false,4,net);
		createPedLink(-18.,-31.,-18.,-43.,1.34,false,4,net);
		createPedLink(-22.,-31.,-22.,-43.,1.34,false,4,net);
		createPedLink(-26.,-31.,-26.,-43.,1.34,false,4,net);
		createPedLink(-30.,-31.,-30.,-43.,1.34,false,4,net);
		createPedLink(-34.,-31.,-34.,-43.,1.34,false,4,net);
		//6. row
		createPedLink(-14.,-54.,-14.,-43.,1.34,false,4,net);
		createPedLink(-18.,-54.,-18.,-43.,1.34,false,4,net);
		createPedLink(-22.,-54.,-22.,-43.,1.34,false,4,net);
		createPedLink(-26.,-54.,-26.,-43.,1.34,false,4,net);
		createPedLink(-30.,-54.,-30.,-43.,1.34,false,4,net);
		createPedLink(-34.,-54.,-34.,-43.,1.34,false,4,net);
		createPedLink(-38.,-54.,-38.,-43.,1.34,false,4,net);
		createPedLink(-42.,-54.,-42.,-43.,1.34,false,4,net);
		//connections
		createPedLink(-14.,-43.,-18.,-43.,1.34,false,4,net);
		createPedLink(-18.,-43.,-22.,-43.,1.34,false,4,net);
		createPedLink(-22.,-43.,-26.,-43.,1.34,false,4,net);
		createPedLink(-26.,-43.,-30.,-43.,1.34,false,4,net);
		createPedLink(-30.,-43.,-34.,-43.,1.34,false,4,net);
		createPedLink(-34.,-43.,-38.,-43.,1.34,false,4,net);
		createPedLink(-38.,-43.,-42.,-43.,1.34,false,4,net);
		createPedLink(-42.,-43.,-45.5,-43.,1.34,false,4,net);
		//path parking lot entry mall
		createPedLink(-45.5,-43.,-45.5,-18.,1.34,false,4,net);
		createPedLink(-45.5,-18.,-45.5,8.,1.34,false,4,net);
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
		cashDesks.add(createPedCashDeskLink(-47.5,8.,-47.5,18.,1.34,true,.71,net));
		cashDesks.add(createPedCashDeskLink(-50,8.,-50,18.,1.34,true,.71,net));
		cashDesks.add(createPedCashDeskLink(-52.5,8.,-52.5,18.,1.34,true,.71,net));
		cashDesks.add(createPedCashDeskLink(-55,8.,-55,18.,1.34,true,.71,net));
		cashDesks.add(createPedCashDeskLink(-57.5,8.,-57.5,18.,1.34,true,.71,net));
		cashDesks.add(createPedCashDeskLink(-60,8.,-60,18.,1.34,true,.71,net));
		
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
		
		//for debugging only
		dumpNetworkAsShapeFile(sc,inputDir);
		
		
		
	}
	
	
	//pedestrian cash desk link 
	private static Link createPedCashDeskLink(double fromX, double fromY, double toX, double toY,
			double v,boolean oneWay,double width,NetworkImpl net) {
		
		NetworkFactoryImpl nf = net.getFactory();
		
		CoordImpl from =  new CoordImpl(fromX, fromY);
		CoordImpl to =  new CoordImpl(toX, toY);
		double length = from.calcDistance(to);
		
		Node n0 = net.getNearestNode(from);
		if (n0 == null || from.calcDistance(n0.getCoord()) > EPSILON) {
			n0 = nf.createNode(new IdImpl(nodeID++), from);
			net.addNode(n0);
		}
		
		Node n1 = net.getNearestNode(to);
		if (to.calcDistance(n1.getCoord()) > EPSILON) {
			n1 = nf.createNode(new IdImpl(nodeID++), to);
			net.addNode(n1);
		}		
		
		double cap  = 1./20;//every 20 sec one person
		double lanes = 5.4*0.26 * width;
		Link l = nf.createLink(new IdImpl(linkID++), n0, n1, net, length, v,cap , lanes);
		net.addLink(l);
		if (!oneWay) {
			Link lr = nf.createLink(new IdImpl(linkID++), n1, n0, net, length, v,cap , lanes);
			net.addLink(lr);	
		}
		
		return l;
		
	}

	//pedestrian link 
	private static Link createPedLink(double fromX, double fromY, double toX, double toY,
			double v,boolean oneWay,double width,NetworkImpl net) {
		
		NetworkFactoryImpl nf = net.getFactory();
		
		CoordImpl from =  new CoordImpl(fromX, fromY);
		CoordImpl to =  new CoordImpl(toX, toY);
		double length = from.calcDistance(to);
		
		Node n0 = net.getNearestNode(from);
		if (n0 == null || from.calcDistance(n0.getCoord()) > EPSILON) {
			n0 = nf.createNode(new IdImpl(nodeID++), from);
			net.addNode(n0);
		}
		
		Node n1 = net.getNearestNode(to);
		if (to.calcDistance(n1.getCoord()) > EPSILON) {
			n1 = nf.createNode(new IdImpl(nodeID++), to);
			net.addNode(n1);
		}		
		
		double cap  = width * 1.33;
		double lanes = 5.4*0.26 * width;
		Link l = nf.createLink(new IdImpl(linkID++), n0, n1, net, length, v,cap , lanes);
		net.addLink(l);
		if (!oneWay) {
			Link lr = nf.createLink(new IdImpl(linkID++), n1, n0, net, length, v,cap , lanes);
			net.addLink(lr);	
		}
		
		return l;
	}

	private static void createAndSaveEnvironment(String inputDir) {
		
		
		
		//stairway boundary from subway station
		List<Coordinate> stair = new ArrayList<Coordinate>();
		stair.add(getC(0,12));
		stair.add(getC(0,0));
		stair.add(getC(-4,0));
		stair.add(getC(-4,12));
		crLs(stair,"stairway");
		
		//curbside
		List<Coordinate> curbs = new ArrayList<Coordinate>();
		curbs.add(getC(-4,12));
		curbs.add(getC(-4,20));
		crLs(curbs,"curbside01");
		
		//parking lot
		List<Coordinate> parking = new ArrayList<Coordinate>();
		parking.add(getC(-12,20));
		parking.add(getC(-44,20));
		crLs(parking,"parking01");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-44,20));
		parking.add(getC(-44,12));
		crLs(parking,"parking02");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-40,20));
		parking.add(getC(-40,12));
		crLs(parking,"parking03");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-36,20));
		parking.add(getC(-36,12));
		crLs(parking,"parking04");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-32,20));
		parking.add(getC(-32,12));
		crLs(parking,"parking05");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-28,20));
		parking.add(getC(-28,12));
		crLs(parking,"parking06");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-24,20));
		parking.add(getC(-24,12));
		crLs(parking,"parking07");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-20,20));
		parking.add(getC(-20,12));
		crLs(parking,"parking08");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-16,20));
		parking.add(getC(-16,12));
		crLs(parking,"parking09");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-12,20));
		parking.add(getC(-12,12));
		crLs(parking,"parking10");
		
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-12,-5));
		parking.add(getC(-36,-5));
		crLs(parking,"parking11");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-12,3));
		parking.add(getC(-12,-13));
		crLs(parking,"parking12");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-16,3));
		parking.add(getC(-16,-13));
		crLs(parking,"parking13");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-20,3));
		parking.add(getC(-20,-13));
		crLs(parking,"parking14");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-24,3));
		parking.add(getC(-24,-13));
		crLs(parking,"parking15");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-28,3));
		parking.add(getC(-28,-13));
		crLs(parking,"parking16");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-32,3));
		parking.add(getC(-32,-13));
		crLs(parking,"parking17");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-36,3));
		parking.add(getC(-36,-13));
		crLs(parking,"parking18");
		
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-12,-30));
		parking.add(getC(-36,-30));
		crLs(parking,"parking19");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-12,-22));
		parking.add(getC(-12,-38));
		crLs(parking,"parking20");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-16,-22));
		parking.add(getC(-16,-38));
		crLs(parking,"parking21");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-20,-22));
		parking.add(getC(-20,-38));
		crLs(parking,"parking22");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-24,-22));
		parking.add(getC(-24,-38));
		crLs(parking,"parking23");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-28,-22));
		parking.add(getC(-28,-38));
		crLs(parking,"parking24");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-32,-22));
		parking.add(getC(-32,-38));
		crLs(parking,"parking25");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-36,-22));
		parking.add(getC(-36,-38));
		crLs(parking,"parking26");
		
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-12,-55));
		parking.add(getC(-44,-55));
		crLs(parking,"parking27");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-12,-55));
		parking.add(getC(-12,-47));
		crLs(parking,"parking28");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-16,-55));
		parking.add(getC(-16,-47));
		crLs(parking,"parking29");		
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-20,-55));
		parking.add(getC(-20,-47));
		crLs(parking,"parking30");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-24,-55));
		parking.add(getC(-24,-47));
		crLs(parking,"parking31");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-28,-55));
		parking.add(getC(-28,-47));
		crLs(parking,"parking32");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-32,-55));
		parking.add(getC(-32,-47));
		crLs(parking,"parking33");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-36,-55));
		parking.add(getC(-36,-47));
		crLs(parking,"parking34");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-40,-55));
		parking.add(getC(-40,-47));
		crLs(parking,"parking35");
		parking = new ArrayList<Coordinate>();
		parking.add(getC(-44,-55));
		parking.add(getC(-44,-47));
		crLs(parking,"parking36");
		
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
		
		LineString ls = geofac.createLineString(coords);
		GisDebugger.addGeometry(ls, string);
		
	}

	private static Coordinate getC(double x, double y) {
		return new Coordinate(x,y);
	}
	
	private static void dumpNetworkAsShapeFile(Scenario sc, String inputDir) {
		final Network network = sc.getNetwork();
		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, "EPSG: 32632");
		builder.setFeatureGeneratorPrototype(LineStringBasedFeatureGenerator.class);
		builder.setWidthCoefficient(0.5);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		new Links2ESRIShape(network,inputDir+"/links_ls.shp", builder).write();
	}

}
