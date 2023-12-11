/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.minibus.schedule;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.routeProvider.PScenarioHelper;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;


public class CreateStopsForAllCarLinksTest {
	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	final void testCreateStopsForAllCarLinks() {

		Network net = PScenarioHelper.createTestNetwork().getNetwork();
		PConfigGroup pC = new PConfigGroup();

		int numberOfCarLinks = 0;
		for (Link link : net.getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.car)) {
				numberOfCarLinks++;
			}
		}

		TransitSchedule transitSchedule = CreateStopsForAllCarLinks.createStopsForAllCarLinks(net, pC);

		int numberOfParaStops = 0;
		for (TransitStopFacility stopFacility : transitSchedule.getFacilities().values()) {
			if (stopFacility.getId().toString().startsWith(pC.getPIdentifier())) {
				numberOfParaStops++;
			}
		}

		Assertions.assertEquals(numberOfCarLinks, numberOfParaStops, MatsimTestUtils.EPSILON, "All car links got a paratransit stop");

		TransitScheduleFactoryImpl tSF = new TransitScheduleFactoryImpl();

		TransitSchedule realTransitSchedule = tSF.createTransitSchedule();
		TransitStopFacility stop1 = tSF.createTransitStopFacility(Id.create("1314", TransitStopFacility.class), new Coord(0.0, 0.0), false);
		stop1.setLinkId(Id.create("1314", Link.class));
		realTransitSchedule.addStopFacility(stop1);

		transitSchedule = CreateStopsForAllCarLinks.createStopsForAllCarLinks(net, pC, realTransitSchedule);

		numberOfParaStops = 0;
		for (TransitStopFacility stopFacility : transitSchedule.getFacilities().values()) {
			if (stopFacility.getId().toString().startsWith(pC.getPIdentifier())) {
				numberOfParaStops++;
			}
		}

		Assertions.assertEquals(numberOfCarLinks - 1, numberOfParaStops, MatsimTestUtils.EPSILON, "All car links minus one stop from formal transit got a paratransit stop");

	}

}
