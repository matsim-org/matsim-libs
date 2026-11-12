/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.passenger;

import java.util.Set;

import org.matsim.contrib.drt.routing.DrtPickupDropoffRestriction;
import org.matsim.contrib.drt.routing.PickupDropoffRestrictionAccessEgressFacilityFinder;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;

/**
 * Rejects requests whose pickup and dropoff links violate the {@link DrtPickupDropoffRestriction}.
 * <p>
 * The restriction is primarily enforced during routing (see {@link PickupDropoffRestrictionAccessEgressFacilityFinder}).
 * This validator is the safety net for requests that reach the optimizer without having been routed in this run
 * (prebooking, within-day replanning, input plans carrying finished DRT routes).
 */
public class DrtPickupDropoffRestrictionRequestValidator implements PassengerRequestValidator {

	public static final String PICKUP_DROPOFF_RESTRICTION_CAUSE = "violates_pickup_dropoff_restriction";

	private final DrtPickupDropoffRestriction restriction;

	public DrtPickupDropoffRestrictionRequestValidator(DrtPickupDropoffRestriction restriction) {
		this.restriction = restriction;
	}

	@Override
	public Set<String> validateRequest(PassengerRequest request) {
		return restriction.isAllowed(request.getFromLink(), request.getToLink()) ?
				Set.of() :
				Set.of(PICKUP_DROPOFF_RESTRICTION_CAUSE);
	}
}
