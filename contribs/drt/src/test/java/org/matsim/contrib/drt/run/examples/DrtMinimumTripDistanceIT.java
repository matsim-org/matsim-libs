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
 * *********************************************************************** */

package org.matsim.contrib.drt.run.examples;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.passenger.DrtMinimumTripDistanceRequestValidator;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.drt.routing.MinimumTripDistanceAccessEgressFacilityFinder;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * Integration test for the {@code minimumTripDistance} of a DRT service, based on the Mielec example.
 *
 * @author nkuehnel / MOIA
 */
public class DrtMinimumTripDistanceIT {

	private static final double MINIMUM_TRIP_DISTANCE = 1000;

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testTripsBelowTheMinimumTripDistanceAreNotServed() {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		DrtConfigGroup.getSingleModeDrtConfig(config)
				.addOrGetDrtOptimizationConstraintsParams()
				.addOrGetDefaultDrtOptimizationConstraintsSet()
				.setMinimumTripDistance(MINIMUM_TRIP_DISTANCE);

		Controler controller = DrtControlerCreator.createControler(config, false);
		Tracker tracker = new Tracker();
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(tracker);
			}
		});
		controller.run();

		Network network = controller.getScenario().getNetwork();
		assertThat(tracker.submitted).isNotEmpty();
		assertThat(tracker.submitted).allSatisfy(event -> assertThat(
				MinimumTripDistanceAccessEgressFacilityFinder.calcTripDistance(
						network.getLinks().get(event.getFromLinkId()),
						network.getLinks().get(event.getToLinkId()))).isGreaterThanOrEqualTo(MINIMUM_TRIP_DISTANCE));

		// the short trips are not routed, so they become walk legs of routing mode 'drt' (the fallback routing module)
		assertThat(tracker.drtFallbackDepartures).isNotEmpty();

		// all trips were routed in this run, so the validator (the safety net) has nothing to reject
		assertThat(tracker.rejected).noneSatisfy(event -> assertThat(event.getCause()).contains(
				DrtMinimumTripDistanceRequestValidator.BELOW_MINIMUM_TRIP_DISTANCE_CAUSE));
	}

	private static class Tracker
			implements DrtRequestSubmittedEventHandler, PassengerRequestRejectedEventHandler,
			PersonDepartureEventHandler {

		private final List<DrtRequestSubmittedEvent> submitted = new ArrayList<>();
		private final List<PassengerRequestRejectedEvent> rejected = new ArrayList<>();
		private final List<PersonDepartureEvent> drtFallbackDepartures = new ArrayList<>();

		@Override
		public void handleEvent(DrtRequestSubmittedEvent event) {
			submitted.add(event);
		}

		@Override
		public void handleEvent(PassengerRequestRejectedEvent event) {
			rejected.add(event);
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			if (TransportMode.drt.equals(event.getRoutingMode()) && !TransportMode.drt.equals(event.getLegMode())) {
				drtFallbackDepartures.add(event);
			}
		}
	}
}
