/* *********************************************************************** *
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

package org.matsim.contrib.drt.prebooking;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.drt.prebooking.logic.ProbabilityBasedPrebookingLogic;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;
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
 * Stop choice for prebooked requests in the stop-based Mielec example over several iterations: the insertion search
 * picks up or drops off some passengers at other than the routed stops, they reach these stops, only rejected
 * passengers get stuck,
 * and drt_legs reports the stops where the passengers actually departed.
 *
 * @author nkuehnel / MOIA
 */
class PrebookingStopCandidatesIT {
	private static final int LAST_ITERATION = 2;

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void prebookedRequestsUseOtherStopsThanRouted() throws IOException {
		Id.resetCaches();
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"),
				"mielec_stop_based_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(LAST_ITERATION);

		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(config);
		drtConfig.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet()
				.setMaxStopCandidates(3);
		PrebookingParams prebookingParams = new PrebookingParams();
		prebookingParams.setAbortRejectedPrebookings(false);
		drtConfig.addParameterSet(prebookingParams);

		Controler controller = DrtControlerCreator.createControler(config, false);
		// all requests prebooked well in advance
		ProbabilityBasedPrebookingLogic.install(controller, drtConfig, 1.0, 4.0 * 3600.0);
		StopTracker tracker = new StopTracker(drtConfig.getMode());
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(tracker);
			}
		});
		controller.run();

		// last iteration: only rejected passengers get stuck (standard drt behaviour), nobody because of stop choice
		assertThat(tracker.rejectedPersons).containsAll(tracker.stuckPersons);
		assertThat(tracker.scheduled).isPositive();
		int changed = tracker.changedPickups + tracker.changedDropoffs;
		assertThat(changed).as("requests served at other than the routed stops").isPositive();
		System.out.printf("stop choice: %d scheduled, %d other pickup stop, %d other dropoff stop%n",
				tracker.scheduled, tracker.changedPickups, tracker.changedDropoffs);

		// drt_legs reports the stop where the passenger departed with drt (pickup stop)
		Set<String> legPickups = readDrtLegsPickups(controller.getControllerIO()
				.getIterationFilename(LAST_ITERATION, "drt_legs_" + drtConfig.getMode() + ".csv"));
		assertThat(legPickups).isNotEmpty();
		assertThat(tracker.drtDepartures).containsAll(legPickups);
	}

	/**
	 * @return person id and pickup link of each row in drt_legs, as "person#link"
	 */
	private static Set<String> readDrtLegsPickups(String file) throws IOException {
		Set<String> pickups = new HashSet<>();
		try (BufferedReader reader = IOUtils.getBufferedReader(file)) {
			List<String> header = List.of(reader.readLine().split(";"));
			int person = header.indexOf("personId");
			int fromLink = header.indexOf("fromLinkId");
			String line;
			while ((line = reader.readLine()) != null) {
				String[] row = line.split(";");
				pickups.add(row[person] + "#" + row[fromLink]);
			}
		}
		return pickups;
	}

	private static final class StopTracker implements DrtRequestSubmittedEventHandler,
			PassengerRequestScheduledEventHandler, PassengerRequestRejectedEventHandler, PersonDepartureEventHandler,
			PersonStuckEventHandler {
		private final String mode;
		private final Map<Id<Request>, DrtRequestSubmittedEvent> submitted = new HashMap<>();
		private final Set<String> drtDepartures = new HashSet<>();
		private int scheduled;
		private int changedPickups;
		private int changedDropoffs;
		private final Set<Id<Person>> stuckPersons = new HashSet<>();
		private final Set<Id<Person>> rejectedPersons = new HashSet<>();

		private StopTracker(String mode) {
			this.mode = mode;
		}

		@Override
		public void handleEvent(DrtRequestSubmittedEvent event) {
			if (event.getMode().equals(mode)) {
				submitted.put(event.getRequestId(), event);
			}
		}

		@Override
		public void handleEvent(PassengerRequestScheduledEvent event) {
			DrtRequestSubmittedEvent request = submitted.get(event.getRequestId());
			if (request == null) {
				return;
			}
			scheduled++;
			Id<Link> pickup = event.getPickupLinkId().orElseThrow();
			Id<Link> dropoff = event.getDropoffLinkId().orElseThrow();
			if (!pickup.equals(request.getFromLinkId())) {
				changedPickups++;
			}
			if (!dropoff.equals(request.getToLinkId())) {
				changedDropoffs++;
			}
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			if (event.getLegMode().equals(mode)) {
				drtDepartures.add(event.getPersonId() + "#" + event.getLinkId());
			}
		}

		@Override
		public void handleEvent(PassengerRequestRejectedEvent event) {
			if (event.getMode().equals(mode)) {
				rejectedPersons.addAll(event.getPersonIds());
			}
		}

		@Override
		public void handleEvent(PersonStuckEvent event) {
			stuckPersons.add(event.getPersonId());
		}

		@Override
		public void reset(int iteration) {
			submitted.clear();
			drtDepartures.clear();
			scheduled = 0;
			changedPickups = 0;
			changedDropoffs = 0;
			stuckPersons.clear();
			rejectedPersons.clear();
		}
	}
}
