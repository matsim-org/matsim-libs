/* *********************************************************************** *
 * project: org.matsim.*
 * VolvoAnalysis.java
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

package playground.marcel;

/*
 * $Id$
 */

/* *********************************************************************** *
 * org.matsim.playground.marcel
 * VolvoAnalysis.java
 * -----------------
 * copyright       : (C) 2007 by Michael Balmer, Marcel Rieser,            *
 *                   David Strippgen, Gunnar Flötteröd, Konrad Meister,    *
 *                   Kai Nagel, Kay W. Axhausen                            *
 *                   Technische Universitaet Berlin (TU-Berlin) and        *
 *                   Swiss Federal Institute of Technology Zurich (ETHZ)   *
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.utils.identifiers.IdI;

/**
 * @author mrieser
 * @author dgrether
 *
 */
public class VolvoAnalysis implements EventHandlerLinkEnterI,
		EventHandlerLinkLeaveI, EventHandlerAgentDepartureI, EventHandlerAgentArrivalI {
	/**
	 * Number of timesteps used
	 */
	public static final int TIMESTEPS = 24;

	private NetworkLayer network = null;

//	private Collection<Link> hundekopf = null;

//	private Collection<Link> gemarkung = null;

	private Set<IdI> hundekopfLinkIds;

	private Set<IdI> gemarkungLinkIds;

	private double [] distHundekopf = new double[TIMESTEPS];

	private double [] distGemarkung = new double[TIMESTEPS];

	private double [] distRest = new double[TIMESTEPS];

	private double [] timeHundekopf = new double[TIMESTEPS];

	private double [] timeGemarkung = new double[TIMESTEPS];

	private double [] timeRest = new double[TIMESTEPS];

	private Map<String, Double> depTimes = new HashMap<String, Double>();

	private int[] tripsHundekopf = new int[TIMESTEPS];

	private int[] tripsGemarkung = new int[TIMESTEPS];

	private int[] tripsRest = new int[TIMESTEPS];

	public VolvoAnalysis(NetworkLayer network, RoadPricingScheme hundekopf,
			RoadPricingScheme gemarkung) {
		this.network = network;
//		this.hundekopf = hundekopf.getLinks();
//		this.gemarkung = gemarkung.getLinks();
		this.hundekopfLinkIds = new HashSet<IdI>(hundekopf.getLinkIds());
		this.gemarkungLinkIds = new HashSet<IdI>(gemarkung.getLinkIds());
	}

	/**
   *
   * @param seconds
   * @return The hour of the day corresponding to the given seconds. 0 for 0-1
   *         am, 1 for 1-2 am...
   */
	private int getTimestep(double seconds) {
		return (int) seconds / (60 * 60 * 24 / TIMESTEPS);
	}

	public void handleEvent(EventLinkEnter event) {
		Link link = event.link;
		int hour = getTimestep(event.time);
		if (link == null) {
			link = this.network.getLink(event.linkId);
		}
		if (this.hundekopfLinkIds.contains(link.getId())) {
			this.distHundekopf[hour] += link.getLength();
		}
		else if (this.gemarkungLinkIds.contains(link.getId())) {
			this.distGemarkung[hour] += link.getLength();
		}
		else {
			this.distRest[hour] += link.getLength();
		}

		this.depTimes.put(event.agentId, Double.valueOf(event.time));
	}

	public void handleEvent(EventLinkLeave event) {
		Double depTime = this.depTimes.put(event.agentId, Double.valueOf(-1.0));
		int hour = getTimestep(event.time);
		if (depTime == null)
			return;
		if (depTime.doubleValue() == -1.0)
			return;

		Link link = event.link;
		if (link == null) {
			link = this.network.getLink(event.linkId);
		}
		if (this.hundekopfLinkIds.contains(link.getId())) {
			this.timeHundekopf[hour] += (event.time - depTime.doubleValue());
		}
		else if (this.gemarkungLinkIds.contains(link.getId())) {
			this.timeGemarkung[hour] += (event.time - depTime.doubleValue());
		}
		else {
			this.timeRest[hour] += (event.time - depTime.doubleValue());
		}
	}

	public void handleEvent(EventAgentArrival event) {
		Double depTime = this.depTimes.put(event.agentId, Double.valueOf(-1.0));
		int hour = getTimestep(event.time);
		if (depTime == null)
			return;
		if (depTime.doubleValue() == -1.0)
			return;

		Link link = event.link;
		if (link == null) {
			link = this.network.getLink(event.linkId);
		}
		if (this.hundekopfLinkIds.contains(link.getId())) {
			this.timeHundekopf[hour] += (event.time - depTime.doubleValue());
		}
		else if (this.gemarkungLinkIds.contains(link.getId())) {
			this.timeGemarkung[hour] += (event.time - depTime.doubleValue());
		}
		else {
			this.timeRest[hour] += (event.time - depTime.doubleValue());
		}
	}

	public void handleEvent(EventAgentDeparture event) {
		Link link = event.link;
		int hour = getTimestep(event.time);
		if (link == null) {
			link = this.network.getLink(event.linkId);
		}
		if (this.hundekopfLinkIds.contains(link.getId())) {
			this.tripsHundekopf[hour]++;
		}
		else if (this.gemarkungLinkIds.contains(link.getId())) {
			this.tripsGemarkung[hour]++;
		}
		else  {
			this.tripsRest[hour]++;
		}
	}

	public void reset(int iteration) {
		this.distHundekopf = new double[TIMESTEPS];
		this.distGemarkung = new double[TIMESTEPS];
		this.distRest = new double[TIMESTEPS];
		this.timeHundekopf = new double[TIMESTEPS];
		this.timeGemarkung = new double[TIMESTEPS];
		this.timeRest = new double[TIMESTEPS];
		this.tripsGemarkung = new int[TIMESTEPS];
		this.tripsHundekopf = new int[TIMESTEPS];
		this.tripsRest = new int[TIMESTEPS];
	}

	public double getDistHundekopf(int timestep) {
		return this.distHundekopf[timestep];
	}
	public double getDistGemarkung(int timestep) {
		return this.distGemarkung[timestep];
	}
	public double getDistRest(int timestep) {
		return this.distRest[timestep];
	}
	public double getTimeHundekopf(int timestep) {
		return this.timeHundekopf[timestep];
	}
	public double getTimeGemarkung(int timestep) {
		return this.timeGemarkung[timestep];
	}
	public double getTimeRest(int timestep) {
		return this.timeRest[timestep];
	}
	public int getTripsHundekopf(int timestep) {
		return this.tripsHundekopf[timestep];
	}
	public int getTripsGemarkung(int timestep) {
		return this.tripsGemarkung[timestep];
	}
	public int getTripsRest(int timestep) {
		return this.tripsRest[timestep];
	}

}
