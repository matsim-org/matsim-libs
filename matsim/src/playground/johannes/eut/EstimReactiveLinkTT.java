/* *********************************************************************** *
 * project: org.matsim.*
 * EstimReactiveLinkTT.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.eut;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentWait2Link;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentWait2LinkI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.network.Link;
import org.matsim.plans.Person;
import org.matsim.router.util.TravelTimeI;

/**
 * @author illenberger
 *
 */
public class EstimReactiveLinkTT implements
		EventHandlerLinkEnterI,
		EventHandlerLinkLeaveI,
		EventHandlerAgentArrivalI,
		EventHandlerAgentWait2LinkI,
		TravelTimeI {
	
	private Map<BasicLinkI, LinkTTCalculator> linkTTCalculators;
	
	private BasicLinkI lastQueriedLink;
	
	private double lastQueryTime;
	
	private double lastTravelTime;
	
	public void reset(int iteration) {
		linkTTCalculators = new LinkedHashMap<BasicLinkI, LinkTTCalculator>();
	}
	
	public void handleEvent(EventLinkEnter event) {
		increaseCount((QueueLink) event.link, event.agent, event.time);
	}

	public void handleEvent(EventLinkLeave event) {
		decreaseCount((QueueLink) event.link, event.agent, event.time);
	}

	public void handleEvent(EventAgentArrival event) {
		decreaseCount((QueueLink) event.link, event.agent, event.time);
	}

	public void handleEvent(EventAgentWait2Link event) {
		increaseCount((QueueLink) event.link, event.agent, event.time);
	}
	
	private void increaseCount(QueueLink link, Person person, double time) {
		LinkTTCalculator f = linkTTCalculators.get(link);
		if(f == null) {
			f = new LinkTTCalculator(link);
			linkTTCalculators.put(link, f);
		}
		f.enterLink(person, time);
	}
	
	private void decreaseCount(QueueLink link, Person person, double time) {
		LinkTTCalculator f = linkTTCalculators.get(link);
		f.leaveLink(person, time);
	}

	public double getLinkTravelTime(Link link, double time) {
		double simtime = SimulationTimer.getTime();
		if (simtime == lastQueryTime && link == lastQueriedLink)
			return lastTravelTime;
		else {
			lastQueryTime = simtime;
			lastQueriedLink = link;
			
			LinkTTCalculator f = linkTTCalculators.get(link);
			if (f == null)
				lastTravelTime = link.getFreespeed();
			else
				/*
				 * TODO: This is ugly!
				 */
				lastTravelTime = f.getLinkTravelTime(simtime);
			
			return lastTravelTime;
		}
	}
	
	private class LinkTTCalculator {
		
		private final QueueLink link;
		
		private final double freeFlowTravTime;
		
		private int outCount = 0;
		
		private double lastEvent = 0;
		
		private double lastCall = 0;
		
		private double currentTravelTime;
		
		private double currentOutFlow;
		
		private double feasibleOutFlow;
		
		private SortedSet<Sample> samples;
		
		public LinkTTCalculator(QueueLink link) {
			samples = new TreeSet<Sample>();
			this.link = link;
			freeFlowTravTime = link.getFreeTravelDuration();
			currentTravelTime = freeFlowTravTime;
			feasibleOutFlow = link.getSimulatedFlowCapacity();
			currentOutFlow = feasibleOutFlow;
		}
		
		public void enterLink(Person person, double time) {
			samples.add(new Sample(person, time + freeFlowTravTime));
		}
		
		public void leaveLink(Person person, double time) {
			outCount++;
			
			Sample sample = null;
			/*
			 * Since we can expect that the person is near the head of the set,
			 * this should not be that expensive...
			 */
			for(Sample s : samples) {
				if(s.person.equals(person)) {
					sample = s;
					break;
				}
			}
			samples.remove(sample);
			
			double deltaT = time - lastEvent;
			if(deltaT > 0) {
				currentOutFlow = outCount/deltaT;
				lastEvent = time;
				outCount = 0;
				
				if(samples.isEmpty())
					feasibleOutFlow = link.getSimulatedFlowCapacity();
				else if(samples.first().linkLeaveTime > time)
					feasibleOutFlow = link.getSimulatedFlowCapacity();
				else
					feasibleOutFlow = currentOutFlow;
			}
		}
		
		public double getLinkTravelTime(double time) {
			if (time > lastCall) {
				lastCall = time;
				
				if (samples.isEmpty())
					currentTravelTime = link.getFreespeed();
				else {
					double tt = samples.size() / feasibleOutFlow;
					currentTravelTime = Math.max(freeFlowTravTime, tt);
				}
			}
			
			return currentTravelTime;
		}
	}
	
	private class Sample implements Comparable<Sample> {

		public Person person;
		
		public double linkLeaveTime;
		
		public Sample(Person person, double linkLeaveTime) {
			this.person = person;
			this.linkLeaveTime = linkLeaveTime;
		}

		public int compareTo(Sample o) {
			if(o == null)
				return 1;
			else {
				int result = Double.compare(linkLeaveTime, o.linkLeaveTime);
				if(result == 0)
					result = person.getId().compareTo(o.person.getId());
				
				return result;	
			}
		}
		
		
	}
	
//	private class TupleComparator implements Comparator<Tuple<Person, Double>> {
//
//		public int compare(Tuple<Person, Double> o1, Tuple<Person, Double> o2) {
//			if(o1 == null)
//				return -1;
//			else if(o2 == null)
//				return 1;
//			else
//				return o1.getSecond().compareTo(o2.getSecond());
//		}
//		
//	}
}
