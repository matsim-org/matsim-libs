/* *********************************************************************** *
 * project: org.matsim.*
 * PTTravelTimeKTI.java
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

package playground.christoph.evacuation.trafficmonitoring;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.util.PersonalizableTravelTime;

import playground.balmermi.world.Layer;
import playground.meisterk.kti.router.KtiPtRoute;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;
import playground.meisterk.kti.router.SwissHaltestelle;

public class PTTravelTimeKTI implements PersonalizableTravelTime {

	private final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo;
	private final PlansCalcRouteConfigGroup configGroup;
	private final Map<Id, Double> agentSpeedMap;
	private final PersonalizableTravelTime ptTravelTime;

	private Double personSpeed;
	
	public PTTravelTimeKTI(PlansCalcRouteKtiInfo plansCalcRouteKtiInfo, 
			PlansCalcRouteConfigGroup configGroup, Map<Id, Double> agentSpeedMap,
			PersonalizableTravelTime ptTravelTime) {
		this.plansCalcRouteKtiInfo = plansCalcRouteKtiInfo;
		this.configGroup = configGroup;
		this.agentSpeedMap = agentSpeedMap;
		this.ptTravelTime = ptTravelTime;
	}
	
	@Override
	public double getLinkTravelTime(Link link, double time) {
		if (this.personSpeed != null) {
			return link.getLength() / this.personSpeed;			
		} else return ptTravelTime.getLinkTravelTime(link, time);
	}

	@Override
	public void setPerson(Person person) {
		this.personSpeed = agentSpeedMap.get(person.getId());
		
		/*
		 * If no speed value for the person is set, the agent performs
		 * the PT trip before the evacuation starts. Therefore use
		 * the simple PT travel time calculator.
		 */
		if (this.personSpeed == null) {
			this.ptTravelTime.setPerson(person);			
		}
	}

	public void setPersonSpeed(Id personId, double speed) {
		this.agentSpeedMap.put(personId, speed);
	}
	
	/**
	 * Based on PlansCalcRouteKti (see playground meisterk)
	 * Make use of the Swiss National transport model to find more or less realistic travel times.
	 *
	 * The travel time of a leg with mode 'pt' (meaning pseudo/public transport)
	 * is estimated by a municipality-to-municipality travel time matrix, plus walk-speed access to and egress from the next pt stop.
	 *
	 * @param fromAct
	 * @param toAct
	 * @param depTime
	 * @return travel time
	 */
	public double calcSwissPtTravelTime(final Activity fromAct, final Activity toAct, final double depTime) {

		double travelTime = 0.0;

		SwissHaltestelle fromStop = this.plansCalcRouteKtiInfo.getHaltestellen().getClosestLocation(fromAct.getCoord());
		SwissHaltestelle toStop = this.plansCalcRouteKtiInfo.getHaltestellen().getClosestLocation(toAct.getCoord());

		Layer municipalities = this.plansCalcRouteKtiInfo.getLocalWorld().getLayer("municipality");
		final List<? extends BasicLocation> froms = municipalities.getNearestLocations(fromStop.getCoord());
		final List<? extends BasicLocation> tos = municipalities.getNearestLocations(toStop.getCoord());
		BasicLocation fromMunicipality = froms.get(0);
		BasicLocation toMunicipality = tos.get(0);

		KtiPtRoute newRoute = new KtiPtRoute(fromAct.getLinkId(), toAct.getLinkId(), plansCalcRouteKtiInfo, fromStop, fromMunicipality, toMunicipality, toStop);

		final double timeInVehicle = newRoute.getInVehicleTime();

		final double walkAccessEgressDistance = newRoute.calcAccessEgressDistance(fromAct, toAct);
		final double walkAccessEgressTime = walkAccessEgressDistance / configGroup.getWalkSpeed();

		newRoute.setDistance((walkAccessEgressDistance + newRoute.calcInVehicleDistance()));

		travelTime = walkAccessEgressTime + timeInVehicle;

		return travelTime;
	}

}