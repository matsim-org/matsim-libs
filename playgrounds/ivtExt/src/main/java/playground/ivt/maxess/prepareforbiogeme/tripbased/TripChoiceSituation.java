/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ivt.maxess.prepareforbiogeme.tripbased;

import org.matsim.core.router.TripStructureUtils;
import playground.ivt.maxess.prepareforbiogeme.framework.ChoiceSituation;

import java.util.List;

/**
 * @author thibautd
 */
public class TripChoiceSituation implements ChoiceSituation<Trip> {
	private final Trip trip;
	private final List<TripStructureUtils.Trip> tripSequence;
	private final int positionInTripSequence;

	public TripChoiceSituation(
			final Trip trip,
			final List<TripStructureUtils.Trip> tripSequence,
			final int positionInTripSequence) {
		this.trip = trip;
		this.tripSequence = tripSequence;
		this.positionInTripSequence = positionInTripSequence;
	}

	@Override
	public Trip getChoice() {
		return trip;
	}

	public List<TripStructureUtils.Trip> getTripSequence() {
		return tripSequence;
	}

	public int getPositionInTripSequence() {
		return positionInTripSequence;
	}
}
