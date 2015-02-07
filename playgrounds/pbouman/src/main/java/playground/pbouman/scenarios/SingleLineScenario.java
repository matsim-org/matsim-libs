/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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


package playground.pbouman.scenarios;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;

import playground.pbouman.agentproperties.AgentProperties;


/**
 * This was some scenario that I played with a while ago and it is probably
 * similar to the ElFaroLTrainScenario, however, I do not remember exactly.
 * @author pcbouman
 *
 */

public class SingleLineScenario
{
	public static ScenarioImpl buildScenario()
	{
		return buildScenario(300);
	}
	
	public static ScenarioImpl buildScenario(int numAgents)
	{
		Config config = ConfigUtils.createConfig();
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		
		HashSet<String> transitModes = new HashSet<String>();
		transitModes.add("pt");
		config.transit().setTransitModes(transitModes);
		ScenarioImpl scen = (ScenarioImpl) ScenarioUtils.createScenario(config);
		
		/* ************** *
		 * Create Network *
		 * ************** */
		
		Network net = scen.getNetwork();
		NetworkFactory nw = net.getFactory();
		
		Node a = nw.createNode(Id.create("NodeA", Node.class), scen.createCoord(100, 100));
		Node b = nw.createNode(Id.create("NodeB", Node.class), scen.createCoord(5000, 5000));
		Node u = nw.createNode(Id.create("NodeU", Node.class), scen.createCoord(99, 99));
		Node v = nw.createNode(Id.create("NodeV", Node.class), scen.createCoord(5001, 5001));
		Link ab = nw.createLink(Id.create("LinkAB", Link.class),a,b);
		Link ua = nw.createLink(Id.create("LinkUA", Link.class),u,a);
		Link bv = nw.createLink(Id.create("LinkBV", Link.class),b,v);
		Link ba = nw.createLink(Id.create("LinkBA", Link.class),b,a);
		Link au = nw.createLink(Id.create("LinkAU", Link.class),a,u);
		Link vb = nw.createLink(Id.create("LinkVB", Link.class),v,b);
		
		ab.setLength(5000);
		ba.setLength(5000);
		
		
		net.addNode(a);
		net.addNode(b);
		net.addNode(u);
		net.addNode(v);
		net.addLink(ab);
		net.addLink(ua);
		net.addLink(bv);
		net.addLink(ba);
		net.addLink(au);
		net.addLink(vb);
			
		HashSet<String> modes = new HashSet<String>();
		modes.add("car");
		modes.add("walk");
		for (Link l : net.getLinks().values())
		{
			l.setAllowedModes(modes);
		}
		
		/* *************** *
		 * Create Schedule *
		 * *************** */
		
		boolean blockLane = false;
		
		TransitSchedule schedule = scen.getTransitSchedule();
		TransitScheduleFactory sFac = schedule.getFactory();
		
		TransitStopFacility aStop = sFac.createTransitStopFacility(Id.create("AStop", TransitStopFacility.class), ua.getCoord(), blockLane);
		aStop.setLinkId(ua.getId());
		TransitStopFacility iStop = sFac.createTransitStopFacility(Id.create("IStop", TransitStopFacility.class), ab.getCoord(), blockLane);
		iStop.setLinkId(ab.getId());
		TransitStopFacility bStop = sFac.createTransitStopFacility(Id.create("BStop", TransitStopFacility.class), bv.getCoord(), blockLane);
		bStop.setLinkId(bv.getId());
		
		
		schedule.addStopFacility(aStop);
		schedule.addStopFacility(bStop);
		schedule.addStopFacility(iStop);
		
		TransitLine line1 = sFac.createTransitLine(Id.create("Line1", TransitLine.class));
		
		List<Id<Link>> routeIds1 = new LinkedList<Id<Link>>();
		routeIds1.add(ua.getId());
		routeIds1.add(ab.getId());
		routeIds1.add(bv.getId());
		
		List<TransitRouteStop> stops1 = new LinkedList<TransitRouteStop>();
		stops1.add(sFac.createTransitRouteStop(aStop, 0, 0));
		stops1.add(sFac.createTransitRouteStop(iStop, 2.1 * 3600, 2.1 * 3600));
		stops1.add(sFac.createTransitRouteStop(bStop, 4.2 * 3600, 4.2 * 3600));
		
		VehiclesFactory vFac = scen.getTransitVehicles().getFactory();
		
		NetworkRoute route1 = RouteUtils.createNetworkRoute(routeIds1, net);
		TransitRoute transitRoute1 = sFac.createTransitRoute(Id.create("l1r1", TransitRoute.class), route1, stops1, "pt");
		for (int t=1; t <= 20; t++)
		{
			Departure dep = sFac.createDeparture(Id.create("r1dep"+t, Departure.class), 3600 * t);
			
			VehicleCapacity cap = vFac.createVehicleCapacity();
			cap.setSeats(10);
			cap.setStandingRoom(20);
			VehicleType type = vFac.createVehicleType(Id.create("r1dep"+t+"type", VehicleType.class));
			type.setCapacity(cap);
			type.setMaximumVelocity(20);
			scen.getTransitVehicles().addVehicleType(type);
			Vehicle veh = vFac.createVehicle(Id.create("r1dep"+t+"veh", Vehicle.class), type);
			scen.getTransitVehicles().addVehicle( veh);
			dep.setVehicleId(veh.getId());
			
			
			transitRoute1.addDeparture(dep);
		}
		line1.addRoute(transitRoute1);
		
		schedule.addTransitLine(line1);
		
		/* ***************** *
		 * Create Population *
		 * ***************** */
		
		ObjectAttributes oa = new ObjectAttributes();
		Random ran = new Random(config.global().getRandomSeed());
		
		Population pop = scen.getPopulation();
		PopulationFactory popFac = pop.getFactory();
		
		ActivityImpl homeBegin = (ActivityImpl) popFac.createActivityFromLinkId("home", ua.getId());
		homeBegin.setCoord(ua.getCoord());
		homeBegin.setEndTime(7 * 3600);
		ActivityImpl homeEnd = (ActivityImpl) popFac.createActivityFromLinkId("home", bv.getId());
		homeEnd.setCoord(bv.getCoord());
		
		
		for (int t=0; t < numAgents; t++)
		{
			Person p = popFac.createPerson(Id.create("Agent"+t, Person.class));
			Plan plan = popFac.createPlan();
			plan.addActivity(homeBegin);
			plan.addLeg(popFac.createLeg("pt"));
			plan.addActivity(homeEnd);
			p.addPlan(plan);
			pop.addPerson(p);
			oa.putAttribute(p.getId().toString(), "agentProperties", AgentProperties.getRandom(ran));
		}
		
		
		/* ****************** *
		 * Initialize Scoring *
		 * ****************** */
		
		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(3600 * 12);	
		config.planCalcScore().addActivityParams(home);
		
		/* ******************* *
		 * Initialize Strategy *
		 * ******************* */
		
		StrategySettings stratSets = new StrategySettings(Id.create("1", StrategySettings.class));
		//stratSets.setModuleName("BestScore");
		stratSets.setStrategyName("SelectExpBeta");
		stratSets.setWeight(0.9);
		config.strategy().addStrategySettings(stratSets);
		stratSets = new StrategySettings(Id.create("2", StrategySettings.class));
		stratSets.setStrategyName("TransitTimeAllocationMutator");
		stratSets.setWeight(0.1);
		config.strategy().addStrategySettings(stratSets);
		
		/* ****************** *
		 * Return scenario... *
		 * ****************** */
		
		return scen;
		
	}

}
