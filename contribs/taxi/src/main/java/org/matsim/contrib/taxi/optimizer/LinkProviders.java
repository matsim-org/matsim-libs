/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentDestinationData.DestEntry;
import org.matsim.contrib.taxi.scheduler.TaxiScheduleInquiry;
import org.matsim.contrib.util.LinkProvider;

/**
 * @author michalm
 */
public class LinkProviders {
	public static final LinkProvider<TaxiRequest> REQUEST_TO_FROM_LINK = req -> req.getFromLink();

	public static <D> LinkProvider<DestEntry<D>> createDestEntryToLink() {
		return new LinkProvider<DestEntry<D>>() {
			@Override
			public Link apply(DestEntry<D> dest) {
				return dest.link;
			}
		};
	}

	public static final LinkProvider<VehicleData.Entry> VEHICLE_ENTRY_TO_LINK = veh -> veh.link;

	public static LinkProvider<Vehicle> createImmediateDiversionOrEarliestIdlenessLinkProvider(
			final TaxiScheduleInquiry scheduleInquiry) {
		return new LinkProvider<Vehicle>() {
			public Link apply(Vehicle veh) {
				return scheduleInquiry.getImmediateDiversionOrEarliestIdleness(veh).link;
			}
		};
	}
}
