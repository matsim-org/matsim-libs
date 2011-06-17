/* *********************************************************************** *
 * project: org.matsim.*
 * RouteTravelTimeMeasure.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.yu.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;

import playground.yu.utils.container.CollectionMath;

/**
 * measures travel time of {@code NetworkRoute}, and number of agents using
 * respective {@code Route}, outputs average travel time of each
 * {@code NetworkRoute}
 *
 * @author yu
 *
 */
public class RouteTravelTimeMeasure implements AgentDepartureEventHandler,
		AgentArrivalEventHandler, LinkEnterEventHandler {

	private Map<String/* route "ID" */, List<Double>/* travelTimes */> routeTimes = new HashMap<String, List<Double>>();
	private Map<Id/* agent Id */, StringBuffer/* route ID in future */> agentRouteIdCreator/* temporary */= new HashMap<Id, StringBuffer>();
	private Map<Id/* agent Id */, Double/* departure time */> agentDepartTimes/* temporary */= new HashMap<Id, Double>();

	@Override
	public void reset(int iteration) {
		routeTimes.clear();
		agentDepartTimes.clear();
		agentRouteIdCreator.clear();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id agentId = event.getPersonId();
		Id linkId = event.getLinkId();
		StringBuffer strBuf = agentRouteIdCreator.get(agentId);
		if (strBuf == null) {
			throw new RuntimeException("Agent\t" + agentId
					+ "\tentering Link\t" + linkId + "\twithout departure??");
		}
		strBuf.append('-');
		strBuf.append(linkId);
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		// creates route Id
		Id agentId = event.getPersonId();
		Id linkId = event.getLinkId();
		StringBuffer strBuf = agentRouteIdCreator.remove(agentId);
		if (strBuf == null) {
			throw new RuntimeException(
					"Without departure and entering Links records of Agent\t"
							+ agentId + "\tjust arriving in Link\t" + linkId
							+ "??");
		}
		String routeId = strBuf.toString();

		// calculates travel time
		Double departTime = agentDepartTimes.remove(agentId);
		if (departTime == null) {
			throw new RuntimeException("Without departure record of Agent\t"
					+ agentId + "\tjust arriving in Link\t" + linkId + "??");
		}
		double travelTime = event.getTime() - departTime;

		// saves travel time
		List<Double> travelTimes = routeTimes.get(routeId);
		if (travelTimes == null) {
			travelTimes = new Vector<Double>();
			routeTimes.put(routeId, travelTimes);
		}
		travelTimes.add(travelTime);
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		Id agentId = event.getPersonId();
		if (agentRouteIdCreator.containsKey(agentId)) {
			throw new RuntimeException("The LAST record of agent\t" + agentId
					+ "\tin agentRouteIdCreator should have been deleted!");
		}
		if (agentDepartTimes.containsKey(agentId)) {
			throw new RuntimeException("The LAST record of agent\t" + agentId
					+ "\tin agentDepartTimes should have been deleted!");
		}

		agentRouteIdCreator.put(agentId, new StringBuffer(event.getLinkId()
				.toString()));

		agentDepartTimes.put(agentId, event.getTime());
	}

	public Map<String/* route Id */, Double/* avg travel ime */> getAvgTravelTimes() {
		Map<String, Double> avgTravTimes = new HashMap<String, Double>();
		for (String routeId : routeTimes.keySet()) {
			List<Double> routeTravTimes = routeTimes.get(routeId);
			double avgTravTime = CollectionMath.getAvg(routeTravTimes);
			avgTravTimes.put(routeId, avgTravTime);
		}
		return avgTravTimes;
	}

	public Map<String/* routeId */, Integer/* nb. of agents taking one route */> getNbTakingRoute() {
		Map<String, Integer> routeUserNbs = new HashMap<String, Integer>();
		for (String routeId : routeTimes.keySet()) {
			routeUserNbs.put(routeId, routeTimes.get(routeId).size());
		}
		return routeUserNbs;
	}
}
