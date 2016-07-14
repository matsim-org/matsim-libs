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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.router.TransitTravelDisutility;
import org.matsim.vehicles.Vehicle;

/**
 * @author droeder
 *
 */
public class TransitFareTravelDisutility implements TransitTravelDisutility {

	private static final Logger log = Logger
			.getLogger(TransitFareTravelDisutility.class);
	
	private TransitRouterNetworkTravelTimeAndDisutility disutility;
	private double margUtilMoney;
	private TicketMachine ticketMachine;

	public TransitFareTravelDisutility(final TransitRouterNetworkTravelTimeAndDisutility routerDisutility, 
			PlanCalcScoreConfigGroup planCalcScore, TicketMachineImpl ticketMachine) {
		this.disutility = routerDisutility; 
		this.margUtilMoney = planCalcScore.getMarginalUtilityOfMoney();
		this.ticketMachine = ticketMachine;
		
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle, CustomDataManager dataManager) {
		StringBuffer debugger = new StringBuffer();
		//get the default disutility
		Double disutilityOfTravelling = this.disutility.getLinkTravelDisutility(link, time, person, vehicle, dataManager);
		TransitRouterNetworkLink l = (TransitRouterNetworkLink) link;
		debugger.append("handling link " + ((TransitRouterNetworkNode) link.getFromNode()).getStop().getStopFacility().getId() 
				+ ">" + ((TransitRouterNetworkNode) link.getToNode()).getStop().getStopFacility().getId() + "\t");
		debugger.append("disutilityOfTravelling: " + disutilityOfTravelling + "\t");
		Object o = dataManager.getFromNodeCustomData();
		if(l.getLine() == null){
			// we're on a transit-link. Thus, there is no fare, but we have to store the old ticket again!
			dataManager.setToNodeCustomData(o);
			return disutilityOfTravelling;
		}
		// calculate the expected travelTime on the link
		Double expTravelTime = l.toNode.stop.getArrivalOffset() - l.fromNode.stop.getDepartureOffset();
		debugger.append("TransitLine: " + l.getLine().getId() + "\tTransitRoute: " + l.getRoute().getId() + "\t");
		// we're traveling on real pt, thus calculate the TransitFareDisutility
		Ticket t = null;
		if(o == null){
			// we're on the first real pt-leg within this trip. Thus, get a new ticket! 
			t = this.ticketMachine.getNewTicket(l.getRoute().getId(), l.getLine().getId(), person, time, expTravelTime);
			debugger.append("bought a new " + t.getType() + " without update" + "\t");
		}else {
			// we already bought a ticket
			t = (Ticket) o;
			// TODO implement the distance calculation
			Double travelledDistance = 0.;
			// check if it is valid
			if(!this.ticketMachine.isValid(t, l.getRoute().getId(), l.getLine().getId(), time, expTravelTime, travelledDistance)){
				// try to upgrade
				debugger.append("trying to upgrade " + t.getType() + "\t");
				t = this.ticketMachine.upgrade(t, l.getRoute().getId(), l.getLine().getId(), person, time, expTravelTime, travelledDistance);
				// if not possible, buy a new one
				if(t == null){
					t = this.ticketMachine.getNewTicket(l.getRoute().getId(), l.getLine().getId(), person, time, expTravelTime);
					debugger.append("no upgrade possible, bought " + t.getType() + "\t");
				}else{
					debugger.append("upgraded to " + t.getType() + "\t");
				}
			}else{
				debugger.append("using a valid " + t.getType() + "\t");
			}
		}
		//store the bought ticket
		dataManager.setToNodeCustomData(t);
		// and calculate the complete disutility
		Double disutilityOfFare = (this.margUtilMoney * t.getFare()); 
		debugger.append("fareDisutility: " + disutilityOfFare + "\t");
		debugger.append("time: " + time + "\t");
		log.debug(debugger.toString());
		return disutilityOfTravelling + disutilityOfFare;
	}
	
	public double getTravelDisutility(Person person, Coord coord, Coord toCoord) {
		return disutility.getTravelDisutility(person, coord, toCoord);
	}

