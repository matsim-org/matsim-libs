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

package org.matsim.contrib.drt.passenger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.contrib.drt.passenger.DrtServiceTimeRequestValidator.OUTSIDE_SERVICE_AREA_ACCESS_CAUSE;
import static org.matsim.contrib.drt.passenger.DrtServiceTimeRequestValidator.OUTSIDE_SERVICE_AREA_EGRESS_CAUSE;
import static org.matsim.contrib.drt.passenger.DrtServiceTimeRequestValidator.OUTSIDE_SERVICE_TIME_CAUSE;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.drt.routing.DrtStopFacility;
import org.matsim.contrib.drt.routing.DrtStopFacilityImpl;
import org.matsim.contrib.drt.routing.DrtStopNetwork;
import org.matsim.contrib.drt.run.DrtServiceConfigurationParams;
import org.matsim.contrib.drt.run.DrtServiceConfigurations;
import org.matsim.contrib.drt.run.DrtServiceConfigurationsParams;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.router.AttributeBasedStopFinder;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import com.google.common.collect.ImmutableMap;

/**
 * @author nkuehnel / MOIA
 */
class DrtServiceTimeRequestValidatorTest {

	private final Network network = NetworkUtils.createNetwork();

	private final Link peakLink = link("peakLink");
	private final Link offpeakLink = link("offpeakLink");
	private final Link alwaysLink = link("alwaysLink");

	private final DrtStopNetwork stopNetwork = stopNetwork(stop(peakLink, "peak"), stop(offpeakLink, "offpeak"),
			stop(alwaysLink, "peak,offpeak"));

	@Test
	void testRequestInsideTheServiceTimeAndAreaIsAccepted() {
		var validator = new DrtServiceTimeRequestValidator(
				serviceConfigurations(serviceConfiguration("morning", 6 * 3600, 10 * 3600, "peak")));

		assertThat(validator.validateRequest(request(8 * 3600, peakLink, alwaysLink))).isEmpty();
	}

	@Test
	void testRequestOutsideAllServiceTimeWindowsIsRejected() {
		var validator = new DrtServiceTimeRequestValidator(
				serviceConfigurations(serviceConfiguration("morning", 6 * 3600, 10 * 3600, "peak"),
						serviceConfiguration("evening", 16 * 3600, 20 * 3600, "peak")));

		// before the first window
		assertThat(validator.validateRequest(request(5 * 3600, peakLink, alwaysLink))).containsExactly(
				OUTSIDE_SERVICE_TIME_CAUSE);
		// in the gap between the two windows
		assertThat(validator.validateRequest(request(13 * 3600, peakLink, alwaysLink))).containsExactly(
				OUTSIDE_SERVICE_TIME_CAUSE);
		// after the last window
		assertThat(validator.validateRequest(request(22 * 3600, peakLink, alwaysLink))).containsExactly(
				OUTSIDE_SERVICE_TIME_CAUSE);
	}

	@Test
	void testLinksNotServedInTheActiveRegimeAreRejected() {
		var validator = new DrtServiceTimeRequestValidator(
				serviceConfigurations(serviceConfiguration("morning", 6 * 3600, 10 * 3600, "peak")));

		assertThat(validator.validateRequest(request(8 * 3600, offpeakLink, alwaysLink))).containsExactly(
				OUTSIDE_SERVICE_AREA_ACCESS_CAUSE);
		assertThat(validator.validateRequest(request(8 * 3600, alwaysLink, offpeakLink))).containsExactly(
				OUTSIDE_SERVICE_AREA_EGRESS_CAUSE);
		assertThat(validator.validateRequest(request(8 * 3600, offpeakLink, offpeakLink))).containsExactlyInAnyOrder(
				OUTSIDE_SERVICE_AREA_ACCESS_CAUSE, OUTSIDE_SERVICE_AREA_EGRESS_CAUSE);
	}

