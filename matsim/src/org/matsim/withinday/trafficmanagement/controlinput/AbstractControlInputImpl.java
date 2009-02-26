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
import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.mobsim.queuesim.SimulationTimer;
import org.matsim.utils.misc.Time;
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

	protected CarRoute mainRoute;

	protected CarRoute alternativeRoute;

	protected Map<String, Integer> numberOfAgents;

	protected Map<Id, Double> ttFreeSpeeds;

	protected String firstLinkOnMainRoute;

	protected String firstLinkOnAlternativeRoute;

	protected String lastLinkOnMainRoute;

	protected String lastLinkOnAlternativeRoute;

	protected Map<String, Double> enterEvents1;

	protected Map<String, Double> enterEvents2;

	protected double lastTimeMainRoute;

	protected double lastTimeAlternativeRoute;

	protected double timeDifference;

	protected Link altRouteNaturalBottleNeck;

	protected Link mainRouteNaturalBottleNeck;

	protected double ttFreeSpeedAltRoute;

	protected double ttFreeSpeedMainRoute;

	protected Map<Double, Double> ttMeasuredMainRoute = new HashMap<Double, Double>();

	protected Map<Double, Double> ttMeasuredAlternativeRoute = new HashMap<Double, Double>();

	private final ControlInputWriter writer;

	protected Map<String, Double> intraFlows = new HashMap<String, Double>();

	protected Map<String, List<Double>> enterLinkEventTimes = new HashMap<String, List<Double>>();

	protected List<Link> inLinksMainRoute = new ArrayList<Link>();

	protected List<Link> outLinksMainRoute = new ArrayList<Link>();


	protected List<Link> inLinksAlternativeRoute = new ArrayList<Link>();

	protected List<Link> outLinksAlternativeRoute = new ArrayList<Link>();

	// For in/outlinks disturbance check:
	protected Map<String, Double> extraFlowsMainRoute = new HashMap<String, Double>();

	protected Map<String, Double> extraFlowsAlternativeRoute = new HashMap<String, Double>();



	public AbstractControlInputImpl() {
		this.numberOfAgents = new HashMap<String, Integer>();
		this.enterEvents1 = new HashMap<String, Double>();
		this.enterEvents2 = new HashMap<String, Double>();
		this.ttFreeSpeeds = new HashMap<Id, Double>();

		this.lastTimeMainRoute = 0.0;
		this.lastTimeAlternativeRoute = 0.0;
		this.timeDifference = 0.0;
		this.ttFreeSpeedAltRoute = 0.0;
		this.ttFreeSpeedMainRoute = 0.0;

		this.writer = new ControlInputWriter();

	}

	public abstract double getPredictedNashTime(CarRoute route);

	public double getNashTime() {
		try {
			this.writer.writeTravelTimesMainRoute(SimulationTimer.getTime(),
					this.lastTimeMainRoute, this.getPredictedNashTime(this.mainRoute));
			this.writer.writeTravelTimesAlternativeRoute(SimulationTimer.getTime(),
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

	public CarRoute getMainRoute() {
		return this.mainRoute;
	}

	public CarRoute getAlternativeRoute() {
		return this.alternativeRoute;
	}

	public void setAlternativeRoute(final CarRoute route) {
		this.alternativeRoute = route;
	}

	public void setMainRoute(final CarRoute route) {
		this.mainRoute = route;
	}

	public int getNumberOfVehiclesOnRoute(final CarRoute route) {
		int ret = 0;
		for (Link link : route.getLinks()) {
			ret += this.numberOfAgents.get(link.getId().toString());
		}
		return ret;
	}

	/**
	 * @see org.matsim.withinday.trafficmanagement.ControlInput#init()
	 */

	public void init() {
		this.writer.open();

		List<Link> routeLinks = this.getAlternativeRoute().getLinks();
		this.firstLinkOnAlternativeRoute = routeLinks.get(0).getId().toString();
		this.lastLinkOnAlternativeRoute = routeLinks.get(routeLinks.size() - 1).getId()
				.toString();
		for (Link l : routeLinks) {
			if (!this.numberOfAgents.containsKey(l.getId().toString())) {
				this.numberOfAgents.put(l.getId().toString(), Integer.valueOf(0));
			}
			double tt = l.getLength() / l.getFreespeed(Time.UNDEFINED_TIME);
			this.ttFreeSpeeds.put(l.getId(), tt);
			this.ttFreeSpeedAltRoute += tt;
		}
		this.lastTimeAlternativeRoute = this.ttFreeSpeedAltRoute;

		// find the natural bottleneck on the alternative route
		List<Link> altRouteLinks = this.getAlternativeRoute().getLinks();
		this.altRouteNaturalBottleNeck = altRouteLinks.get(0);
		for (int i = 1; i < altRouteLinks.size(); i++) {
			if (altRouteLinks.get(i).getCapacity(Time.UNDEFINED_TIME) <= this.altRouteNaturalBottleNeck
					.getCapacity(Time.UNDEFINED_TIME))
				this.altRouteNaturalBottleNeck = altRouteLinks.get(i);
		}

		routeLinks = this.getMainRoute().getLinks();
		this.firstLinkOnMainRoute = routeLinks.get(0).getId().toString();
		this.lastLinkOnMainRoute = routeLinks.get(routeLinks.size() - 1).getId()
				.toString();
		double tt;
		for (Link l : routeLinks) {
			if (!this.numberOfAgents.containsKey(l.getId().toString())) {
				this.numberOfAgents.put(l.getId().toString(), Integer.valueOf(0));
			}
			tt = l.getLength() / l.getFreespeed(Time.UNDEFINED_TIME);
			this.ttFreeSpeeds.put(l.getId(), tt);
			this.ttFreeSpeedMainRoute += tt;
		}
		this.lastTimeMainRoute = this.ttFreeSpeedMainRoute;

		// find the natural bottleneck on the main route
		List<Link> mainRouteLinks = this.getMainRoute().getLinks();
		this.mainRouteNaturalBottleNeck = mainRouteLinks.get(0);
		for (int i = 1; i < mainRouteLinks.size(); i++) {
			if (mainRouteLinks.get(i).getCapacity(Time.UNDEFINED_TIME) < this.mainRouteNaturalBottleNeck
					.getCapacity(Time.UNDEFINED_TIME))
				this.mainRouteNaturalBottleNeck = mainRouteLinks.get(i);
		}
	}

	// memorize linkEnterEvents on the first links of the two alternative routes:
	public void handleEvent(final LinkEnterEvent event) {
		// count the agents on the route links

		if (event.linkId.equals(this.firstLinkOnMainRoute)) {
			this.enterEvents1.put(event.agentId, event.time);
		}
		else if (event.linkId.equals(this.firstLinkOnAlternativeRoute)) {
			this.enterEvents2.put(event.agentId, event.time);
		}

		if (this.numberOfAgents.containsKey(event.linkId)) {
			int number = this.numberOfAgents.get(event.linkId);
			number++;
			this.numberOfAgents.put(event.linkId, Integer.valueOf(number));
		}

	}

	public void handleEvent(final LinkLeaveEvent event) {
		// decrease current #agents
		if (this.numberOfAgents.containsKey(event.linkId)) {
			int number = this.numberOfAgents.get(event.linkId);
			number--;
			this.numberOfAgents.put(event.linkId, Integer.valueOf(number));
		}

		// if someone leaves one of the last links of the two alternative routes,
		// then
		// - check if that vehicle entered at the beginning
		// - if so, then derive latest measured nashtime input from that
		boolean timeDifferenceHasChanged = false;
		if (event.linkId.equals(this.lastLinkOnMainRoute)) {
			Double t1 = this.enterEvents1.remove(event.agentId);
			if (t1 != null) {
				double deltaT = event.time - t1;
				this.ttMeasuredMainRoute.put(event.time, deltaT);
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
		else if (event.linkId.equals(this.lastLinkOnAlternativeRoute)) {
			Double t1 = this.enterEvents2.remove(event.agentId);
			if (t1 != null) {
				double deltaT = event.time - t1;
				this.ttMeasuredAlternativeRoute.put(event.time, deltaT);
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
		if (this.numberOfAgents.containsKey(event.linkId)) {
			int number = this.numberOfAgents.get(event.linkId);
			number++;
			this.numberOfAgents.put(event.linkId, Integer.valueOf(number));
		}
	}

	public void handleEvent(final AgentArrivalEvent event) {
		// decrease number of agents on the route links
		if (this.numberOfAgents.containsKey(event.linkId)) {
			int number = this.numberOfAgents.get(event.linkId);
			number--;
			this.numberOfAgents.put(event.linkId, Integer.valueOf(number));
		}
	}

	public double getFreeSpeedRouteTravelTime(final CarRoute route) {
		if (route == this.mainRoute)
			return this.ttFreeSpeedMainRoute;
		else if (route == this.alternativeRoute)
			return this.ttFreeSpeedAltRoute;
		else
			throw new IllegalArgumentException("This route object does not exist!");
	}

	public double getMeasuredRouteTravelTime(final CarRoute route) {
		if (route == this.mainRoute)
			return this.lastTimeMainRoute;
		else if (route == this.alternativeRoute)
			return this.lastTimeAlternativeRoute;
		else
			throw new IllegalArgumentException("This route object does not exist!");
	}

	public Link getNaturalBottleNeck(final CarRoute r) {
		Link naturalBottleNeck;
		if (r == this.mainRoute) {
			naturalBottleNeck = this.mainRouteNaturalBottleNeck;
		}
		else {
			naturalBottleNeck = this.altRouteNaturalBottleNeck;
		}
		return naturalBottleNeck;
	}

	protected void updateFlow(final int flowResolution, final LinkLeaveEvent event) {

		LinkedList<Double> list = (LinkedList<Double>) this.enterLinkEventTimes
				.get(event.linkId);
		if (list.size() == flowResolution) {
			list.removeFirst();
			list.addLast(event.time);
		}
		else if ((1 < list.size()) || (list.size() < flowResolution)) {
			list.add(event.time);
		}
		else if (list.size() == 0) {
			list.addLast(event.time - 1);
			list.addLast(event.time);
		}
		else {
			System.err
					.println("Error: number of enter event times stored exceeds numberofflowevents!");
		}

		// Flow = agents / seconds:
		double flow = (list.size() - 1) / (list.getLast() - list.getFirst());

		if (this.intraFlows.containsKey(event.linkId)) {
			this.intraFlows.put(event.linkId, flow);
		}
		if (this.inLinksMainRoute.contains(event.link)) {
			double inFlow = flow;
			this.extraFlowsMainRoute.put(event.linkId, inFlow);
		}
		if (this.outLinksMainRoute.contains(event.link)) {
			double outFlow = -flow;
			this.extraFlowsMainRoute.put(event.linkId, outFlow);
		}
		if (this.inLinksAlternativeRoute.contains(event.link)) {
			double inFlow = flow;
			this.extraFlowsAlternativeRoute.put(event.linkId, inFlow);
		}
		if (this.outLinksAlternativeRoute.contains(event.link)) {
			double outFlow = -flow;
			this.extraFlowsAlternativeRoute.put(event.linkId, outFlow);
		}
	}

	protected double sumUpTTFreeSpeed(final Node node, final CarRoute route) {

		double ttFS = 0;
		for (Link l : route.getLinks()) {
			ttFS += this.ttFreeSpeeds.get(l.getId());
			if (l.getToNode() == node) {
				break;
			}
		}
		return ttFS;
	}

	protected void updateFlow(final double flowUpdateTime, final LinkLeaveEvent event) {

		LinkedList<Double> list = (LinkedList<Double>) this.enterLinkEventTimes
				.get(event.linkId);
		// Remove times older than flowUpdateTime
			while (!list.isEmpty() && ((list.getFirst() + flowUpdateTime) < event.time)) {
				list.removeFirst();
			}
		// Add new values
		list.addLast(event.time);

		// Flow = agents / seconds:
		double flow = (list.size() - 1) / (list.getLast() - list.getFirst());

		if (this.intraFlows.containsKey(event.linkId)) {
			this.intraFlows.put(event.linkId, flow);
		}
		if (this.inLinksMainRoute.contains(event.link)) {
			double inFlow = flow;
			this.extraFlowsMainRoute.put(event.linkId, inFlow);
		}
		if (this.outLinksMainRoute.contains(event.link)) {
			double outFlow = -flow;
			this.extraFlowsMainRoute.put(event.linkId, outFlow);
		}
		if (this.inLinksAlternativeRoute.contains(event.link)) {
			double inFlow = flow;
			this.extraFlowsAlternativeRoute.put(event.linkId, inFlow);
		}
		if (this.outLinksAlternativeRoute.contains(event.link)) {
			double outFlow = -flow;
			this.extraFlowsAlternativeRoute.put(event.linkId, outFlow);
		}
	}

	public void reset(final int iteration) {}
	
	protected List<Link> getOutlinks(final CarRoute route) {
		if (route == this.mainRoute) {
			return this.outLinksMainRoute;
		}
		return this.outLinksAlternativeRoute;
	}

	protected List<Link> getInlinks(final CarRoute route) {
		if (route == this.mainRoute) {
			return this.inLinksMainRoute;
		}
		return this.inLinksAlternativeRoute;
	}

}
