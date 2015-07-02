/* *********************************************************************** *
 * project: org.matsim.*
 * RideToRidePassengerAgentIdentifier.java
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

package playground.christoph.passenger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.multimodal.router.util.WalkTravelTime;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDeparture;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.misc.Counter;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.InitialIdentifier;

import playground.christoph.passenger.RideToRidePassengerContextProvider.RideToRidePassengerContext;
import playground.christoph.tools.PersonAgentComparator;

/**
 * This class replaces agent's ride trips with ride_passenger trips.
 * To do so, car trips that are near to the planned ride trip are searched.
 * If a car trip is found, JointDeparture objects are created. If not,
 * the mode of the agent's trip is changed to walk or pt, depending on the distance.
 * 
 * TODO: ensure that departure time and travel time for the ride legs are set.
 * 
 * @author cdobler
 */
public class RideToRidePassengerAgentIdentifier extends InitialIdentifier {

	private static final Logger log = Logger.getLogger(RideToRidePassengerAgentIdentifier.class);
	
	private final double maxWalkTime = 900;
	private final double maxWaitTime = 300;
	private final int timBinSize = 300;
	
	private final Counter matchingCarTrips = new Counter("Found ride legs that can be assinged to car legs: ");
	private final Counter needAlternativeMode = new Counter("Found ride legs that have to be converted to other modes: ");
	
	private final Network network;
	private final MobsimDataProvider mobsimDataProvider;
	private final RideToRidePassengerContextProvider rideToRidePassengerContextProvider;
	private final JointDepartureOrganizer jointDepartureOrganizer;
	private final WithinDayAgentUtils withinDayAgentUtils;
	
	private TravelTime carTravelTime = new FreeSpeedTravelTime();
	private TravelTime walkTravelTime = new WalkTravelTime(new PlansCalcRouteConfigGroup());
	
	public RideToRidePassengerAgentIdentifier(Network network, MobsimDataProvider mobsimDataProvider,
			RideToRidePassengerContextProvider rideToRidePassengerContextProvider, 
			JointDepartureOrganizer jointDepartureOrganizer) {
		this.network = network;
		this.mobsimDataProvider = mobsimDataProvider;
		this.rideToRidePassengerContextProvider = rideToRidePassengerContextProvider;
		this.jointDepartureOrganizer = jointDepartureOrganizer;
		this.withinDayAgentUtils = new WithinDayAgentUtils();
	}
	
	@Override
	public Set<MobsimAgent> getAgentsToReplan(double time) {

		this.rideToRidePassengerContextProvider.reset();
		
		this.matchingCarTrips.reset();
		this.needAlternativeMode.reset();
		
		Collection<MobsimAgent> mobsimAgents = new LinkedHashSet<MobsimAgent>(this.mobsimDataProvider.getAgents().values()); 
		Set<MobsimAgent> agentsToReplan = new TreeSet<MobsimAgent>(new PersonAgentComparator());

		for (MobsimAgent mobsimAgent : mobsimAgents) {
	
			Collection<Leg> rideLegs = getModeLegs(mobsimAgent, TransportMode.ride);
			
			if (rideLegs.size() > 0) agentsToReplan.add(mobsimAgent);
		}
		
		log.info("Found " + agentsToReplan.size() + " agents performing a ride trip.");
		
		log.info("Collecting agent leave link times...");
		Map<Id, Map<Integer, List<LinkLeft>>> linkLeaveMap = calculateExpectedLinkLeaveTimes(mobsimAgents, agentsToReplan);
		log.info("done.");
		
		identifyMatchingCarLegs(agentsToReplan, linkLeaveMap);
		
		this.matchingCarTrips.printCounter();
		this.needAlternativeMode.printCounter();
		
		return agentsToReplan;
	}
	
