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

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.events.BasicAgentArrivalEvent;
import org.matsim.api.basic.v01.events.BasicLinkEnterEvent;
import org.matsim.api.basic.v01.events.BasicLinkLeaveEvent;
import org.matsim.api.basic.v01.events.handler.BasicAgentArrivalEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkEnterEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkLeaveEventHandler;
import org.matsim.core.api.experimental.network.Link;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;

/**
 * A link travel time calculator which returns a travel time that comes
 * from a piecewise linear interpolation of the time bin based constant
 * estimation. Mainly a copy of TravelTimeCalculator, should be reworked
 * in order to have nice source code!
 *
 * TODO move linear interpolation of travel times out of planomat package,
 * into org.matsim.trafficmonitoring.TravelTime*
 *
 * @author meisterk
 *
 */
public class LinearInterpolatingTTCalculator
implements BasicLinkEnterEventHandler, BasicLinkLeaveEventHandler, BasicAgentArrivalEventHandler, TravelTime {

	// EnterEvent implements Comparable based on linkId and vehId. This means that the key-pair <linkId, vehId> must always be unique!
	private final HashMap<EnterEvent, Double> enterEvents = new HashMap<EnterEvent, Double>();
	private NetworkLayer network = null;
	private final HashMap<Id, LinearInterpolatingTravelTimeData> linkData;
	private final int timeslice;

	static private class EnterEvent /*implements Comparable<EnterEvent>*/ {

		private final Id linkId;
		private final Id vehId;

		public EnterEvent(final Id linkId, final Id vehId) {
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
			 * is rather low by using vehId as the hashcode.
			 *
			 * variant: use (10*vehId) + (int)floor(Gbl.random.nextDouble()*10) to further decrease possible collisions
			 */
			return this.vehId.hashCode();
		}

		@Override
		public boolean equals(final Object o) {
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
	}

	private class LinearInterpolatingTravelTimeData {
		private HashMap<Integer, Double> timeSum = null;	// map<timeslot-index, sum-of-travel-times>
		private HashMap<Integer, Integer> timeCnt = null;		// map<timeslot-index, count-of-travel-times>
		private final LinkImpl link;

		public LinearInterpolatingTravelTimeData(final LinkImpl link) {
			this.link = link;
			this.timeSum = new HashMap<Integer, Double>();
		}

		private int getTimeSlotIndex(final double time) {
			int slice = (int)(time/LinearInterpolatingTTCalculator.this.timeslice);
			return slice;
		}

		public void resetTravelTimes() {
			int nofSlots = ((27*3600)/LinearInterpolatingTTCalculator.this.timeslice);	// default number of slots
			this.timeSum = new HashMap<Integer, Double>(nofSlots);
			this.timeCnt = new HashMap<Integer, Integer>(nofSlots);
		}

		public void addTravelTime(final double now, final double traveltime) {
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

		public double getTravelTime(final double now) {

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

	}

	public LinearInterpolatingTTCalculator(final NetworkLayer network, final int timeslice) {
		super();
		this.network = network;
		this.timeslice = timeslice;
		this.linkData = new HashMap<Id, LinearInterpolatingTravelTimeData>(network.getLinks().size());
		resetTravelTimes();
	}

	public LinearInterpolatingTTCalculator(final NetworkLayer network) {
		this(network, 15*60);
	}

	public void resetTravelTimes() {
		for (LinkImpl link : this.network.getLinks().values()) {
			getTravelTimeRole(link.getId()).resetTravelTimes();
		}
		this.enterEvents.clear();
	}


	public void reset(final int iteration) {
		resetTravelTimes();
	}

	public void handleEvent(final BasicLinkEnterEvent event) {
		EnterEvent e = new EnterEvent(event.getLinkId(), event.getPersonId());
		this.enterEvents.put(e, event.getTime());
	}

	public void handleEvent(final BasicLinkLeaveEvent event) {
		EnterEvent e = new EnterEvent(event.getLinkId(), event.getPersonId());
		Double starttime = this.enterEvents.remove(e);
		if (starttime != null) {
			double timediff = event.getTime() - starttime.intValue();
			if (timediff < 0) {
				Gbl.errorMsg("");
			}
			getTravelTimeRole(event.getLinkId()).addTravelTime(starttime.intValue(), timediff);
		}
	}

	public void handleEvent(final BasicAgentArrivalEvent event) {
		// remove EnterEvents from list when an agent arrives.
		// otherwise, the activity duration would counted as travel time, when the
		// agent departs again and leaves the link!
		EnterEvent e = new EnterEvent(event.getLinkId(), event.getPersonId());
		this.enterEvents.remove(e);
	}


	public double getLinkTravelTime(final Link link, final double time) {
		return getTravelTimeRole(link.getId()).getTravelTime(time);
	}
	
	private LinearInterpolatingTravelTimeData getTravelTimeRole(final Id linkId) {
		LinearInterpolatingTravelTimeData r = this.linkData.get(linkId);
		if (null == r) {
			r = new LinearInterpolatingTravelTimeData(this.network.getLinks().get(linkId));
			this.linkData.put(linkId, r);
		}
		return r;
	}

	private double getLinkMinimumTravelTime(final LinkImpl link) {
		return (link.getLength() / link.getFreespeed(Time.UNDEFINED_TIME));
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

}
