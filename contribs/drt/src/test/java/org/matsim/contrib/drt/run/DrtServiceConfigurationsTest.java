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

package org.matsim.contrib.drt.run;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.routing.DrtStopFacility;
import org.matsim.contrib.drt.routing.DrtStopFacilityImpl;
import org.matsim.contrib.drt.routing.DrtStopNetwork;
import org.matsim.contrib.drt.run.DrtServiceConfigurations.Regime;
import org.matsim.contrib.dvrp.router.AttributeBasedStopFinder;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import com.google.common.collect.ImmutableMap;

/**
 * @author nkuehnel / MOIA
 */
class DrtServiceConfigurationsTest {

	private final DrtStopFacility peakStop = stop("peakStop", "peak");
	private final DrtStopFacility offpeakStop = stop("offpeakStop", "offpeak");
	private final DrtStopFacility alwaysStop = stop("alwaysStop", "peak,offpeak");
	private final DrtStopFacility unassignedStop = stop("unassignedStop", null);

	private final DrtStopNetwork stopNetwork = () -> ImmutableMap.of(peakStop.getId(), peakStop, offpeakStop.getId(),
			offpeakStop, alwaysStop.getId(), alwaysStop, unassignedStop.getId(), unassignedStop);

	@Test
	void testActiveRegimeFollowsTheHalfOpenWindows() {
		DrtServiceConfigurations serviceConfigurations = serviceConfigurations(
				serviceConfiguration("morning", OptionalTime.defined(6 * 3600), OptionalTime.defined(10 * 3600), null),
				serviceConfiguration("evening", OptionalTime.defined(16 * 3600), OptionalTime.defined(20 * 3600), null));

		assertThat(serviceConfigurations.getActiveRegime(6 * 3600)).map(Regime::name).contains("morning");
		assertThat(serviceConfigurations.getActiveRegime(10 * 3600 - 1)).map(Regime::name).contains("morning");
		// endTime is exclusive
		assertThat(serviceConfigurations.getActiveRegime(10 * 3600)).isEmpty();
		assertThat(serviceConfigurations.getActiveRegime(6 * 3600 - 1)).isEmpty();
		assertThat(serviceConfigurations.getActiveRegime(18 * 3600)).map(Regime::name).contains("evening");
	}

	@Test
	void testUndefinedTimesMeanOpenWindows() {
		DrtServiceConfigurations serviceConfigurations = serviceConfigurations(
				serviceConfiguration("allDay", OptionalTime.undefined(), OptionalTime.undefined(), null));

		assertThat(serviceConfigurations.getActiveRegime(0)).map(Regime::name).contains("allDay");
		assertThat(serviceConfigurations.getActiveRegime(48 * 3600)).map(Regime::name).contains("allDay");
	}

	@Test
	void testGapBetweenTwoWindowsIsNotCovered() {
		DrtServiceConfigurations serviceConfigurations = serviceConfigurations(
				serviceConfiguration("morning", OptionalTime.undefined(), OptionalTime.defined(10 * 3600), null),
				serviceConfiguration("evening", OptionalTime.defined(16 * 3600), OptionalTime.undefined(), null));

		assertThat(serviceConfigurations.getActiveRegime(13 * 3600)).isEmpty();
	}

	@Test
	void testStopsAreFilteredByStopNetwork() {
		DrtServiceConfigurations serviceConfigurations = serviceConfigurations(
				serviceConfiguration("morning", OptionalTime.undefined(), OptionalTime.defined(10 * 3600), "peak"),
				serviceConfiguration("midday", OptionalTime.defined(10 * 3600), OptionalTime.defined(16 * 3600),
						"offpeak"),
				serviceConfiguration("evening", OptionalTime.defined(16 * 3600), OptionalTime.undefined(), null));

		Regime morning = serviceConfigurations.getActiveRegime(8 * 3600).orElseThrow();
		assertThat(morning.stops()).containsExactlyInAnyOrder(peakStop, alwaysStop);

		Regime midday = serviceConfigurations.getActiveRegime(13 * 3600).orElseThrow();
		assertThat(midday.stops()).containsExactlyInAnyOrder(offpeakStop, alwaysStop);

		// a service configuration without stopNetwork serves all stops, including those without the attribute
		Regime evening = serviceConfigurations.getActiveRegime(18 * 3600).orElseThrow();
		assertThat(evening.stops()).containsExactlyInAnyOrder(peakStop, offpeakStop, alwaysStop, unassignedStop);
	}

	@Test
	void testLinkIdsMatchTheStops() {
		DrtServiceConfigurations serviceConfigurations = serviceConfigurations(
				serviceConfiguration("morning", OptionalTime.undefined(), OptionalTime.undefined(), "peak"));

		Regime morning = serviceConfigurations.getActiveRegime(8 * 3600).orElseThrow();
		assertThat(morning.linkIds()).containsExactlyInAnyOrder(peakStop.getLinkId(), alwaysStop.getLinkId());
	}

	private static DrtServiceConfigurations serviceConfigurations(DrtStopNetwork stopNetwork,
			DrtServiceConfigurationParams... serviceConfigurations) {
		DrtServiceConfigurationsParams params = new DrtServiceConfigurationsParams();
		for (DrtServiceConfigurationParams serviceConfiguration : serviceConfigurations) {
			params.addParameterSet(serviceConfiguration);
		}
		return new DrtServiceConfigurations(params, stopNetwork);
	}

	private DrtServiceConfigurations serviceConfigurations(DrtServiceConfigurationParams... serviceConfigurations) {
		return serviceConfigurations(stopNetwork, serviceConfigurations);
	}

	private static DrtServiceConfigurationParams serviceConfiguration(String name, OptionalTime startTime,
			OptionalTime endTime, String stopNetwork) {
		DrtServiceConfigurationParams serviceConfiguration = new DrtServiceConfigurationParams(name);
		serviceConfiguration.setStartTime(startTime);
		serviceConfiguration.setEndTime(endTime);
		serviceConfiguration.setStopNetwork(stopNetwork);
		return serviceConfiguration;
	}

	private static DrtStopFacility stop(String id, String stopNetworks) {
		AttributesImpl attributes = new AttributesImpl();
		if (stopNetworks != null) {
			attributes.putAttribute(AttributeBasedStopFinder.FACILITY_STOP_NETWORKS_ATTRIBUTE, stopNetworks);
		}
		return new DrtStopFacilityImpl(Id.create(id, DrtStopFacility.class), Id.createLinkId(id + "_link"),
				new Coord(0, 0), attributes);
	}
}
