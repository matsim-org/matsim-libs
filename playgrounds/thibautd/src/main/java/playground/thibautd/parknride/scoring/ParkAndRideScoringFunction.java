/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideScoringFunction.java
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
package playground.thibautd.parknride.scoring;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.Facility;

import playground.thibautd.parknride.ParkAndRideConstants;

/**
 * Defines penalties for parking for park and ride simulation. The rest of
 * the scoring is handled by a delegate.
 *
 * @author thibautd
 */
public class ParkAndRideScoringFunction implements ScoringFunction {
	private final ScoringFunction delegate;
	private final ParkingPenalty penalty;
	private final ActivityFacilities facilities;
	private final Network network;
	// for informative log msgs only.
	private final Plan plan;

	private double lastArrivalTime = Time.UNDEFINED_TIME;
	private boolean isParked = false;
	private boolean lastLegWasCar = false;

	public ParkAndRideScoringFunction(
			final ScoringFunction scoringFunction,
			final ParkingPenalty penalty,
			final ActivityFacilities facilities,
			final Network network,
			final Plan plan) {
		this.delegate = scoringFunction;
		this.penalty = penalty;
		this.facilities = facilities;
		this.network = network;
		this.plan = plan;
	}

	@Override
	public void handleActivity(final Activity activity) {
		try {
			if (!ParkAndRideConstants.PARKING_ACT.equals( activity.getType() )) {
					delegate.handleActivity(activity);
			}

			if (lastLegWasCar && !activity.getType().equals( ParkAndRideConstants.PARKING_ACT )) {
				penalty.park( lastArrivalTime , extractCoord( activity ) );
				isParked = true;
			}
		}
		catch (Exception e) {
			throw new RuntimeException( "problem occured while scoring activity "+activity+" in plan "+plan.getPlanElements()+" for person "+plan.getPerson().getId() , e );
		}
	}

	@Override
	public void handleLeg(final Leg leg) {
		try {
			delegate.handleLeg(leg);

			if (leg.getMode().equals( TransportMode.car )) {
				lastArrivalTime = leg.getDepartureTime() + leg.getTravelTime();

				if (isParked) {
					penalty.unPark( leg.getDepartureTime() );
					isParked = false;
				}

				lastLegWasCar = true;
			}
			else {
				lastLegWasCar = false;
			}
		}
		catch(Exception e) {
			throw new RuntimeException( "a problem occured while scoring "+leg+" in plan "+plan.getPlanElements()+" for person "+plan.getPerson().getId() , e );
		}
	}

	@Override
	public void agentStuck(final double time) {
		delegate.agentStuck(time);
	}

	@Override
	public void addMoney(final double amount) {
		delegate.addMoney(amount);
	}

	@Override
	public void finish() {
		delegate.finish();
		penalty.finish();
	}

	@Override
	public double getScore() {
		return delegate.getScore() + penalty.getPenalty();
	}

	private Coord extractCoord(final Activity activity) {
		// use in priority the coord from act
		Coord coord = activity.getCoord();

		// if no coord, search for a facility, if possible
		if (coord == null && facilities != null) {
			Facility facility = facilities.getFacilities().get( activity.getFacilityId() );
			coord = facility == null ? null : facility.getCoord();
		}

		// if still nothing, use link coord
		if (coord == null && network != null) {
			Id linkId = activity.getLinkId();
			Link link = linkId == null ? null : network.getLinks().get( activity.getLinkId() );
			coord = link == null ? null : link.getCoord();
		}

		if (coord == null) {
			throw new RuntimeException( "could not resolve coord of activity "+activity );
		}

		return coord;
	}

	// /////////////////////////////////////////////////////////////////////////
	// for tests
	// /////////////////////////////////////////////////////////////////////////
	boolean isParked() {
		return isParked;
	}

	boolean lastLegWasCar()  {
		return lastLegWasCar;
	}

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		
	}
}

