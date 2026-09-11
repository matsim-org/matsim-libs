/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.router.AttributeBasedStopFinder;
import org.matsim.core.network.NetworkUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author nkuehnel / MOIA
 */
class DrtStopFacilityImplTest {

	@Test
	void testCreateFromLinkKeepsTheLinkAttributes() {
		Network network = NetworkUtils.createNetwork();
		Node fromNode = NetworkUtils.createAndAddNode(network, Id.createNodeId("a"), new Coord(0, 0));
		Node toNode = NetworkUtils.createAndAddNode(network, Id.createNodeId("b"), new Coord(100, 200));
		Link link = NetworkUtils.createAndAddLink(network, Id.createLinkId("ab"), fromNode, toNode, 1000, 10, 1000, 1);
		link.getAttributes().putAttribute(AttributeBasedStopFinder.FACILITY_STOP_NETWORKS_ATTRIBUTE, "peak");

		DrtStopFacility stop = DrtStopFacilityImpl.createFromLink(link);

		assertThat(stop.getId().toString()).isEqualTo("ab");
		assertThat(stop.getLinkId()).isEqualTo(link.getId());
		assertThat(stop.getCoord()).isEqualTo(toNode.getCoord());
		assertThat(AttributeBasedStopFinder.parseStopNetworks(stop)).containsExactly("peak");
	}

	@Test
	void testCreateFromFacilityKeepsTheFacilityAttributes() {
		TransitStopFacility facility = new TransitScheduleFactoryImpl().createTransitStopFacility(
				Id.create("stop", TransitStopFacility.class), new Coord(100, 200), false);
		facility.setLinkId(Id.createLinkId("ab"));
		facility.getAttributes().putAttribute(AttributeBasedStopFinder.FACILITY_STOP_NETWORKS_ATTRIBUTE, "peak,offpeak");

		DrtStopFacility stop = DrtStopFacilityImpl.createFromFacility(facility);

		assertThat(stop.getId().toString()).isEqualTo("stop");
		assertThat(stop.getLinkId()).isEqualTo(facility.getLinkId());
		assertThat(stop.getCoord()).isEqualTo(facility.getCoord());
		assertThat(AttributeBasedStopFinder.parseStopNetworks(stop)).containsExactlyInAnyOrder("peak", "offpeak");
	}
}
