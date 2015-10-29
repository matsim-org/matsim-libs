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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;

/**
 * This class basically contains a static class that generates a scenario that represents the
 * simplistic case of the El Farol Train Game.
 * 
 * @author pcbouman
 *
 */

public class ElFarolTrainScenario
{

	public static MutableScenario generateScenario()
	{
		return generateScenario(400,10,3, 12 * 60, 20, 20);
	}
	
	public static MutableScenario generateScenario(int numAgents, int timeSlots, int intervalLength, double headWay, double popFrac)
	{
		
		int importantSlots = timeSlots - intervalLength + 1;
		int capacity = (int)Math.ceil((importantSlots + numAgents * popFrac)  / importantSlots);
		
		
		
		return generateScenario(numAgents, timeSlots, intervalLength, headWay, capacity/2, capacity/2 );
		
	}
	
	public static MutableScenario generateScenario(int numAgents, int timeSlots, int intervalLength, double headWay, int seatCap, int standCap)
	{
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		
		config.controler().setLastIteration(1000);
		config.controler().setWriteEventsInterval(50);
		config.controler().setWritePlansInterval(50);
		
		HashSet<String> transitModes = new HashSet<String>();
		transitModes.add("pt");
		config.transit().setTransitModes(transitModes);
		MutableScenario scen = (MutableScenario) ScenarioUtils.createScenario(config);

		
		/* ******** *
		 * Settings *
		 * ******** */
		
		double longDist = 5000;
		double boxSize = 1000;
		double ySpan = 100;
		
		double speed = 20;
		double waitBuffer = 60;
		
		int numVehicles = 4;
		
		double startTime = 7 * 3600;
		int departures = (int)Math.floor((3600 * 24 - startTime) / headWay); 
		
		double workDuration = 8 * 3600;
		
		double ptChance = 1;
		
		boolean blockLane = false;
		
		/* ************** *
		 * Create Network *
		 * ************** */
		Network net = scen.getNetwork();
		NetworkFactory nw = net.getFactory();
		
		Node a, b, c, u, v, w;

		Node [] nodes = new Node [] {
				a = nw.createNode(Id.create("a", Node.class), new Coord(boxSize / 2, boxSize / 2 - ySpan)),
				b = nw.createNode(Id.create("b", Node.class), new Coord(boxSize / 2, boxSize / 2 + ySpan)),
				c = nw.createNode(Id.create("c", Node.class), new Coord(boxSize / 2, boxSize / 2)),
		
				u = nw.createNode(Id.create("u", Node.class), new Coord(longDist + boxSize / 2, boxSize / 2 - ySpan)),
				v = nw.createNode(Id.create("v", Node.class), new Coord(longDist + boxSize / 2, boxSize / 2 + ySpan)),
				w = nw.createNode(Id.create("w", Node.class), new Coord(longDist + boxSize / 2, boxSize / 2)),
		};
		
		for (Node n : nodes)
		{
			net.addNode(n);
		}
		
		Link ac, ca, cb, bc, uw, wu, vw, wv, wc, cw;
		
		Link [] links = new Link [] {
				ac = nw.createLink(Id.create("ac", Link.class), a, c),
				ca = nw.createLink(Id.create("ca", Link.class), c, a),
				cb = nw.createLink(Id.create("cb", Link.class), c, b),
				bc = nw.createLink(Id.create("bc", Link.class), b, c),
		
				uw = nw.createLink(Id.create("uw", Link.class), u, w),
				wu = nw.createLink(Id.create("wu", Link.class), w, u),
				vw = nw.createLink(Id.create("vw", Link.class), v, w),
				wv = nw.createLink(Id.create("wv", Link.class), w, v),
		
				wc = nw.createLink(Id.create("wc", Link.class), w, c),
				cw = nw.createLink(Id.create("cw", Link.class), c, w),
		};
		
		for (Link l : links)
		{
			setDist(l);
			l.setFreespeed(speed);
			l.setCapacity(2);
			net.addLink(l);
		}
		
		/* *************** *
		 * Create Schedule *
		 * *************** */
		
		TransitSchedule schedule = scen.getTransitSchedule();
		TransitScheduleFactory sFac = schedule.getFactory();
				
		TransitStopFacility [] stops = new TransitStopFacility [] {
				sFac.createTransitStopFacility(Id.create("stop1", TransitStopFacility.class), cb.getCoord(), blockLane),
				sFac.createTransitStopFacility(Id.create("stop2", TransitStopFacility.class), wv.getCoord(), blockLane),
				sFac.createTransitStopFacility(Id.create("stop3", TransitStopFacility.class), uw.getCoord(), blockLane),
				sFac.createTransitStopFacility(Id.create("stop4", TransitStopFacility.class), ca.getCoord(), blockLane)
		};
		
		stops[0].setLinkId(cb.getId());
		stops[1].setLinkId(wv.getId());
		stops[2].setLinkId(uw.getId());
		stops[3].setLinkId(ca.getId());
		
		for (TransitStopFacility stop : stops)
		{
			schedule.addStopFacility(stop);
		}
		
		TransitLine line = sFac.createTransitLine(Id.create("Line1", TransitLine.class));
		
		List<Id<Link>> routeIds = Arrays.asList(
				ac.getId(),
				cb.getId(),
				bc.getId(),
				cw.getId(),
				wv.getId(),
				vw.getId(),
				wu.getId(),
				uw.getId(),
				wc.getId(),
				ca.getId()
		);
		
		List<TransitRouteStop> stopList = Arrays.asList(
				sFac.createTransitRouteStop(stops[0], 0, waitBuffer),
				sFac.createTransitRouteStop(stops[1], (2* ySpan + longDist)/speed, waitBuffer + (2 * ySpan + longDist)/speed),
				sFac.createTransitRouteStop(stops[2], (5* ySpan + longDist)/speed, waitBuffer + (5 * ySpan + longDist)/speed),
				sFac.createTransitRouteStop(stops[3], (6* ySpan + 2*longDist)/speed, waitBuffer + (6 * ySpan + 2*longDist)/speed)
				
		);
		
		
		
		VehiclesFactory vFac = scen.getTransitVehicles().getFactory();
		
		Vehicle [] vehicles = new Vehicle[numVehicles];
		for (int t=0; t < numVehicles; t++)
		{
			VehicleCapacity cap = vFac.createVehicleCapacity();
			cap.setSeats(seatCap);
			cap.setStandingRoom(standCap);
			VehicleType type = vFac.createVehicleType(Id.create("vtype"+t, VehicleType.class));
			type.setCapacity(cap);
			type.setMaximumVelocity(speed);
			scen.getTransitVehicles().addVehicleType( type);
			Vehicle veh = vFac.createVehicle(Id.create("vehicle"+t, Vehicle.class), type);
			scen.getTransitVehicles().addVehicle( veh);
			vehicles[t] = veh;
		}
		
		NetworkRoute route = RouteUtils.createNetworkRoute(routeIds, net);
		TransitRoute transitRoute = sFac.createTransitRoute(Id.create("route", TransitRoute.class), route, stopList, "pt");
		
		for (int t=0; t < departures; t++)
		{
			Departure dep = sFac.createDeparture(Id.create("dep"+t, Departure.class), startTime + t*headWay);
			dep.setVehicleId(vehicles[t % vehicles.length].getId());
			transitRoute.addDeparture(dep);
		}
		
		line.addRoute(transitRoute);
		schedule.addTransitLine(line);
		
		/* ************************** *
		 * Create Agents & Activities *
		 * ************************** */

		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(3600 * 12);	
		config.planCalcScore().addActivityParams(home);

		ActivityParams [] workP = new ActivityParams[timeSlots - intervalLength + 1];
		for (int t=0; t < timeSlots - intervalLength + 1; t++)
		{
			workP[t] = new ActivityParams("work"+t);
			workP[t].setOpeningTime(startTime + longDist / speed + t * headWay);
			workP[t].setLatestStartTime(workP[t].getOpeningTime() + intervalLength * headWay);
			workP[t].setEarliestEndTime(workP[t].getOpeningTime() + workDuration);
			workP[t].setClosingTime(workP[t].getLatestStartTime() + workDuration);
			workP[t].setTypicalDuration(workDuration);
			config.planCalcScore().addActivityParams(workP[t]);
		}
		
		Random ran = MatsimRandom.getRandom();
		Population pop = scen.getPopulation();
		PopulationFactory popFac = pop.getFactory();

		for (int t=0; t < numAgents; t++)
		{
			int index = ran.nextInt(workP.length);
			String mode = ran.nextDouble() < ptChance ? "pt" : "car";
			
			Person p = popFac.createPerson(Id.create("Agent"+t, Person.class));
			Plan plan = popFac.createPlan();

			ActivityImpl homeAct = (ActivityImpl) popFac.createActivityFromCoord("home", new Coord(ran.nextDouble() * boxSize, ran.nextDouble() * boxSize));
			homeAct.setEndTime(workP[index].getOpeningTime() - longDist/speed - headWay);
			plan.addActivity(homeAct);
			plan.addLeg(popFac.createLeg(mode));

			ActivityImpl workAct = (ActivityImpl) popFac.createActivityFromCoord("work"+index,
					new Coord(longDist + boxSize + ran.nextDouble() * boxSize, ran.nextDouble() * boxSize));
			workAct.setEndTime(workP[index].getEarliestEndTime());
			plan.addActivity(workAct);
			plan.addLeg(popFac.createLeg(mode));
			
			Activity homeAct2 = popFac.createActivityFromCoord("home", homeAct.getCoord());
			plan.addActivity(homeAct2);
			
			p.addPlan(plan);
			pop.addPerson(p);
		}
		
		/* ************** *
		 * Setup Strategy *
		 * ************** */
		
		StrategySettings stratSets = new StrategySettings(Id.create("1", StrategySettings.class));
		//stratSets.setModuleName("BestScore");
		stratSets.setStrategyName("SelectExpBeta");
		stratSets.setWeight(0.8);
		config.strategy().addStrategySettings(stratSets);
		stratSets = new StrategySettings(Id.create("2", StrategySettings.class));
		stratSets.setStrategyName("TransitTimeAllocationMutator");
		stratSets.setWeight(0.2);
		config.strategy().addStrategySettings(stratSets);
		//stratSets = new StrategySettings(Id.create("3"));
		//stratSets.setModuleName("ChangeLegMode");
		//stratSets.setProbability(0.025);
		//config.strategy().addStrategySettings(stratSets);
		
		return scen;		
	}
	
	private static void setDist(Link l)
	{
		Coord f = l.getFromNode().getCoord();
		Coord t = l.getToNode().getCoord();
		double xDiff = f.getX() - t.getX();
		double yDiff = f.getY() - t.getY();
		l.setLength( Math.sqrt( xDiff*xDiff + yDiff*yDiff));
	}
}
