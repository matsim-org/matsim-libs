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

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.utils.misc.Time;

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

	private double lastArrivalTime = Time.UNDEFINED_TIME;
	private boolean isParked = false;
	private boolean lastLegWasCar = false;

	public ParkAndRideScoringFunction(
			final ScoringFunction scoringFunction,
			final ParkingPenalty penalty) {
		this.delegate = scoringFunction;
		this.penalty = penalty;
	}

	@Override
	public void handleActivity(final Activity activity) {
		if (!ParkAndRideConstants.PARKING_ACT.equals( activity.getType() )) {
			delegate.handleActivity(activity);
		}

		if (lastLegWasCar && !activity.getType().equals( ParkAndRideConstants.PARKING_ACT )) {
			isParked = true;
			penalty.park( lastArrivalTime , activity.getCoord() );
		}
	}

	@Override
	public void handleLeg(final Leg leg) {
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

	@Override
	public void reset() {
		delegate.reset();
		penalty.reset();
	}
}

