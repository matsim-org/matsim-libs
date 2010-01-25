/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractControlInputImpl.java
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

package org.matsim.withinday.trafficmanagement.controlinput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.misc.Time;
import org.matsim.ptproject.qsim.QSimTimer;
import org.matsim.withinday.trafficmanagement.ControlInput;

/**
 * @author dgrether
 *
 */
public abstract class AbstractControlInputImpl implements ControlInput,
		AgentDepartureEventHandler, AgentArrivalEventHandler,
		LinkEnterEventHandler, LinkLeaveEventHandler {

	private static final Logger log = Logger
			.getLogger(AbstractControlInputImpl.class);

	protected final Network network;
	
	protected NetworkRouteWRefs mainRoute;

	protected NetworkRouteWRefs alternativeRoute;

	protected Map<Id, Integer> numberOfAgents;

	protected Map<Id, Double> ttFreeSpeeds;

	private Id firstLinkOnMainRoute;

	private Id firstLinkOnAlternativeRoute;

	private Id lastLinkOnMainRoute;

	private Id lastLinkOnAlternativeRoute;

	private Map<Id, Double> enterEvents1;

	private Map<Id, Double> enterEvents2;

	protected double lastTimeMainRoute;

	protected double lastTimeAlternativeRoute;

	protected double timeDifference;

	protected Id altRouteNaturalBottleNeckId;

	protected Id mainRouteNaturalBottleNeckId;

	protected double ttFreeSpeedAltRoute;

	protected double ttFreeSpeedMainRoute;

	protected Map<Double, Double> ttMeasuredMainRoute = new HashMap<Double, Double>();

	protected Map<Double, Double> ttMeasuredAlternativeRoute = new HashMap<Double, Double>();

	private final ControlInputWriter writer;

	protected Map<Id, Double> intraFlows = new HashMap<Id, Double>();

	protected Map<Id, List<Double>> enterLinkEventTimes = new HashMap<Id, List<Double>>();

	protected List<Id> inLinksMainRoute = new ArrayList<Id>();

	protected List<Id> outLinksMainRoute = new ArrayList<Id>();


	protected List<Id> inLinksAlternativeRoute = new ArrayList<Id>();

	protected List<Id> outLinksAlternativeRoute = new ArrayList<Id>();

	// For in/outlinks disturbance check:
	protected Map<Id, Double> extraFlowsMainRoute = new HashMap<Id, Double>();

	protected Map<Id, Double> extraFlowsAlternativeRoute = new HashMap<Id, Double>();



	public AbstractControlInputImpl(final Network network) {
		this.network = network;
		this.numberOfAgents = new HashMap<Id, Integer>();
		this.enterEvents1 = new HashMap<Id, Double>();
		this.enterEvents2 = new HashMap<Id, Double>();
		this.ttFreeSpeeds = new HashMap<Id, Double>();

		this.lastTimeMainRoute = 0.0;
		this.lastTimeAlternativeRoute = 0.0;
		this.timeDifference = 0.0;
		this.ttFreeSpeedAltRoute = 0.0;
		this.ttFreeSpeedMainRoute = 0.0;

		this.writer = new ControlInputWriter();

	}

	public abstract double getPredictedNashTime(NetworkRouteWRefs route);

	public double getNashTime() {
		try {
			this.writer.writeTravelTimesMainRoute(QSimTimer.getTime(),
					this.lastTimeMainRoute, this.getPredictedNashTime(this.mainRoute));
			this.writer.writeTravelTimesAlternativeRoute(QSimTimer.getTime(),
					this.lastTimeAlternativeRoute, this
							.getPredictedNashTime(this.alternativeRoute));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this.timeDifference;
	}

	public void finishIteration() {
		try {
			this.writer.writeTravelTimesPerAgent(this.ttMeasuredMainRoute,
					this.ttMeasuredAlternativeRoute);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.writer.close();
	}

	public NetworkRouteWRefs getMainRoute() {
		return this.mainRoute;
	}

	public NetworkRouteWRefs getAlternativeRoute() {
		return this.alternativeRoute;
	}

	public void setAlternativeRoute(final NetworkRouteWRefs route) {
		this.alternativeRoute = route;
	}

	public void setMainRoute(final NetworkRouteWRefs route) {
		this.mainRoute = route;
	}

	public int getNumberOfVehiclesOnRoute(final NetworkRouteWRefs route) {
		int ret = 0;
		for (Id linkId : route.getLinkIds()) {
			ret += this.numberOfAgents.get(linkId);
		}
		return ret;
	}

	/**
	 * @see org.matsim.withinday.trafficmanagement.ControlInput#init()
	 */

	public void init() {
		this.writer.open();

		List<Id> routeLinkIds = this.getAlternativeRoute().getLinkIds();
		this.firstLinkOnAlternativeRoute = routeLinkIds.get(0);
		this.lastLinkOnAlternativeRoute = routeLinkIds.get(routeLinkIds.size() - 1);
		for (Id linkId : routeLinkIds) {
			if (!this.numberOfAgents.containsKey(linkId)) {
				this.numberOfAgents.put(linkId, Integer.valueOf(0));
			}
			Link link = this.network.getLinks().get(linkId);
			double tt = link.getLength() / link.getFreespeed(Time.UNDEFINED_TIME);
			this.ttFreeSpeeds.put(linkId, tt);
			this.ttFreeSpeedAltRoute += tt;
		}
		this.lastTimeAlternativeRoute = this.ttFreeSpeedAltRoute;

		// find the natural bottleneck on the alternative route
		List<Id> altRouteLinkIds = this.getAlternativeRoute().getLinkIds();
		this.altRouteNaturalBottleNeckId = altRouteLinkIds.get(0);
		Link altRouteNaturalBottleNeck = this.network.getLinks().get(this.altRouteNaturalBottleNeckId);
		for (int i = 1; i < altRouteLinkIds.size(); i++) {
			Link link = this.network.getLinks().get(altRouteLinkIds.get(i));
			if (link.getCapacity(Time.UNDEFINED_TIME) <= altRouteNaturalBottleNeck.getCapacity(Time.UNDEFINED_TIME)) {
				altRouteNaturalBottleNeck = link;
				this.altRouteNaturalBottleNeckId = link.getId();
			}
		}

		routeLinkIds = this.getMainRoute().getLinkIds();
		this.firstLinkOnMainRoute = routeLinkIds.get(0);
		this.lastLinkOnMainRoute = routeLinkIds.get(routeLinkIds.size() - 1);
		double tt;
		for (Id linkId : routeLinkIds) {
			if (!this.numberOfAgents.containsKey(linkId)) {
				this.numberOfAgents.put(linkId, Integer.valueOf(0));
			}
			Link link = this.network.getLinks().get(linkId);
			tt = link.getLength() / link.getFreespeed(Time.UNDEFINED_TIME);
			this.ttFreeSpeeds.put(linkId, tt);
			this.ttFreeSpeedMainRoute += tt;
		}
		this.lastTimeMainRoute = this.ttFreeSpeedMainRoute;

		// find the natural bottleneck on the main route
		List<Id> mainRouteLinkIds = this.getMainRoute().getLinkIds();
		this.mainRouteNaturalBottleNeckId = mainRouteLinkIds.get(0);
		Link mainRouteNaturalBottleNeck = this.network.getLinks().get(this.mainRouteNaturalBottleNeckId);
		for (int i = 1; i < mainRouteLinkIds.size(); i++) {
			Link link = this.network.getLinks().get(mainRouteLinkIds.get(i));
			if (link.getCapacity(Time.UNDEFINED_TIME) < mainRouteNaturalBottleNeck.getCapacity(Time.UNDEFINED_TIME)) {
				mainRouteNaturalBottleNeck = link;
				this.mainRouteNaturalBottleNeckId = link.getId();
			}
		}
	}

	// memorize linkEnterEvents on the first links of the two alternative routes:
	public void handleEvent(final LinkEnterEvent event) {
		// count the agents on the route links

		if (event.getLinkId().equals(this.firstLinkOnMainRoute)) {
			this.enterEvents1.put(event.getPersonId(), event.getTime());
		}
		else if (event.getLinkId().equals(this.firstLinkOnAlternativeRoute)) {
			this.enterEvents2.put(event.getPersonId(), event.getTime());
		}

		if (this.numberOfAgents.containsKey(event.getLinkId())) {
			int number = this.numberOfAgents.get(event.getLinkId());
			number++;
			this.numberOfAgents.put(event.getLinkId(), Integer.valueOf(number));
		}

	}

	public void handleEvent(final LinkLeaveEvent event) {
		// decrease current #agents
		if (this.numberOfAgents.containsKey(event.getLinkId())) {
			int number = this.numberOfAgents.get(event.getLinkId());
			number--;
			this.numberOfAgents.put(event.getLinkId(), Integer.valueOf(number));
		}

		// if someone leaves one of the last links of the two alternative routes,
		// then
		// - check if that vehicle entered at the beginning
		// - if so, then derive latest measured nashtime input from that
		boolean timeDifferenceHasChanged = false;
		if (event.getLinkId().equals(this.lastLinkOnMainRoute)) {
			Double t1 = this.enterEvents1.remove(event.getPersonId());
			if (t1 != null) {
				double deltaT = event.getTime() - t1;
				this.ttMeasuredMainRoute.put(event.getTime(), deltaT);
				if (deltaT >= 0) {
					this.lastTimeMainRoute = deltaT;
					timeDifferenceHasChanged = true;
				}
//				else {
					// System.err.println("not sure why this could ever happen
					// 2vnowskljdf");
//				}
			}
		}
		else if (event.getLinkId().equals(this.lastLinkOnAlternativeRoute)) {
			Double t1 = this.enterEvents2.remove(event.getPersonId());
			if (t1 != null) {
				double deltaT = event.getTime() - t1;
				this.ttMeasuredAlternativeRoute.put(event.getTime(), deltaT);
				if (deltaT >= 0) {
					this.lastTimeAlternativeRoute = deltaT;
					timeDifferenceHasChanged = true;
				}
				else {
					// System.err.println("not sure why this could ever happen
					// 2vnowfskljdf");
				}
			}
		}

		if ((this.lastTimeMainRoute >= 0.) && (this.lastTimeAlternativeRoute >= 0.)
				&& timeDifferenceHasChanged) {
			this.timeDifference = this.lastTimeMainRoute
					- this.lastTimeAlternativeRoute;
			if (log.isTraceEnabled()) {
				log.trace("time at main route: " + this.lastTimeMainRoute);
				log.trace("time at alternative route 2: "
						+ this.lastTimeAlternativeRoute);
				log.trace("timeDifference changed: " + this.timeDifference);
			}
		}
	}

	public void handleEvent(final AgentDepartureEvent event) {
		// increase number of agents on the route links
		if (this.numberOfAgents.containsKey(event.getLinkId())) {
			int number = this.numberOfAgents.get(event.getLinkId());
			number++;
			this.numberOfAgents.put(event.getLinkId(), Integer.valueOf(number));
		}
	}

	public void handleEvent(final AgentArrivalEvent event) {
		// decrease number of agents on the route links
		if (this.numberOfAgents.containsKey(event.getLinkId())) {
			int number = this.numberOfAgents.get(event.getLinkId());
			number--;
			this.numberOfAgents.put(event.getLinkId(), Integer.valueOf(number));
		}
	}

	public double getFreeSpeedRouteTravelTime(final NetworkRouteWRefs route) {
		if (route == this.mainRoute)
			return this.ttFreeSpeedMainRoute;
		else if (route == this.alternativeRoute)
			return this.ttFreeSpeedAltRoute;
		else
			throw new IllegalArgumentException("This route object does not exist!");
	}

	public double getMeasuredRouteTravelTime(final NetworkRouteWRefs route) {
		if (route == this.mainRoute)
			return this.lastTimeMainRoute;
		else if (route == this.alternativeRoute)
			return this.lastTimeAlternativeRoute;
		else
			throw new IllegalArgumentException("This route object does not exist!");
	}

	public Id getNaturalBottleNeckId(final NetworkRouteWRefs r) {
		if (r == this.mainRoute) {
			return this.mainRouteNaturalBottleNeckId;
		}
		return this.altRouteNaturalBottleNeckId;
	}

	protected void updateFlow(final int flowResolution, final LinkLeaveEvent event) {

		LinkedList<Double> list = (LinkedList<Double>) this.enterLinkEventTimes
				.get(event.getLinkId());
		if (list.size() == flowResolution) {
			list.removeFirst();
			list.addLast(event.getTime());
		}
		else if ((1 < list.size()) || (list.size() < flowResolution)) {
			list.add(event.getTime());
		}
		else if (list.size() == 0) {
			list.addLast(event.getTime() - 1);
			list.addLast(event.getTime());
		}
		else {
			System.err
					.println("Error: number of enter event times stored exceeds numberofflowevents!");
		}

		// Flow = agents / seconds:
		double flow = (list.size() - 1) / (list.getLast() - list.getFirst());

		if (this.intraFlows.containsKey(event.getLinkId())) {
			this.intraFlows.put(event.getLinkId(), flow);
		}
		if (this.inLinksMainRoute.contains(event.getLinkId())) {
			double inFlow = flow;
			this.extraFlowsMainRoute.put(event.getLinkId(), inFlow);
		}
		if (this.outLinksMainRoute.contains(event.getLinkId())) {
			double outFlow = -flow;
			this.extraFlowsMainRoute.put(event.getLinkId(), outFlow);
		}
		if (this.inLinksAlternativeRoute.contains(event.getLinkId())) {
			double inFlow = flow;
			this.extraFlowsAlternativeRoute.put(event.getLinkId(), inFlow);
		}
		if (this.outLinksAlternativeRoute.contains(event.getLinkId())) {
			double outFlow = -flow;
			this.extraFlowsAlternativeRoute.put(event.getLinkId(), outFlow);
		}
	}

	protected double sumUpTTFreeSpeed(final Node node, final NetworkRouteWRefs route) {

		double ttFS = 0;
		for (Id linkId : route.getLinkIds()) {
			ttFS += this.ttFreeSpeeds.get(linkId);
			Link l = this.network.getLinks().get(linkId);
			if (l.getToNode() == node) {
				break;
			}
		}
		return ttFS;
	}

	protected void updateFlow(final double flowUpdateTime, final LinkLeaveEvent event) {

		LinkedList<Double> list = (LinkedList<Double>) this.enterLinkEventTimes
				.get(event.getLinkId());
		// Remove times older than flowUpdateTime
			while (!list.isEmpty() && ((list.getFirst() + flowUpdateTime) < event.getTime())) {
				list.removeFirst();
			}
		// Add new values
		list.addLast(event.getTime());

		// Flow = agents / seconds:
		double flow = (list.size() - 1) / (list.getLast() - list.getFirst());

		if (this.intraFlows.containsKey(event.getLinkId())) {
			this.intraFlows.put(event.getLinkId(), flow);
		}
		if (this.inLinksMainRoute.contains(event.getLinkId())) {
			double inFlow = flow;
			this.extraFlowsMainRoute.put(event.getLinkId(), inFlow);
		}
		if (this.outLinksMainRoute.contains(event.getLinkId())) {
			double outFlow = -flow;
			this.extraFlowsMainRoute.put(event.getLinkId(), outFlow);
		}
		if (this.inLinksAlternativeRoute.contains(event.getLinkId())) {
			double inFlow = flow;
			this.extraFlowsAlternativeRoute.put(event.getLinkId(), inFlow);
		}
		if (this.outLinksAlternativeRoute.contains(event.getLinkId())) {
			double outFlow = -flow;
			this.extraFlowsAlternativeRoute.put(event.getLinkId(), outFlow);
		}
	}

	public void reset(final int iteration) {}
	
	protected List<Id> getOutlinks(final NetworkRouteWRefs route) {
		if (route == this.mainRoute) {
			return this.outLinksMainRoute;
		}
		return this.outLinksAlternativeRoute;
	}

	protected List<Id> getInlinks(final NetworkRouteWRefs route) {
		if (route == this.mainRoute) {
			return this.inLinksMainRoute;
		}
		return this.inLinksAlternativeRoute;
	}

}