	@Test
	void testWithoutStopsOnlyTheTimeIsChecked() {
		// door2door: the stop network is empty, so any link is served
		var validator = new DrtServiceTimeRequestValidator(
				serviceConfigurations(ImmutableMap::of, serviceConfiguration("morning", 6 * 3600, 10 * 3600, null)));

		assertThat(validator.validateRequest(request(8 * 3600, offpeakLink, offpeakLink))).isEmpty();
		assertThat(validator.validateRequest(request(13 * 3600, offpeakLink, offpeakLink))).containsExactly(
				OUTSIDE_SERVICE_TIME_CAUSE);
	}

	@Test
	void testEndOfServiceTimeIsSoft() {
		var validator = new DrtServiceTimeRequestValidator(
				serviceConfigurations(serviceConfiguration("morning", 6 * 3600, 10 * 3600, "peak")));

		// only the desired departure time counts; a request submitted or picked up later is still served
		DrtRequest request = DrtRequest.newBuilder()
				.id(Id.create("request", Request.class))
				.submissionTime(11 * 3600)
				.earliestDepartureTime(10 * 3600 - 1)
				.mode("drt")
				.fromLink(peakLink)
				.toLink(alwaysLink)
				.build();

		assertThat(validator.validateRequest(request)).isEmpty();
	}

	private DrtRequest request(double earliestDepartureTime, Link fromLink, Link toLink) {
		return DrtRequest.newBuilder()
				.id(Id.create("request", Request.class))
				.submissionTime(earliestDepartureTime)
				.earliestDepartureTime(earliestDepartureTime)
				.mode("drt")
				.fromLink(fromLink)
				.toLink(toLink)
				.build();
	}

	private Link link(String id) {
		Node fromNode = NetworkUtils.createAndAddNode(network, Id.createNodeId(id + "_from"), new Coord(0, 0));
		Node toNode = NetworkUtils.createAndAddNode(network, Id.createNodeId(id + "_to"), new Coord(100, 100));
		return NetworkUtils.createAndAddLink(network, Id.createLinkId(id), fromNode, toNode, 1000, 10, 1000, 1);
	}

	private static DrtStopFacility stop(Link link, String stopNetworks) {
		AttributesImpl attributes = new AttributesImpl();
		attributes.putAttribute(AttributeBasedStopFinder.FACILITY_STOP_NETWORKS_ATTRIBUTE, stopNetworks);
		return new DrtStopFacilityImpl(Id.create(link.getId(), DrtStopFacility.class), link.getId(), link.getToNode()
				.getCoord(), attributes);
	}

	private static DrtStopNetwork stopNetwork(DrtStopFacility... stops) {
		ImmutableMap.Builder<Id<DrtStopFacility>, DrtStopFacility> builder = ImmutableMap.builder();
		for (DrtStopFacility stop : stops) {
			builder.put(stop.getId(), stop);
		}
		ImmutableMap<Id<DrtStopFacility>, DrtStopFacility> drtStops = builder.build();
		return () -> drtStops;
	}

	private DrtServiceConfigurations serviceConfigurations(DrtServiceConfigurationParams... serviceConfigurations) {
		return serviceConfigurations(stopNetwork, serviceConfigurations);
	}

	private static DrtServiceConfigurations serviceConfigurations(DrtStopNetwork stopNetwork,
			DrtServiceConfigurationParams... serviceConfigurations) {
		DrtServiceConfigurationsParams params = new DrtServiceConfigurationsParams();
		for (DrtServiceConfigurationParams serviceConfiguration : serviceConfigurations) {
			params.addParameterSet(serviceConfiguration);
		}
		return new DrtServiceConfigurations(params, stopNetwork);
	}

	private static DrtServiceConfigurationParams serviceConfiguration(String name, double startTime, double endTime,
			String stopNetwork) {
		DrtServiceConfigurationParams serviceConfiguration = new DrtServiceConfigurationParams(name);
		serviceConfiguration.setStartTime(OptionalTime.defined(startTime));
		serviceConfiguration.setEndTime(OptionalTime.defined(endTime));
		serviceConfiguration.setStopNetwork(stopNetwork);
		return serviceConfiguration;
	}
}
