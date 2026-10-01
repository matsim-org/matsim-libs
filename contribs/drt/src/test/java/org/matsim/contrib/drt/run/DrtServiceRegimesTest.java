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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.matsim.contrib.drt.run.DrtServiceRegimesFixtures.serviceRegime;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.routing.DrtStopFacility;
import org.matsim.contrib.drt.routing.DrtStopFacilityImpl;
import org.matsim.contrib.drt.routing.DrtStopNetwork;
import org.matsim.contrib.drt.run.DrtServiceRegimes.Regime;
import org.matsim.contrib.dvrp.router.AttributeBasedStopFinder;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableMap;

/**
 * @author nkuehnel / MOIA
 */
class DrtServiceRegimesTest {

	private final DrtStopFacility peakStop = stop("peakStop", "peak");
	private final DrtStopFacility offpeakStop = stop("offpeakStop", "offpeak");
	private final DrtStopFacility alwaysStop = stop("alwaysStop", "peak,offpeak");
	private final DrtStopFacility unassignedStop = stop("unassignedStop", null);

	private final DrtStopNetwork stopNetwork = () -> ImmutableMap.of(peakStop.getId(), peakStop, offpeakStop.getId(),
			offpeakStop, alwaysStop.getId(), alwaysStop, unassignedStop.getId(), unassignedStop);

	@Test
	void testActiveRegimeFollowsTheHalfOpenWindows() {
		DrtServiceRegimes serviceRegimes = serviceRegimes(
				serviceRegime("morning", OptionalTime.defined(6 * 3600), OptionalTime.defined(10 * 3600), null),
				serviceRegime("evening", OptionalTime.defined(16 * 3600), OptionalTime.defined(20 * 3600), null));

		assertThat(serviceRegimes.getActiveRegime(6 * 3600)).map(Regime::name).contains("morning");
		assertThat(serviceRegimes.getActiveRegime(10 * 3600 - 1)).map(Regime::name).contains("morning");
		// endTime is exclusive
		assertThat(serviceRegimes.getActiveRegime(10 * 3600)).isEmpty();
		assertThat(serviceRegimes.getActiveRegime(6 * 3600 - 1)).isEmpty();
		assertThat(serviceRegimes.getActiveRegime(18 * 3600)).map(Regime::name).contains("evening");
	}

	@Test
	void testUndefinedTimesMeanOpenWindows() {
		DrtServiceRegimes serviceRegimes = serviceRegimes(
				serviceRegime("allDay", OptionalTime.undefined(), OptionalTime.undefined(), null));

		assertThat(serviceRegimes.getActiveRegime(0)).map(Regime::name).contains("allDay");
		assertThat(serviceRegimes.getActiveRegime(48 * 3600)).map(Regime::name).contains("allDay");
	}

	@Test
	void testGapBetweenTwoWindowsIsNotCovered() {
		DrtServiceRegimes serviceRegimes = serviceRegimes(
				serviceRegime("morning", OptionalTime.undefined(), OptionalTime.defined(10 * 3600), null),
				serviceRegime("evening", OptionalTime.defined(16 * 3600), OptionalTime.undefined(), null));

		assertThat(serviceRegimes.getActiveRegime(13 * 3600)).isEmpty();
	}

	@Test
	void testStopsAreFilteredByStopNetwork() {
		DrtServiceRegimes serviceRegimes = serviceRegimes(
				serviceRegime("morning", OptionalTime.undefined(), OptionalTime.defined(10 * 3600), "peak"),
				serviceRegime("midday", OptionalTime.defined(10 * 3600), OptionalTime.defined(16 * 3600),
						"offpeak"),
				serviceRegime("evening", OptionalTime.defined(16 * 3600), OptionalTime.undefined(), null));

		Regime morning = serviceRegimes.getActiveRegime(8 * 3600).orElseThrow();
		assertThat(morning.stops()).containsExactlyInAnyOrder(peakStop, alwaysStop);

		Regime midday = serviceRegimes.getActiveRegime(13 * 3600).orElseThrow();
		assertThat(midday.stops()).containsExactlyInAnyOrder(offpeakStop, alwaysStop);

		// a service regime without stopNetwork serves all stops, including those without the attribute
		Regime evening = serviceRegimes.getActiveRegime(18 * 3600).orElseThrow();
		assertThat(evening.stops()).containsExactlyInAnyOrder(peakStop, offpeakStop, alwaysStop, unassignedStop);
	}

	@Test
	void testLinkIdsMatchTheStops() {
		DrtServiceRegimes serviceRegimes = serviceRegimes(
				serviceRegime("morning", OptionalTime.undefined(), OptionalTime.undefined(), "peak"));

		Regime morning = serviceRegimes.getActiveRegime(8 * 3600).orElseThrow();
		assertThat(morning.linkIds()).containsExactlyInAnyOrder(peakStop.getLinkId(), alwaysStop.getLinkId());
	}

	@Test
	void testStopNetworkWithoutAnyStopIsRejected() {
		assertThatExceptionOfType(VerifyException.class).isThrownBy(() -> serviceRegimes(
						serviceRegime("night", OptionalTime.undefined(), OptionalTime.undefined(), "nightStops")))
				.withMessageContaining("nightStops")
				.withMessageContaining("night");
	}

	private DrtServiceRegimes serviceRegimes(DrtServiceRegimeParams... serviceRegimes) {
		return DrtServiceRegimesFixtures.serviceRegimes(stopNetwork, serviceRegimes);
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
