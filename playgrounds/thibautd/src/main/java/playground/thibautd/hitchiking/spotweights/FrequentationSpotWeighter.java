/* *********************************************************************** *
 * project: org.matsim.*
 * FrequentationSpotWeighter.java
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
package playground.thibautd.hitchiking.spotweights;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;

import playground.thibautd.hitchiking.HitchHikingConstants;

/**
 * {@link SpotWeighter} which weights depend on the number of passenger/drivers
 * having interacted with this spot (ie it is destination or success blind).
 *
 * It uses learning with linear forgetting to avoid to much fluctuation.
 *
 * CAUTION: the weights are NOT valid during the mobsim step.
 * @author thibautd
 */
public class FrequentationSpotWeighter implements SpotWeighter, AgentDepartureEventHandler {
	private final double learningRate;
	private final double baseWeight;

	private final Map<Id, MyDouble> weightsForDrivers = new TreeMap<Id, MyDouble>();
	private final Map<Id, MyDouble> weightsForPassengers = new TreeMap<Id, MyDouble>();

	private int currentIter = -1;

	public FrequentationSpotWeighter() {
		this( 0.5 , 50 );
	}

	public FrequentationSpotWeighter(
			final double learningRate,
			final double baseWeight) {
		this.learningRate = learningRate;
		this.baseWeight = baseWeight;
	}

	// ///////////////////////////////////////////////////////////////////////////
	// weight interface
	// ///////////////////////////////////////////////////////////////////////////
	@Override
	public double weightDriverOrigin(
			final double departureTime,
			final Id originLink,
			final Id destinationLink) {
		return baseWeight + getValue( originLink , weightsForDrivers ).value;
	}

	@Override
	public double weightPassengerOrigin(
			final double departureTime,
			final Id originLink,
			final Id destinationLink) {
		return baseWeight + getValue( originLink , weightsForPassengers ).value;
	}

	// ///////////////////////////////////////////////////////////////////////////
	// event handler interface
	// ///////////////////////////////////////////////////////////////////////////
	@Override
	public void reset(final int iteration) {
		if (iteration > currentIter) {
			currentIter = iteration;

			for (MyDouble v : weightsForDrivers.values()) {
				v.value *= learningRate;
			}
			for (MyDouble v : weightsForPassengers.values()) {
				v.value *= learningRate;
			}
		}
	}

	@Override
	public void handleEvent(final AgentDepartureEvent event) {
		final String mode = event.getLegMode();
		if (mode.equals( HitchHikingConstants.SIMULATED_DRIVER_MODE )) {
			getValue( event.getLinkId() , weightsForDrivers ).value++;
		}
		else if (mode.equals( HitchHikingConstants.PASSENGER_MODE )) {
			getValue( event.getLinkId() , weightsForPassengers ).value++;
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private static class MyDouble {
		public double value = Double.NaN;
	}

	private static MyDouble getValue(
			final Id key,
			final Map<Id, MyDouble> map) {
		MyDouble val = map.get( key );

		if (val == null) {
			val = new MyDouble();
			map.put( key , val );
		}

		return val;
	}
}