	private Collection<Leg> getModeLegs(MobsimAgent agent, String mode) {
		
		List<Leg> modeLegs = new ArrayList<Leg>();
		for (PlanElement planElement : this.withinDayAgentUtils.getModifiablePlan(agent).getPlanElements()) {
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (leg.getMode().equals(mode)) modeLegs.add(leg);
			}
		}
		return modeLegs;
	}

	private Map<Id, Map<Integer, List<LinkLeft>>> calculateExpectedLinkLeaveTimes(Collection<MobsimAgent> mobsimAgents, Set<MobsimAgent> agentsToReplan) {
		
		Map<Id, Map<Integer, List<LinkLeft>>> map = new HashMap<Id, Map<Integer, List<LinkLeft>>>();
		
		for (MobsimAgent mobsimAgent : mobsimAgents) {
	
			// skip agents with a ride trip
			if (agentsToReplan.contains(mobsimAgent)) continue;
			
			Person person = this.withinDayAgentUtils.getModifiablePlan(mobsimAgent).getPerson();
			
			Collection<Leg> carLegs = getModeLegs(mobsimAgent, TransportMode.car);
			for (Leg leg : carLegs) {
				double time = leg.getDepartureTime();
				NetworkRoute route = (NetworkRoute) leg.getRoute();
				List<Id<Link>> linkIds = new ArrayList<>();
				linkIds.addAll(route.getLinkIds());
				if (linkIds.size() > 0 || !route.getStartLinkId().equals(route.getEndLinkId())) linkIds.add(route.getEndLinkId());
				
				this.getLinkLeftList(time, route.getStartLinkId(), map).add(new LinkLeft(time, mobsimAgent, leg));
				
				for (Id<Link> linkId : linkIds) {
					Link link = this.network.getLinks().get(linkId);
					double linkTravelTime = this.carTravelTime.getLinkTravelTime(link, time, person, null);
					time += linkTravelTime;
					this.getLinkLeftList(time, route.getStartLinkId(), map).add(new LinkLeft(time, mobsimAgent, leg));
				}
			}
		}
//		int count = 0;
//		for (Map<Integer, List<LinkLeft>> m : map.values()) {
//			for (List<LinkLeft> list : m.values()) {
//				count += list.size();
//			}
//		}
//		log.info("\tFound " + count + " expected link left times.");
		
		return map;
	}
	
	private int getBin(double time) {
		int bin = ((int) time) / this.timBinSize;
		return bin;
	}
	
	private List<LinkLeft> getLinkLeftList(double time, Id<Link> linkId, Map<Id, Map<Integer, List<LinkLeft>>> map) {
		
		Map<Integer, List<LinkLeft>> linkMap = map.get(linkId);
		if (linkMap == null) {
			linkMap = new HashMap<Integer, List<LinkLeft>>();
			map.put(linkId, linkMap);
		}
		
		int bin = getBin(time);
		List<LinkLeft> binList = linkMap.get(bin);
		if (binList == null) {
			binList = new ArrayList<LinkLeft>();
			linkMap.put(bin, binList);
		}
		return binList;
	}
	
	private void identifyMatchingCarLegs(Set<MobsimAgent> agents, Map<Id, Map<Integer, List<LinkLeft>>> linkLeaveMap) {

		int jdCounter = 0;
		
		/*
		 * We cannot adapt a car leg more than once without increasing
		 * the complexity dramatically. Therefore, we remember those legs,
		 * which are going to be adapted.
		 */
		Set<Leg> adaptedCarLegs = new HashSet<Leg>();
		
		for (MobsimAgent agent : agents) {
			
			Person person = this.withinDayAgentUtils.getModifiablePlan(agent).getPerson();
			Collection<Leg> rideLegs = getModeLegs(agent, TransportMode.ride);
			
			for (Leg rideLeg : rideLegs) {
	
				Route route = rideLeg.getRoute();
				
				// get potential pickup links
				Link fromLink = this.network.getLinks().get(route.getStartLinkId());
				Map<Id, Double> fromTimes = new HashMap<Id, Double>();
				PriorityQueue<Link> fromLinks = new PriorityQueue<Link>(10, new LinkTimesComparator(fromTimes));
				fromTimes.put(fromLink.getId(), 0.0);
				fromLinks.add(fromLink);
				expandToNode(fromLink.getToNode(), 0.0, fromTimes, fromLinks, person, rideLeg.getDepartureTime());
				
				// get potential drop off links
				Link toLink = this.network.getLinks().get(route.getEndLinkId());
				Map<Id, Double> toTimes = new HashMap<Id, Double>();
				PriorityQueue<Link> toLinks = new PriorityQueue<Link>(10, new LinkTimesComparator(toTimes));
				toTimes.put(toLink.getId(), 0.0);
				toLinks.add(toLink);
				expandFromNode(fromLink.getToNode(), toLink.getLength(), toTimes, toLinks, person, rideLeg.getDepartureTime() + rideLeg.getTravelTime());
				
//				log.info("\tFound " + fromLinks.size() + " potential start links and " + toLinks.size() + " potential end links for ride leg.");
				
				// create tuple of <PotentialFromLink, PotentialToLink> and sort them by the total walk distance
				PriorityQueue<LinkTuple> linkTuples = new PriorityQueue<LinkTuple>(10, new LinkTupleComparator());
				Iterator<Link> fromIter = fromLinks.iterator();
				while (fromIter.hasNext()) {
					Link from = fromIter.next();
					
					// check whether the link allows car
					if (from.getAllowedModes() != null && !from.getAllowedModes().contains(TransportMode.car)) continue;
					
					Iterator<Link> toIter = toLinks.iterator();
					while (toIter.hasNext()) {
						Link to = toIter.next();
						
						// check whether the link allows car
						if (to.getAllowedModes() != null && !to.getAllowedModes().contains(TransportMode.car)) continue;
						
						LinkTuple linkTuple = new LinkTuple(from, to, fromTimes.get(from.getId()), toTimes.get(to.getId())); 
						linkTuples.add(linkTuple);
					}
				}
				
//				log.info("\tChecking " + linkTuples.size() + " potential link tuples.");
				
				RideToRidePassengerContext context = this.rideToRidePassengerContextProvider.createAndAddContext(rideLeg);
				boolean foundMatch = false;
				while (linkTuples.peek() != null && !foundMatch) {
					LinkTuple linkTuple = linkTuples.poll();
					
					double potentialPickupTime = rideLeg.getDepartureTime() + linkTuple.toWalkTime;
					PriorityQueue<PotentialCarLeg> potentialCarLegs = getPotentialCarLegs(potentialPickupTime, linkTuple.fromLink, linkTuple.toLink, linkLeaveMap.get(fromLink.getId()));
					
//					log.info("\t\tFound " + potentialCarLegs.size() + " potential car legs for the pickup.");
					
					PotentialCarLeg potentialCarLeg = checkPotentialLegs(potentialCarLegs, linkTuple.fromLink, linkTuple.toLink, adaptedCarLegs);
					if (potentialCarLeg != null) {
						this.matchingCarTrips.incCounter();
						context.carLeg = potentialCarLeg.leg;
						context.carLegAgent = potentialCarLeg.carAgent;
						context.pickupLink = linkTuple.fromLink;
						context.dropOffLink = linkTuple.toLink;		
						
						// Mark leg as to be adapted.
						adaptedCarLegs.add(potentialCarLeg.leg);
						
						// schedule joint departure for pickup
//						Id id, Id linkId, Id vehicleId, Id driverId, Collection<Id> passengerIds
						Id driverId = context.carLegAgent.getId();
						Id vehicleId = ((NetworkRoute) context.carLeg.getRoute()).getVehicleId();
						if (vehicleId == null) vehicleId = driverId;
						Id linkId = context.pickupLink.getId();
						Set<Id<Person>> passengerIds = new LinkedHashSet<>();
						passengerIds.add(agent.getId());
						context.pickupDeparture = this.jointDepartureOrganizer.createJointDeparture(Id.create("jd" + jdCounter++, JointDeparture.class), linkId, vehicleId, driverId, passengerIds);
						
						// schedule joint departure for drop off
						linkId = context.dropOffLink.getId();
						passengerIds = new LinkedHashSet<>();
						
						foundMatch = true;
					}
				}
				
				this.rideToRidePassengerContextProvider.addContext(agent.getId(), context);
				if (!foundMatch) this.needAlternativeMode.incCounter(); 
			}		
		}
	}
	
	private PotentialCarLeg checkPotentialLegs(PriorityQueue<PotentialCarLeg> potentialCarLegs, Link fromLink, Link toLink, Set<Leg> adaptedCarLegs) {
		
		while (potentialCarLegs.peek() != null) {
			PotentialCarLeg potentialCarLeg = potentialCarLegs.poll();
			
			// Do not adapt car legs more than once.
			if (adaptedCarLegs.contains(potentialCarLeg.leg)) continue;
			
			NetworkRoute route = (NetworkRoute) potentialCarLeg.leg.getRoute();
			List<Id> linkIds = new ArrayList<Id>();
			linkIds.add(route.getStartLinkId());
			linkIds.addAll(route.getLinkIds());
			linkIds.add(route.getEndLinkId());
			
			int fromIndex = linkIds.indexOf(fromLink.getId());
			int toIndex = linkIds.indexOf(toLink.getId());
			
			// both links have to be part of the car route
			if (toIndex < 0 || fromIndex < 0) continue;
			
			// if the route passes the toLink after the fromLink, it is a positive match
			if (toIndex >= fromIndex) return potentialCarLeg;
		}
		return null;
	}
	
	private PriorityQueue<PotentialCarLeg> getPotentialCarLegs(double pickupTime, Link fromLink, Link toLink, Map<Integer, List<LinkLeft>> linkTimeBinMap) {
		
		int bin = getBin(pickupTime);
		PriorityQueue<PotentialCarLeg> potentialLegs = new PriorityQueue<PotentialCarLeg>(10, new PotentialCarLegComparator());
		
		if (linkTimeBinMap == null) return potentialLegs;
		
		// check time bin before
		List<LinkLeft> before = linkTimeBinMap.get(bin - 1);
		List<LinkLeft> exact = linkTimeBinMap.get(bin);
		List<LinkLeft> after = linkTimeBinMap.get(bin + 1);
		
		if (before != null) {
			for (LinkLeft linkLeft : before) {
				double waitTime = Math.abs(linkLeft.time - pickupTime); 
				if (waitTime < maxWaitTime) potentialLegs.add(new PotentialCarLeg(linkLeft.agent, linkLeft.leg, waitTime));
			}
		}
		if (exact != null) {
			for (LinkLeft linkLeft : exact) {
				double waitTime = Math.abs(linkLeft.time - pickupTime); 
				if (waitTime < maxWaitTime) potentialLegs.add(new PotentialCarLeg(linkLeft.agent, linkLeft.leg, waitTime));
			}
		}
		if (after != null) {			
			for (LinkLeft linkLeft : after) {
				double waitTime = Math.abs(linkLeft.time - pickupTime); 
				if (waitTime < maxWaitTime) potentialLegs.add(new PotentialCarLeg(linkLeft.agent, linkLeft.leg, waitTime));
			}
		}
		
		return potentialLegs;
	}
	
	private void expandToNode(Node toNode, double walkTime, Map<Id, Double> walkTimes, PriorityQueue<Link> links, Person person, double departureTime) {
		
		for (Link outLink : toNode.getOutLinks().values()) {
			double newWalkTime = walkTime + this.walkTravelTime.getLinkTravelTime(outLink, departureTime + walkTime, person, null);
			if (newWalkTime < this.maxWalkTime) {
				
				// if the link is new or a shorter path has been found
				Double oldWalkTime = walkTimes.get(outLink.getId());
				if (oldWalkTime == null || newWalkTime < oldWalkTime) {
					walkTimes.put(outLink.getId(), newWalkTime);
					links.add(outLink);
					expandToNode(outLink.getToNode(), newWalkTime, walkTimes, links, person, departureTime);
				}
			}
		}
	}
	
	private void expandFromNode(Node fromNode, double walkTime, Map<Id, Double> walkTimes, PriorityQueue<Link> links, Person person, double arrivalTime) {
		
		for (Link inLink : fromNode.getOutLinks().values()) {
			double newWalkTime = walkTime + this.walkTravelTime.getLinkTravelTime(inLink, arrivalTime - walkTime, person, null);
			if (newWalkTime < this.maxWalkTime) {
				
				// if the link is new or a shorter path has been found
				Double oldWalkTime = walkTimes.get(inLink.getId());
				if (oldWalkTime == null || newWalkTime < oldWalkTime) {
					walkTimes.put(inLink.getId(), newWalkTime);
					links.add(inLink);
					expandFromNode(inLink.getFromNode(), newWalkTime, walkTimes, links, person, arrivalTime);
				}
			}
		}
	}
	
	private static class LinkTimesComparator implements Comparator<Link> {
		
		private final Map<Id, Double> times;
		
		public LinkTimesComparator(Map<Id, Double> times) {
			this.times = times;
		}

		@Override
		public int compare(Link l1, Link l2) {
			return Double.compare(this.times.get(l1.getId()), this.times.get(l2.getId()));
		}
	}
	
	private static class PotentialCarLeg {
		
		private final MobsimAgent carAgent;
		private final Leg leg;
		private final double waitTime;
		
		public PotentialCarLeg(MobsimAgent carAgent, Leg leg, double waitTime) {
			this.carAgent = carAgent;
			this.leg = leg;
			this.waitTime = waitTime;
		}
	}
	
	private static class PotentialCarLegComparator implements Comparator<PotentialCarLeg> {
		
		@Override
		public int compare(PotentialCarLeg pl1, PotentialCarLeg pl2) {
			int cmp = Double.compare(pl1.waitTime, pl2.waitTime);
			if (cmp == 0) return pl1.carAgent.getId().compareTo(pl2.carAgent.getId()); 
			else return cmp;
		}
	}
	
	private static class LinkTuple {
		
		private final Link fromLink;
		private final Link toLink;
		private final double fromWalkTime;
		private final double toWalkTime;
		private final double walkTime;
		
		public LinkTuple(Link fromLink, Link toLink, double fromWalkTime, double toWalkTime) {
			this.fromLink = fromLink;
			this.toLink = toLink;
			this.fromWalkTime = fromWalkTime;
			this.toWalkTime = toWalkTime;
			this.walkTime = fromWalkTime + toWalkTime;
		}
		
	}
	
	private static class LinkTupleComparator implements Comparator<LinkTuple> {
		
		@Override
		public int compare(LinkTuple lt1, LinkTuple lt2) {
			int cmp = Double.compare(lt1.walkTime, lt2.walkTime);
			if (cmp == 0) cmp = lt1.fromLink.getId().compareTo(lt2.fromLink.getId());
			if (cmp == 0) return lt1.toLink.getId().compareTo(lt2.toLink.getId()); 
			else return cmp;
		}
	}
	
	private static class LinkLeft {

		private final double time;
		private final MobsimAgent agent;
		private final Leg leg;
		
		public LinkLeft(double time, MobsimAgent agent, Leg leg) {
			this.time = time;
			this.agent = agent;
			this.leg = leg;
		}		
	}
}
