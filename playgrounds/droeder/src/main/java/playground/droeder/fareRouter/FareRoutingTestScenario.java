/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.droeder.fareRouter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author droeder
 *
 */
class FareRoutingTestScenario {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(FareRoutingTestScenario.class);

	
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		// take into account only in-veh-time and money 
		scenario.getConfig().planCalcScore().setUtilityOfLineSwitch(0.);
		scenario.getConfig().planCalcScore().setTravelingWalk_utils_hr(0.00000000001);
		scenario.getConfig().planCalcScore().setMarginalUtlOfWaitingPt_utils_hr(0.);
		// and that 1:1
		scenario.getConfig().planCalcScore().setTravelingPt_utils_hr(3600.);
		scenario.getConfig().planCalcScore().setMarginalUtilityOfMoney(1.);
		
		
		createSchedule(scenario);
		createPersons(scenario);
		
		TransitRouterConfig config = new TransitRouterConfig(scenario.getConfig().planCalcScore(),
				scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(),
				scenario.getConfig().vspExperimental());
		config.setBeelineWalkSpeed(0.000001); // something very slow, so the agent does not walk over night
		config.setAdditionalTransferTime(0);
		config.setSearchRadius(501.);
		config.setExtensionRadius(0);
		
		
		@SuppressWarnings("deprecation")
		TransitRouterNetworkTravelTimeAndDisutility transitRouterNetworkTravelTimeAndDisutility = new TransitRouterNetworkTravelTimeAndDisutility(config);
		
		Map<String, Double> ticketType2fare = new HashMap<String, Double>();
		ticketType2fare.put(ThirtyMinTicket.NAME, new Double(1800.));
		ticketType2fare.put(TwoHourTicket.NAME, new Double(3598.));
		
		TicketFactory ticketFactory = new MyTicketFactory(scenario.getTransitSchedule().getTransitLinesAttributes(), ticketType2fare);
		
		TransitFareTravelDisutility disutility = new TransitFareTravelDisutility(transitRouterNetworkTravelTimeAndDisutility, 
				scenario.getConfig().planCalcScore(), new TicketMachineImpl(ticketFactory, scenario.getTransitSchedule().getTransitLinesAttributes()));
		
		TransitRouterNetwork transitNetwork = TransitRouterNetwork.createFromSchedule(scenario.getTransitSchedule(), config.getBeelineWalkConnectionDistance());
		
		TransitRouterImpl router = new TransitRouterImpl(config, new PreparedTransitSchedule(scenario.getTransitSchedule()), transitNetwork, transitRouterNetworkTravelTimeAndDisutility, disutility);
		
		for(Person p : scenario.getPopulation().getPersons().values()){
			Plan plan = p.getSelectedPlan();
			System.out.println("person " + p.getId().toString() + " planned to go ");
			Coord from, to;
			Double departureTime;
			from = ((Activity) plan.getPlanElements().get(0)).getCoord();
			to = ((Activity) plan.getPlanElements().get(2)).getCoord();
			departureTime = ((Leg) plan.getPlanElements().get(1)).getDepartureTime();
			System.out.println("from " + from.toString()  + " to "+ to.toString() + " at " + departureTime);
			System.out.println(p.getCustomAttributes().toString());
			System.out.println();
			System.out.println("he realized:");
			
			for(Leg leg : router.calcRoute(from, to, departureTime, p)){
				System.out.println(leg.toString());
			}
			System.out.println();
			System.out.println();
		}
		
	}
