/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxibus.tasks;

import java.util.*;

import org.matsim.contrib.taxibus.TaxibusRequest;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DriveTaskImpl;

/**
 * @author jbischoff
 *
 */
public class TaxibusDriveWithPassengersTask extends DriveTaskImpl implements TaxibusTaskWithRequests {
	private final Set<TaxibusRequest> requests;

	public TaxibusDriveWithPassengersTask(VrpPathWithTravelData path, Set<TaxibusRequest> requests) {
		super(path);
		this.requests = new HashSet<>(requests);
	}

	@Override
	public TaxibusTaskType getTaxibusTaskType() {
		return TaxibusTaskType.DRIVE_WITH_PASSENGERS;
	}

	@Override
	public Set<TaxibusRequest> getRequests() {
		return Collections.unmodifiableSet(requests);
	}
}
