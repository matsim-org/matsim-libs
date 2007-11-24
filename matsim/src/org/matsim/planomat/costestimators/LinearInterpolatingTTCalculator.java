/* *********************************************************************** *
 * project: org.matsim.*
 * LinearInterpolatingTTCalculator.java
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

package org.matsim.planomat.costestimators;

import java.util.HashMap;

import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.router.util.TravelTimeI;

/**
 * A link travel time calculator which returns a travel time that comes
 * from a piecewise linear interpolation of the time bin based constant
 * estimation. Mainly a copy of TravelTimeCalculator, should be reworked
 * in order to have nice source code!
 *
 * @author meisterk
 *
 */
public class LinearInterpolatingTTCalculator
implements EventHandlerLinkEnterI, EventHandlerLinkLeaveI, EventHandlerAgentArrivalI, TravelTimeI {

	// EnterEvent implements Comparable based on linkId and vehId. This means that the key-pair <linkId, vehId> must always be unique!
	private HashMap<EnterEvent, Double> enterEvents = new HashMap<EnterEvent, Double>();
	private NetworkLayer network = null;
	private int roleIndex;
	private int timeslice;

	static private class EnterEvent /*implements Comparable<EnterEvent>*/ {

		private String linkId;
		private String vehId;

		public EnterEvent(String linkId, String vehId) {
			this.linkId = linkId;
			this.vehId = vehId;
		}

		@Override
		public String toString() {
			return "[[linkId=" + this.linkId + "][vehId=" + this.vehId + "]]";
		}

		@Override
		public int hashCode() {
			/*
			 * There may be better values for the hashcode, but each vehicle should
			 * only be once or at most twice (in some rare cases where enter-link events
			 * are written before leave-link events, e.g. in distributed/parallel
			 * computations) at the same time in the hashmap. Thus the number of collisions
			 * is rather low by using vehID as the hashcode.
			 *
			 * variant: use (10*vehID_) + (int)floor(Math.random()*10) to further decrease possible collisions
			 */
			return this.vehId.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (o == null) return false;
			try {
				EnterEvent event = (EnterEvent)o;
				return (this.vehId.equals(event.vehId) && this.linkId.equals(event.linkId));
			}
			catch (ClassCastException e) {
				return false;	// we obviously cannot be equal if we're of different classes
			}
		}

/*		public int compareTo(EnterEvent event) {
			System.out.println("- Comparing events " + this + " to " + event);
			if (this.linkID_ == event.linkID_ && this.vehID_ == event.vehID_) {
				return 0;
			} else if (this.linkID_ < event.linkID_) {
				return -1;		//pseudo-order, compare linkIDs just that we have consistent results
			} else {
				return +1;
			}
		}
*/
	};

	private class LinearInterpolatingTravelTimeRole {
		private HashMap<Integer, Double> timeSum = null;	// map<timeslot-index, sum-of-travel-times>
		private HashMap<Integer, Integer> timeCnt = null;		// map<timeslot-index, count-of-travel-times>
		private Link link;

		public LinearInterpolatingTravelTimeRole(Link link) {
			this.link = link;
			this.timeSum = new HashMap<Integer, Double>();
		}

		private int getTimeSlotIndex(double time) {
			int slice = (int)(time/LinearInterpolatingTTCalculator.this.timeslice);
			return slice;
		}

		public void resetTravelTimes() {
			int nofSlots = ((27*3600)/LinearInterpolatingTTCalculator.this.timeslice);	// default number of slots
			this.timeSum = new HashMap<Integer, Double>(nofSlots);
			this.timeCnt = new HashMap<Integer, Integer>(nofSlots);
		}

		public void addTravelTime(double now, double traveltime) {
			Integer index = Integer.valueOf(getTimeSlotIndex(now));
			Double sum = this.timeSum.get(index);
			Integer cnt = this.timeCnt.get(index);
			if (null == sum) {
				sum = Double.valueOf(traveltime);
				cnt = Integer.valueOf(1);
			} else {
				sum += traveltime;
				cnt = Integer.valueOf(cnt.intValue() + 1);
			}
			this.timeSum.put(index, sum);
			this.timeCnt.put(index, cnt);
		}

		public double getTravelTime(double now) {

			double tTravelEstimation = 0.0;
			Double sumA, sumB;
			Integer cntA, cntB;

			double offset = LinearInterpolatingTTCalculator.this.timeslice / 2.0;

			int indexA = getTimeSlotIndex(now - offset);
			double xA = (indexA * LinearInterpolatingTTCalculator.this.timeslice) + offset;
			int indexB = indexA + 1;
			double xB = (indexB * LinearInterpolatingTTCalculator.this.timeslice) + offset;

			double tTravelA = getLinkMinimumTravelTime(this.link);
			// belehr mich eines besseren
			sumA = this.timeSum.get(indexA);
			if (sumA != null) {
				cntA = this.timeCnt.get(indexA);
				if (cntA != null) {
					tTravelA = (sumA.doubleValue() / cntA.doubleValue());
				}
			}

			double tTravelB = getLinkMinimumTravelTime(this.link);
			// belehr mich eines besseren
			sumB = this.timeSum.get(indexB);
			if (sumB != null) {
				cntB = this.timeCnt.get(indexB);
				if (cntB != null) {
					tTravelB = (sumB.doubleValue() / cntB.doubleValue());
				}
			}

			// linear interpolation

			double m = (tTravelB - tTravelA) / (xB - xA);
			tTravelEstimation = tTravelA + m * (now - xA);

			return tTravelEstimation;
		}

	};

	public LinearInterpolatingTTCalculator(NetworkLayer network, int timeslice) {
		super();
		this.network = network;
		this.timeslice = timeslice;
		this.roleIndex = network.requestLinkRole();
		resetTravelTimes();
	}

	public LinearInterpolatingTTCalculator(NetworkLayer network) {
		this(network, 15*60);
	}

	public void resetTravelTimes() {
		for (Link link : this.network.getLinks().values()) {
			getTravelTimeRole(link).resetTravelTimes();
		}
		this.enterEvents.clear();
	}

	//////////////////////////////////////////////////////////////////////
	// Implementation of EventAlgorithmI
	//////////////////////////////////////////////////////////////////////

	public void reset(int iteration) {
		resetTravelTimes();
	}

	public void handleEvent(EventLinkEnter event) {
		EnterEvent e = new EnterEvent(event.linkId, event.agentId);
		this.enterEvents.put(e, event.time);
	}

	public void handleEvent(EventLinkLeave event) {
		EnterEvent e = new EnterEvent(event.linkId, event.agentId);
		Double starttime = this.enterEvents.remove(e);
		if (starttime != null) {
			double timediff = event.time - starttime.intValue();
			if (timediff < 0) {
				Gbl.errorMsg("");
			}
			Link link = event.link;
			if (null == link) link = (Link)this.network.getLocation(event.linkId);
			if (null != link) {
				getTravelTimeRole(link).addTravelTime(starttime.intValue(), timediff);
			}
		}
	}

	public void handleEvent(EventAgentArrival event) {
		// remove EnterEvents from list when an agent arrives.
		// otherwise, the activity duration would counted as travel time, when the
		// agent departs again and leaves the link!
		EnterEvent e = new EnterEvent(event.linkId, event.agentId);
		this.enterEvents.remove(e);
	}


	public double getLinkTravelTime(Link link, double time) {
		return getTravelTimeRole(link).getTravelTime(time);
	}

	private LinearInterpolatingTravelTimeRole getTravelTimeRole(Link l) {
		LinearInterpolatingTravelTimeRole r = (LinearInterpolatingTravelTimeRole)l.getRole(this.roleIndex);
		if (null == r) {
			r = new LinearInterpolatingTravelTimeRole(l);
			l.setRole(this.roleIndex, r);
		}
		return r;
	}

	private double getLinkMinimumTravelTime(Link link) {
		return (link.getLength() / link.getFreespeed());
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return this.getClass().getSimpleName();
	}

}
