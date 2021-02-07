/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.insertion;

import java.util.Collection;
import java.util.Optional;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.passenger.DrtRequest;

/**
 * @author michalm
 */
public interface DrtInsertionSearch<D> {
	Optional<InsertionWithDetourData<D>> findBestInsertion(DrtRequest drtRequest,
			Collection<VehicleEntry> vehicleEntries);
}