	public double getTravelTime(Person person, Coord coord, Coord toCoord) {
		return disutility.getTravelTime(person, coord, toCoord);
	}
	
//	private static Double FARELINE1 = 0.;
//	private static Double FARELINE2 = 1.;
//	public static void main(String[] args) {
//		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		scenario.getConfig().transit().setUseTransit(true);
//		scenario.getConfig().planCalcScore().setUtilityOfLineSwitch(0);
//		scenario.getConfig().planCalcScore().setMarginalUtlOfWaitingPt_utils_hr(0.);
//		createTestSchedule(scenario);
//		
//		TransitRouterConfig config = new TransitRouterConfig(scenario.getConfig().planCalcScore(),
//				scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(),
//				scenario.getConfig().vspExperimental());
//		config.setBeelineWalkSpeed(0.1); // something very slow, so the agent does not walk over night
//		config.searchRadius = 200;
//		
//		TransitRouterNetworkTravelTimeAndDisutility transitRouterNetworkTravelTimeAndDisutility = new TransitRouterNetworkTravelTimeAndDisutility(config);
//		
//		TransitFareTravelDisutility disutility = new TransitFareTravelDisutility(transitRouterNetworkTravelTimeAndDisutility, 
//				scenario.getConfig().planCalcScore(), new TicketMachineImpl(scenario.getTransitSchedule().getTransitLinesAttributes()));
//		
//		TransitRouterNetwork transitNetwork = TransitRouterNetwork.createFromSchedule(scenario.getTransitSchedule(), config.beelineWalkConnectionDistance);
//
//		TransitRouterImpl router = new TransitRouterImpl(config, transitNetwork, transitRouterNetworkTravelTimeAndDisutility, disutility);
//		
//		List<Leg> legs = router.calcRoute(scenario.createCoord(0, 0), new CoordImpl(2000, 0), 6*3600, null);
//		System.out.println();
//		for(Leg l : legs){
//			System.out.println(l.toString());
//		}
//	}
//	
//	
//	/**
//	 * Creates the following test schedule:
//	 * 
//	 * 
//	 *      (2)       (4)
//	 *     /   \     /   \
//	 *    /     \   /     \
//	 *   /       \ /       \
//	 * (1)-------(3)-------(5)
//	 * 
//	 * line 1 traveling from 1 to 2 to 3 to 4 to 5
//	 * line 2 traveling from 1 to 3 to 5, slower from 1 to 3 than line 1, faster from 3 to 5 and from 5 to 7
//	 * 
//	 */
//	private static void createTestSchedule(final Scenario scenario) {
//		TransitSchedule schedule = scenario.getTransitSchedule();
//		TransitScheduleFactory f = schedule.getFactory();
//		
//		TransitStopFacility f1 = f.createTransitStopFacility(new IdImpl("1"), new CoordImpl(0, 0), false);
//		TransitStopFacility f2 = f.createTransitStopFacility(new IdImpl("2"), new CoordImpl(500, 500), false);
//		TransitStopFacility f3 = f.createTransitStopFacility(new IdImpl("3"), new CoordImpl(1000, 0), false);
//		TransitStopFacility f4 = f.createTransitStopFacility(new IdImpl("4"), new CoordImpl(1500, 500), false);
//		TransitStopFacility f5 = f.createTransitStopFacility(new IdImpl("5"), new CoordImpl(2000, 0), false);
//		
//		schedule.addStopFacility(f1);
//		schedule.addStopFacility(f2);
//		schedule.addStopFacility(f3);
//		schedule.addStopFacility(f4);
//		schedule.addStopFacility(f5);
//		
//		TransitLine line1 = f.createTransitLine(new IdImpl("1"));
//		schedule.getTransitLinesAttributes().putAttribute(line1.getId().toString(), SingleBoardingTicket.NAME,  FARELINE1);
//		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
//		stops.add(f.createTransitRouteStop(f1, Time.UNDEFINED_TIME, 0.0));
//		stops.add(f.createTransitRouteStop(f2, Time.UNDEFINED_TIME, 300.0));
//		stops.add(f.createTransitRouteStop(f3, Time.UNDEFINED_TIME, 600.0));
//		stops.add(f.createTransitRouteStop(f4, Time.UNDEFINED_TIME, 900.0));
//		stops.add(f.createTransitRouteStop(f5, 1200.0, Time.UNDEFINED_TIME));
//		TransitRoute route1 = f.createTransitRoute(new IdImpl("1"), null, stops, "pt");
//		line1.addRoute(route1);
//		schedule.addTransitLine(line1);
//		route1.addDeparture(f.createDeparture(new IdImpl("1"), 6.0*3600));
//		
//		TransitLine line2 = f.createTransitLine(new IdImpl("2"));
//		schedule.getTransitLinesAttributes().putAttribute(line2.getId().toString(), SingleBoardingTicket.NAME,  FARELINE2);
//		List<TransitRouteStop> stops2 = new ArrayList<TransitRouteStop>();
//		stops2.add(f.createTransitRouteStop(f1, Time.UNDEFINED_TIME, 0.0));
//		stops2.add(f.createTransitRouteStop(f3, Time.UNDEFINED_TIME, 750.0));
//		stops2.add(f.createTransitRouteStop(f5, 1100.0, Time.UNDEFINED_TIME));
//		TransitRoute route2 = f.createTransitRoute(new IdImpl("2"), null, stops2, "pt");
//		line2.addRoute(route2);
//		schedule.addTransitLine(line2);
//		route2.addDeparture(f.createDeparture(new IdImpl("2"), 6.0*3600 + 60));
//	}
}

