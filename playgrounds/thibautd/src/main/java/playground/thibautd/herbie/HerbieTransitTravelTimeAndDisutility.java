/* *********************************************************************** *
 * project: org.matsim.*
 * HerbieTravelTimeAndDisutility.java
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
package playground.thibautd.herbie;

import java.util.TreeSet;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.util.PersonalizableTravelDisutility;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;

import herbie.running.config.HerbieConfigGroup;
import herbie.running.scoring.TravelScoringFunction;

/**
 * Travel time disutility taking into account travel distance
 * @author thibautd
 */
public class HerbieTransitTravelTimeAndDisutility implements PersonalizableTravelTime, PersonalizableTravelDisutility {
	private final TransitRouterNetworkTravelTimeAndDisutility timeCost;
	private final TravelScoringFunction distanceScoring;
	private final HerbieConfigGroup herbieConfig;

	private double distanceCost;

	public HerbieTransitTravelTimeAndDisutility(
			final HerbieConfigGroup herbieConfig,
			final TransitRouterConfig config,
			final TravelScoringFunction distanceScoring) {
		this.herbieConfig= herbieConfig;
		this.timeCost = new TransitRouterNetworkTravelTimeAndDisutility( config );
		this.distanceScoring = distanceScoring;
	}

	@Override
	public double getLinkTravelTime(
			final Link link,
			final double time) {
		return timeCost.getLinkTravelTime( link , time );
	}

	@Override
	public double getLinkTravelDisutility(
			final Link link,
			final double time) {
		// this is fine as long as the scores are additives are additive
		return -distanceScoring.getInVehiclePtScore(
				link.getLength(),
				getLinkTravelTime( link , time ),
				distanceCost);
	}

	@Override
	public void setPerson(
			final Person person) {
		TreeSet<String> travelCards = ((PersonImpl) person).getTravelcards();
		if (travelCards == null) {
			distanceCost = herbieConfig.getDistanceCostPtNoTravelCard();
		}
		else if (travelCards.contains("unknown")) {
			distanceCost = herbieConfig.getDistanceCostPtUnknownTravelCard();
		}
		else {
			throw new RuntimeException("Person " + person.getId() + 
					" has an invalid travelcard set: "+travelCards+". This should never happen.");
		}
	}
}

