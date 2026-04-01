/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.routing;

import com.google.common.collect.ImmutableMap;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;

/**
 * @author Michal Maciejewski (michalm)
 */
public interface DrtStopNetwork {
	ImmutableMap<Id<DrtStopFacility>, DrtStopFacility> getDrtStops();

	static TransitSchedule toTransitSchedule(DrtStopNetwork stopNetwork) {
		TransitScheduleFactoryImpl transitScheduleFactory = new TransitScheduleFactoryImpl();
		TransitSchedule transitSchedule = transitScheduleFactory.createTransitSchedule();

		for (DrtStopFacility stopFacility : stopNetwork.getDrtStops().values()) {
			TransitStopFacility transitStopFacility = transitScheduleFactory.createTransitStopFacility(Id.create(stopFacility.getId(), TransitStopFacility.class), stopFacility.getCoord(), false);
			transitStopFacility.setLinkId(stopFacility.getLinkId());
			AttributesUtils.copyAttributesFromTo(stopFacility, transitStopFacility);
			transitSchedule.addStopFacility(transitStopFacility);
		}

		return transitSchedule;
	}
}
