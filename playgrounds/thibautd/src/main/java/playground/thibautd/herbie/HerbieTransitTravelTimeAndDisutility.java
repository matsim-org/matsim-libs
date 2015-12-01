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

import herbie.running.config.HerbieConfigGroup;
import herbie.running.scoring.TravelScoringFunction;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.router.TransitTravelDisutility;
import org.matsim.vehicles.Vehicle;

import java.util.TreeSet;

/**
 * Travel time disutility taking into account travel distance
 * @author thibautd
 */
public class HerbieTransitTravelTimeAndDisutility implements TravelTime, TransitTravelDisutility {
	private final boolean CONSIDER_NEGATIVE_WALK_TIMES = true;
	private final boolean USE_CUSTOM_IV_COSTS = false;
	private final TransitRouterNetworkTravelTimeAndDisutility timeCost;
	private final TravelScoringFunction distanceScoring;
	private final HerbieConfigGroup herbieConfig;
	private final TransitRouterConfig config;

	@Override
	public double getTravelDisutility(
			final Person person,
			final Coord coord,
			final Coord toCoord) {
		return timeCost.getTravelDisutility(person, coord, toCoord);
	}

	@Override
	public double getTravelTime(
			final Person person,
			final Coord coord,
			final Coord toCoord) {
		return timeCost.getTravelTime(person, coord, toCoord);
	}

	private double distanceCost;

	public HerbieTransitTravelTimeAndDisutility(
			final HerbieConfigGroup herbieConfig,
			final TransitRouterConfig config,
			final TravelScoringFunction distanceScoring) {
		this.herbieConfig= herbieConfig;
		this.timeCost = new TransitRouterNetworkTravelTimeAndDisutility( config );
		this.distanceScoring = distanceScoring;
		this.config = config;
	}

	@Override
	public double getLinkTravelTime(
			final Link link,
			final double time, Person person, Vehicle vehicle) {
		return timeCost.getLinkTravelTime( link , time, person, vehicle );
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle, final CustomDataManager dataManager) {
		setPerson(person);
		if (((TransitRouterNetworkLink) link).getRoute() == null) {
			// it's a transfer link (walk)

			//			cost = -getLinkTravelTime(link, time) * this.config.getEffectiveMarginalUtilityOfTravelTimeWalk_utl_s() + this.config.getUtilityOfLineSwitch_utl();
			// (old specification)
			
			double transfertime = getLinkTravelTime(link, time, person, vehicle);
			double waittime = config.getAdditionalTransferTime();
			
			// say that the effective walk time is the transfer time minus some "buffer"
			double walktime = transfertime - waittime;
			
			// weigh the "buffer" not with the walk time disutility, but with the wait time disutility:
			// (note that this is the same "additional disutl of wait" as in the scoring function.  Its default is zero.
			// only if you are "including the opportunity cost of time into the router", then the disutility of waiting will
			// be the same as the marginal opprotunity cost of time).  kai, nov'11
			double cost = 
				HerbieRoutingWalkCostEstimator.getWalkCost(
						config,
						link.getLength(),
						CONSIDER_NEGATIVE_WALK_TIMES || (walktime > 0) ? walktime : 0 )
			    -waittime * config.getMarginalUtilityOfWaitingPt_utl_s()
			    - config.getUtilityOfLineSwitch_utl();

			if (!CONSIDER_NEGATIVE_WALK_TIMES && cost < 0) {
				throw new RuntimeException( "got a negative cost! "+cost );
			}

			return cost;
		}

		// this is fine as long as the scores are additives
		double cost =
			USE_CUSTOM_IV_COSTS ?
			-distanceScoring.getInVehiclePtScore(
				link.getLength(),
				getLinkTravelTime( link , time, person, vehicle ),
				distanceCost) :
			-getLinkTravelTime(link, time, person, vehicle) * this.config.getMarginalUtilityOfTravelTimePt_utl_s()
			- link.getLength() * this.config.getMarginalUtilityOfTravelDistancePt_utl_m();

		if (cost < 0) {
			throw new RuntimeException( "negative cost! "+cost );
		}

		return cost;
	}
	
	public void setPerson(final Person person) {
		TreeSet<String> travelCards = PersonUtils.getTravelcards(person);
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

