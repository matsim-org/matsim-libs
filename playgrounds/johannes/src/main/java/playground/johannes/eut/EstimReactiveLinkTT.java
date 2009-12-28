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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.mobsim.queuesim.SimulationTimer;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.router.util.TravelTime;

/**
 * @author illenberger
 *
 */
public class EstimReactiveLinkTT implements
		LinkEnterEventHandler,
		LinkLeaveEventHandler,
		AgentArrivalEventHandler,
		AgentWait2LinkEventHandler,
		TravelTime {

//	private QueueNetwork queueNetwork;
	
	private double capacityFactor;
	
	private Map<Id, LinkTTCalculator> linkTTCalculators;

	private Link lastQueriedLink;

	private double lastQueryTime;

	private double lastTravelTime;
	
	private final Network network;

	public EstimReactiveLinkTT(double scenarioScale, Network network) {
		capacityFactor = scenarioScale;
		this.network = network;
	}
	
	public void reset(int iteration) {
		this.linkTTCalculators = new LinkedHashMap<Id, LinkTTCalculator>();
	}

	public void handleEvent(LinkEnterEvent event) {
		increaseCount(event.getLinkId(), event.getPersonId(), (int) event.getTime());
	}

	public void handleEvent(LinkLeaveEvent event) {
		decreaseCount(event.getLinkId(), event.getPersonId(), (int) event.getTime());
	}

	public void handleEvent(AgentArrivalEvent event) {
		decreaseCount(event.getLinkId(), event.getPersonId(), (int) event.getTime());
	}

	public void handleEvent(AgentWait2LinkEvent event) {
		increaseCount(event.getLinkId(), event.getPersonId(), (int) event.getTime());
	}

	private void increaseCount(Id linkId, Id personId, int time) {
		LinkTTCalculator f = this.linkTTCalculators.get(linkId);
		if(f == null) {
			f = new LinkTTCalculator(this.network.getLinks().get(linkId));
			this.linkTTCalculators.put(linkId, f);
		}
		f.enterLink(personId, time);
	}

	private void decreaseCount(Id linkId, Id personId, int time) {
		LinkTTCalculator f = this.linkTTCalculators.get(linkId);
		f.leaveLink(personId, time);
	}

	public double getLinkTravelTime(Link link, double time) {
		int simtime = (int) SimulationTimer.getTime();
		if ((simtime == this.lastQueryTime) && (link == this.lastQueriedLink))
			return this.lastTravelTime;
		else {
			this.lastQueryTime = simtime;
			this.lastQueriedLink = link;

			LinkTTCalculator f = this.linkTTCalculators.get(link.getId());
			if (f == null)
				this.lastTravelTime = link.getLength()/link.getFreespeed(simtime);
			else
				/*
				 * TODO: This is ugly!
				 */
				this.lastTravelTime = f.getLinkTravelTime(simtime);

			return this.lastTravelTime;
		}
	}

	private class LinkTTCalculator {

		private final Link link;

//		private final double freeFlowTravTime;

		private int outCount = 0;

		private int lastEvent = 0;

		private int lastCall = 0;

		private double currentTravelTime;

//		private double currentOutFlow;

		private double feasibleOutFlow;
		

		private SortedSet<Sample> samples;

		public LinkTTCalculator(Link link) {
//			this.qLink = queueNetwork.getQueueLink(link.getId());
			this.link = link;
			this.samples = new TreeSet<Sample>();
//			this.freeFlowTravTime = link.getFreespeedTravelTime(Time.UNDEFINED_TIME);
//			this.currentTravelTime = this.freeFlowTravTime;
			this.feasibleOutFlow = Double.NaN;//link.getFlowCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME) * capacityFactor;
//			this.currentOutFlow = this.feasibleOutFlow;
		}

		public void enterLink(Id personId, int time) {
			this.samples.add(new Sample(personId, (int) Math.ceil(time + ((LinkImpl) link).getFreespeedTravelTime(time))));
		}

		public void leaveLink(Id personId, int time) {
			this.outCount++;

			Sample sample = null;
			/*
			 * Since we can expect that the person is near the head of the set,
			 * this should not be that expensive...
			 */
			for(Sample s : this.samples) {
				if(s.personId.equals(personId)) {
					this.samples.remove(s);
					sample = s;
					break;
				}
			}
//			this.samples.remove(sample);

			int deltaT = time - this.lastEvent;
			this.lastEvent = time;
			if(deltaT > 10) {
//				this.currentOutFlow = this.outCount/(double)deltaT;
				
				this.outCount = 0;

				if(this.samples.isEmpty())
					this.feasibleOutFlow = ((LinkImpl) this.link).getFlowCapacity(time) * capacityFactor;
//				else if(this.samples.first().linkLeaveTime > time)
				else if(sample.linkLeaveTime >= time)
					this.feasibleOutFlow = ((LinkImpl) this.link).getFlowCapacity(time) * capacityFactor;
				else
					this.feasibleOutFlow = this.outCount/(double)deltaT;
			}
		}

		public double getLinkTravelTime(int time) {
			if (time > this.lastCall) {
				this.lastCall = time;
				double traveltime;
//				if (this.samples.isEmpty())
//					traveltime = link.getLength() /
//											 link.getFreespeed(time);
//				else {
				double tt = Double.NaN;
				if(Double.isNaN(feasibleOutFlow)) {
					tt = samples.size() / ((LinkImpl) link).getFlowCapacity(time) * capacityFactor;
				} else {
					tt = this.samples.size() / this.feasibleOutFlow;
				}
				traveltime = Math.max(((LinkImpl) link).getFreespeedTravelTime(time), tt);
//				}
				currentTravelTime = traveltime;
			}

			return currentTravelTime;
		}
	}

	private class Sample implements Comparable<Sample> {

		public Id personId;

		public int linkLeaveTime;

		public Sample(Id personId, int linkLeaveTime) {
			this.personId = personId;
			this.linkLeaveTime = linkLeaveTime;
		}

		public int compareTo(Sample o) {
			if(o == null)
				return 1;
			else {
				int result = Double.compare(this.linkLeaveTime, o.linkLeaveTime);
				if(result == 0)
					result = this.personId.compareTo(o.personId);

				return result;
			}
		}


	}
}
