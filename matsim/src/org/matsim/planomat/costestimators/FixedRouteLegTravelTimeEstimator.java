/* *********************************************************************** *
 * project: org.matsim.*
 * FixedRouteLegTravelTimeEstimator.java
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
import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.TravelTime;

/**
 * Abstract class for <code>LegTravelTimeEstimator</code>s
 * that estimate the travel time of a fixed route.
 *
 * @author meisterk
 *
 */
public class FixedRouteLegTravelTimeEstimator implements LegTravelTimeEstimator {

	protected final TravelTime linkTravelTimeEstimator;
	protected final DepartureDelayAverageCalculator tDepDelayCalc;
	private final PlansCalcRoute plansCalcRoute;
	private final PlanomatConfigGroup.SimLegInterpretation simLegInterpretation;

	private HashMap<RouteWRefs, List<LinkImpl>> linkRoutesCache = new HashMap<RouteWRefs, List<LinkImpl>>();
	private HashMap<LegImpl, HashMap<TransportMode, Double>> travelTimeCache = new HashMap<LegImpl, HashMap<TransportMode, Double>>();

	protected FixedRouteLegTravelTimeEstimator(
			TravelTime linkTravelTimeEstimator,
			DepartureDelayAverageCalculator depDelayCalc,
			PlansCalcRoute plansCalcRoute,
			PlanomatConfigGroup.SimLegInterpretation simLegInterpretation) {

		this.linkTravelTimeEstimator = linkTravelTimeEstimator;
		this.tDepDelayCalc = depDelayCalc;
		this.plansCalcRoute = plansCalcRoute;
		// TODO check
		this.simLegInterpretation = simLegInterpretation;

	}

	public double getLegTravelTimeEstimation(Id personId, double departureTime,
			ActivityImpl actOrigin, ActivityImpl actDestination,
			LegImpl legIntermediate, Boolean doModifyLeg) {

		double legTravelTimeEstimation = 0.0;

		if (legIntermediate.getMode().equals(TransportMode.car)) {

			double now = departureTime;
			now = this.processDeparture(actOrigin.getLink(), now);

			if (simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CetinCompatible)) {
				now = this.processRouteTravelTime((NetworkRoute) legIntermediate.getRoute(), now);
				now = this.processLink(actDestination.getLink(), now);
			} else if (simLegInterpretation.equals(PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible)) {
				now = this.processLink(actOrigin.getLink(), now);
				now = this.processRouteTravelTime((NetworkRoute) legIntermediate.getRoute(), now);
			}

			legTravelTimeEstimation = now - departureTime;
			
		} else {

			HashMap<TransportMode, Double> legInformation = null; 
			if (this.travelTimeCache.containsKey(legIntermediate)) {
				legInformation = this.travelTimeCache.get(legIntermediate);
			} else {
				legInformation = new HashMap<TransportMode, Double>();
				this.travelTimeCache.put(legIntermediate, legInformation);
			}
			if (legInformation.containsKey(legIntermediate.getMode())) {
				legTravelTimeEstimation = legInformation.get(legIntermediate.getMode()).doubleValue();
			} else {
				legTravelTimeEstimation = this.plansCalcRoute.handleLeg(legIntermediate, actOrigin, actDestination, departureTime);
				legInformation.put(legIntermediate.getMode(), legTravelTimeEstimation);
			}
			
		}

		return legTravelTimeEstimation;
		
	}


	
	protected double processDeparture(final LinkImpl link, final double start) {

		double departureDelayEnd = start + this.tDepDelayCalc.getLinkDepartureDelay(link, start);
		return departureDelayEnd;

	}

	protected double processRouteTravelTime(final NetworkRoute route, final double start) {

		double now = start;

		List<LinkImpl> links = null;
		if (this.linkRoutesCache.containsKey(route)) {
			links = this.linkRoutesCache.get(route);
		} else {
			links = route.getLinks();
			this.linkRoutesCache.put(route, links);
		}

		for (LinkImpl link : links) {
			now = this.processLink(link, now);
		}
		return now;

	}

	protected double processLink(final LinkImpl link, final double start) {

		double linkEnd = start + this.linkTravelTimeEstimator.getLinkTravelTime(link, start);
		return linkEnd;

	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	public void reset() {
		this.linkRoutesCache.clear();
		this.travelTimeCache.clear();
	}

}