/**
	 * @param scenario
	 */
	private static void createPersons(Scenario scenario) {
		Population population = scenario.getPopulation();
		PopulationFactory factory = scenario.getPopulation().getFactory();
		
		Person person;
		Plan plan;
		Leg leg ;
		
		person = factory.createPerson(Id.create("1", Person.class));
		plan = factory.createPlan();
		plan.addActivity(factory.createActivityFromCoord("h", scenario.createCoord(0., 0.)));
		leg = factory.createLeg("pt");
		leg.setDepartureTime(6*3600);
		plan.addLeg(leg);
		plan.addActivity(factory.createActivityFromCoord("h", scenario.createCoord(3500., 0.)));
		person.addPlan(plan);
		person.getCustomAttributes().put(MyTicketFactory.USEFLATRATE, new Boolean(true));
		population.addPerson(person);
		
		person = factory.createPerson(Id.create("2", Person.class));
		plan = factory.createPlan();
		plan.addActivity(factory.createActivityFromCoord("h", scenario.createCoord(0., 0.)));
		leg = factory.createLeg("pt");
		leg.setDepartureTime(6*3600);
		plan.addLeg(leg);
		plan.addActivity(factory.createActivityFromCoord("h", scenario.createCoord(3500., 0.)));
		person.addPlan(plan);
		person.getCustomAttributes().put(MyTicketFactory.USEFLATRATE, new Boolean(false));
		population.addPerson(person);
		
	}


	/**
	 * @param scenario
	 */
	private static void createSchedule(Scenario scenario) {
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory f = schedule.getFactory();
		
		TransitStopFacility f1 = f.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new CoordImpl(0, 0), false);
		TransitStopFacility f2 = f.createTransitStopFacility(Id.create("2", TransitStopFacility.class), new CoordImpl(500, 0), false);
		TransitStopFacility f3 = f.createTransitStopFacility(Id.create("3", TransitStopFacility.class), new CoordImpl(1000, 0), false);
		TransitStopFacility f4 = f.createTransitStopFacility(Id.create("4", TransitStopFacility.class), new CoordImpl(1500, 0), false);
		TransitStopFacility f5 = f.createTransitStopFacility(Id.create("5", TransitStopFacility.class), new CoordImpl(2000, 0), false);
		TransitStopFacility f6 = f.createTransitStopFacility(Id.create("6", TransitStopFacility.class), new CoordImpl(2500, 0), false);
		TransitStopFacility f7 = f.createTransitStopFacility(Id.create("7", TransitStopFacility.class), new CoordImpl(3000, 0), false);
		TransitStopFacility f8 = f.createTransitStopFacility(Id.create("8", TransitStopFacility.class), new CoordImpl(3500, 0), false);
		
		schedule.addStopFacility(f1);
		schedule.addStopFacility(f2);
		schedule.addStopFacility(f3);
		schedule.addStopFacility(f4);
		schedule.addStopFacility(f5);
		schedule.addStopFacility(f6);
		schedule.addStopFacility(f7);
		schedule.addStopFacility(f8);
		
		
		Double time = 6 * 3600.;
		Double offset = 1800.;
		String lineId;
		TransitLine line;
		TransitRoute r1, r2;
		List<TransitRouteStop> stops;
		Double fareSingleBoarding;
		
		lineId ="1";
		fareSingleBoarding = 1799.;
		line = f.createTransitLine(Id.create(lineId, TransitLine.class));
		stops = new ArrayList<TransitRouteStop>();
		stops.add(f.createTransitRouteStop(f1, Time.UNDEFINED_TIME, 0.0));
		stops.add(f.createTransitRouteStop(f2, offset, Time.UNDEFINED_TIME));
		
		r1 = f.createTransitRoute(Id.create(lineId + ".1", TransitRoute.class), null, stops, "pt");
		r1.addDeparture(f.createDeparture(Id.create("1", Departure.class), time));
		schedule.getTransitLinesAttributes().putAttribute(r1.getId().toString(), SingleBoardingTicket.NAME, fareSingleBoarding);
		line.addRoute(r1);
		
		stops = new ArrayList<TransitRouteStop>();
		stops.add(f.createTransitRouteStop(f1, Time.UNDEFINED_TIME, 0.0));
		stops.add(f.createTransitRouteStop(f2, offset, Time.UNDEFINED_TIME));
		r2 = f.createTransitRoute(Id.create(lineId + ".2", TransitRoute.class), null, stops, "pt");
		r2.addDeparture(f.createDeparture(Id.create("1", Departure.class), time));
		line.addRoute(r2);
		
		schedule.getTransitLinesAttributes().putAttribute(r1.getId().toString(), TicketMachineImpl.ALLOWEDTICKETS, FlatRate.NAME + "," + SingleBoardingTicket.NAME);
		schedule.getTransitLinesAttributes().putAttribute(r2.getId().toString(), TicketMachineImpl.ALLOWEDTICKETS, ThirtyMinTicket.NAME + "," + TwoHourTicket.NAME);
		schedule.addTransitLine(line);
		
		
		time += offset;
		lineId ="2";
		fareSingleBoarding = 1801.;
		line = f.createTransitLine(Id.create(lineId, TransitLine.class));
		stops = new ArrayList<TransitRouteStop>();
		stops.add(f.createTransitRouteStop(f2, Time.UNDEFINED_TIME, 0.0));
		stops.add(f.createTransitRouteStop(f3, offset, Time.UNDEFINED_TIME));
		
		r1 = f.createTransitRoute(Id.create(lineId + ".1", TransitRoute.class), null, stops, "pt");
		r1.addDeparture(f.createDeparture(Id.create("1", Departure.class), time));
		schedule.getTransitLinesAttributes().putAttribute(r1.getId().toString(), SingleBoardingTicket.NAME, fareSingleBoarding);
		line.addRoute(r1);
		
		stops = new ArrayList<TransitRouteStop>();
		stops.add(f.createTransitRouteStop(f2, Time.UNDEFINED_TIME, 0.0));
		stops.add(f.createTransitRouteStop(f3, offset, Time.UNDEFINED_TIME));
		r2 = f.createTransitRoute(Id.create(lineId + ".2", TransitRoute.class), null, stops, "pt");
		r2.addDeparture(f.createDeparture(Id.create("1", Departure.class), time));
		line.addRoute(r2);
		
		schedule.getTransitLinesAttributes().putAttribute(r1.getId().toString(), TicketMachineImpl.ALLOWEDTICKETS, FlatRate.NAME + "," + SingleBoardingTicket.NAME);
		schedule.getTransitLinesAttributes().putAttribute(r2.getId().toString(), TicketMachineImpl.ALLOWEDTICKETS, ThirtyMinTicket.NAME + "," + TwoHourTicket.NAME);
		schedule.addTransitLine(line);
		
		
		time += offset;
		lineId ="3";
		fareSingleBoarding =1799.;
		line = f.createTransitLine(Id.create(lineId, TransitLine.class));
		stops = new ArrayList<TransitRouteStop>();
		stops.add(f.createTransitRouteStop(f3, Time.UNDEFINED_TIME, 0.0));
		stops.add(f.createTransitRouteStop(f4, offset, Time.UNDEFINED_TIME));
		
		r1 = f.createTransitRoute(Id.create(lineId + ".1", TransitRoute.class), null, stops, "pt");
		r1.addDeparture(f.createDeparture(Id.create("1", Departure.class), time));
		schedule.getTransitLinesAttributes().putAttribute(r1.getId().toString(), SingleBoardingTicket.NAME, fareSingleBoarding);
		line.addRoute(r1);
		
		stops = new ArrayList<TransitRouteStop>();
		stops.add(f.createTransitRouteStop(f3, Time.UNDEFINED_TIME, 0.0));
		stops.add(f.createTransitRouteStop(f4, offset, Time.UNDEFINED_TIME));
		r2 = f.createTransitRoute(Id.create(lineId + ".2", TransitRoute.class), null, stops, "pt");
		r2.addDeparture(f.createDeparture(Id.create("1", Departure.class), time));
		line.addRoute(r2);
		
		schedule.getTransitLinesAttributes().putAttribute(r1.getId().toString(), TicketMachineImpl.ALLOWEDTICKETS, FlatRate.NAME + "," + SingleBoardingTicket.NAME);
		schedule.getTransitLinesAttributes().putAttribute(r2.getId().toString(), TicketMachineImpl.ALLOWEDTICKETS, ThirtyMinTicket.NAME + "," + TwoHourTicket.NAME);
		schedule.addTransitLine(line);
		
		
		time += offset;
		lineId ="4";
		fareSingleBoarding = 1799.;
		line = f.createTransitLine(Id.create(lineId, TransitLine.class));
		stops = new ArrayList<TransitRouteStop>();
		stops.add(f.createTransitRouteStop(f4, Time.UNDEFINED_TIME, 0.0));
		stops.add(f.createTransitRouteStop(f5, offset, Time.UNDEFINED_TIME));
		
		r1 = f.createTransitRoute(Id.create(lineId + ".1", TransitRoute.class), null, stops, "pt");
		r1.addDeparture(f.createDeparture(Id.create("1", Departure.class), time));
		schedule.getTransitLinesAttributes().putAttribute(r1.getId().toString(), SingleBoardingTicket.NAME, fareSingleBoarding);
		line.addRoute(r1);
		
		stops = new ArrayList<TransitRouteStop>();
		stops.add(f.createTransitRouteStop(f4, Time.UNDEFINED_TIME, 0.0));
		stops.add(f.createTransitRouteStop(f5, offset, Time.UNDEFINED_TIME));
		r2 = f.createTransitRoute(Id.create(lineId + ".2", TransitRoute.class), null, stops, "pt");
		r2.addDeparture(f.createDeparture(Id.create("1", Departure.class), time));
		line.addRoute(r2);
		
		schedule.getTransitLinesAttributes().putAttribute(r1.getId().toString(), TicketMachineImpl.ALLOWEDTICKETS, SingleBoardingTicket.NAME);
		schedule.getTransitLinesAttributes().putAttribute(r2.getId().toString(), TicketMachineImpl.ALLOWEDTICKETS, ThirtyMinTicket.NAME + "," + TwoHourTicket.NAME);
		schedule.addTransitLine(line);
		
		
		time += offset;
		lineId ="5";
		fareSingleBoarding = 1799.;
		line = f.createTransitLine(Id.create(lineId, TransitLine.class));
		stops = new ArrayList<TransitRouteStop>();
		stops.add(f.createTransitRouteStop(f5, Time.UNDEFINED_TIME, 0.0));
		stops.add(f.createTransitRouteStop(f6, offset, Time.UNDEFINED_TIME));
		
		r1 = f.createTransitRoute(Id.create(lineId + ".1", TransitRoute.class), null, stops, "pt");
		r1.addDeparture(f.createDeparture(Id.create("1", Departure.class), time));
		schedule.getTransitLinesAttributes().putAttribute(r1.getId().toString(), SingleBoardingTicket.NAME, fareSingleBoarding);
		line.addRoute(r1);
		
		stops = new ArrayList<TransitRouteStop>();
		stops.add(f.createTransitRouteStop(f5, Time.UNDEFINED_TIME, 0.0));
		stops.add(f.createTransitRouteStop(f6, offset, Time.UNDEFINED_TIME));
		r2 = f.createTransitRoute(Id.create(lineId + ".2", TransitRoute.class), null, stops, "pt");
		r2.addDeparture(f.createDeparture(Id.create("1", Departure.class), time));
		line.addRoute(r2);
		
		schedule.getTransitLinesAttributes().putAttribute(r1.getId().toString(), TicketMachineImpl.ALLOWEDTICKETS, FlatRate.NAME + "," + SingleBoardingTicket.NAME);
		schedule.getTransitLinesAttributes().putAttribute(r2.getId().toString(), TicketMachineImpl.ALLOWEDTICKETS, ThirtyMinTicket.NAME + "," + TwoHourTicket.NAME);
		schedule.addTransitLine(line);
		
		
		time += offset;
		lineId ="6";
		fareSingleBoarding = 1799.;
		line = f.createTransitLine(Id.create(lineId, TransitLine.class));
		stops = new ArrayList<TransitRouteStop>();
		stops.add(f.createTransitRouteStop(f6, Time.UNDEFINED_TIME, 0.0));
		stops.add(f.createTransitRouteStop(f7, offset, Time.UNDEFINED_TIME));
		
		stops = new ArrayList<TransitRouteStop>();
		stops.add(f.createTransitRouteStop(f6, Time.UNDEFINED_TIME, 0.0));
		stops.add(f.createTransitRouteStop(f7, offset, Time.UNDEFINED_TIME));
		r1 = f.createTransitRoute(Id.create(lineId + ".1", TransitRoute.class), null, stops, "pt");
		r1.addDeparture(f.createDeparture(Id.create("1", Departure.class), time));
		schedule.getTransitLinesAttributes().putAttribute(r1.getId().toString(), SingleBoardingTicket.NAME, fareSingleBoarding);
		line.addRoute(r1);
		
		r2 = f.createTransitRoute(Id.create(lineId + ".2", TransitRoute.class), null, stops, "pt");
		r2.addDeparture(f.createDeparture(Id.create("1", Departure.class), time));
		line.addRoute(r2);
		
		schedule.getTransitLinesAttributes().putAttribute(r1.getId().toString(), TicketMachineImpl.ALLOWEDTICKETS, FlatRate.NAME + "," + SingleBoardingTicket.NAME);
		schedule.getTransitLinesAttributes().putAttribute(r2.getId().toString(), TicketMachineImpl.ALLOWEDTICKETS, ThirtyMinTicket.NAME + "," + TwoHourTicket.NAME);
		schedule.addTransitLine(line);
		
		
		time += offset;
		lineId ="7";
		fareSingleBoarding = 1801.;
		line = f.createTransitLine(Id.create(lineId, TransitLine.class));
		stops = new ArrayList<TransitRouteStop>();
		stops.add(f.createTransitRouteStop(f7, Time.UNDEFINED_TIME, 0.0));
		stops.add(f.createTransitRouteStop(f8, offset, Time.UNDEFINED_TIME));
		
		r1 = f.createTransitRoute(Id.create(lineId + ".1", TransitRoute.class), null, stops, "pt");
		r1.addDeparture(f.createDeparture(Id.create("1", Departure.class), time));
		schedule.getTransitLinesAttributes().putAttribute(r1.getId().toString(), SingleBoardingTicket.NAME, fareSingleBoarding);
		line.addRoute(r1);
		
		stops = new ArrayList<TransitRouteStop>();
		stops.add(f.createTransitRouteStop(f7, Time.UNDEFINED_TIME, 0.0));
		stops.add(f.createTransitRouteStop(f8, offset, Time.UNDEFINED_TIME));
		r2 = f.createTransitRoute(Id.create(lineId + ".2", TransitRoute.class), null, stops, "pt");
		r2.addDeparture(f.createDeparture(Id.create("1", Departure.class), time));
		line.addRoute(r2);
		
		schedule.getTransitLinesAttributes().putAttribute(r1.getId().toString(), TicketMachineImpl.ALLOWEDTICKETS, SingleBoardingTicket.NAME);
		schedule.getTransitLinesAttributes().putAttribute(r2.getId().toString(), TicketMachineImpl.ALLOWEDTICKETS, ThirtyMinTicket.NAME + "," + TwoHourTicket.NAME);
		schedule.addTransitLine(line);
	}
}

