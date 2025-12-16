/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
package ch.sbb.matsim.routing.pt.raptor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author mrieser / SBB
 */
public class SwissRailRaptorDataTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testTransfersFromSchedule() {
		Fixture f = new Fixture();
		f.init();

		f.config.transitRouter().setMaxBeelineWalkConnectionDistance(100);
		RaptorStaticConfig raptorConfig = RaptorUtils.createStaticConfig(f.config);
		SwissRailRaptorData data = SwissRailRaptorData.create(f.schedule, null, raptorConfig, f.network, null);

		// check there is no transfer between stopFacility 19 and stopFacility 9
		Id<TransitStopFacility> stopId5 = Id.create(5, TransitStopFacility.class);
		Id<TransitStopFacility> stopId9 = Id.create(9, TransitStopFacility.class);
		Id<TransitStopFacility> stopId18 = Id.create(18, TransitStopFacility.class);
		Id<TransitStopFacility> stopId19 = Id.create(19, TransitStopFacility.class);
		for (SwissRailRaptorData.RTransfer t : data.transfers) {
			TransitStopFacility fromStop = data.routeStops[t.fromRouteStop].routeStop.getStopFacility();
			TransitStopFacility toStop = data.routeStops[t.toRouteStop].routeStop.getStopFacility();
			if (fromStop.getId().equals(stopId19) && toStop.getId().equals(stopId9)) {
				Assertions.fail("There should not be any transfer between stop facilities 19 and 9.");
			}
		}

		// add a previously inexistant transfer

		f.schedule.getMinimalTransferTimes().set(stopId19, stopId9, 345);
		SwissRailRaptorData data2 = SwissRailRaptorData.create(f.schedule, null, raptorConfig, f.network, null);
		int foundTransferCount = 0;
		for (SwissRailRaptorData.RTransfer t : data2.transfers) {
			TransitStopFacility fromStop = data2.routeStops[t.fromRouteStop].routeStop.getStopFacility();
			TransitStopFacility toStop = data2.routeStops[t.toRouteStop].routeStop.getStopFacility();
			if (fromStop.getId().equals(stopId19) && toStop.getId().equals(stopId9)) {
				foundTransferCount++;
			}
		}
		Assertions.assertEquals(1, foundTransferCount, "wrong number of transfers between stop facilities 19 and 9.");
		Assertions.assertEquals(data.transfers.length + 1, data2.transfers.length, "number of transfers should have incrased.");

		// assign a high transfer time to a "default" transfer
		f.schedule.getMinimalTransferTimes().set(stopId5, stopId18, 456);
		SwissRailRaptorData data3 = SwissRailRaptorData.create(f.schedule, null, raptorConfig, f.network, null);
		boolean foundCorrectTransfer = false;
		for (SwissRailRaptorData.RTransfer t : data3.transfers) {
			TransitStopFacility fromStop = data3.routeStops[t.fromRouteStop].routeStop.getStopFacility();
			TransitStopFacility toStop = data3.routeStops[t.toRouteStop].routeStop.getStopFacility();
			if (fromStop.getId().equals(stopId5) && toStop.getId().equals(stopId18)) {
				Assertions.assertEquals(456, t.transferTime, "transfer has wrong transfer time.");
				foundCorrectTransfer = true;
			}
		}
		Assertions.assertTrue(foundCorrectTransfer, "did not find overwritten transfer");
		Assertions.assertEquals(data2.transfers.length, data3.transfers.length, "number of transfers should have stayed the same.");

		// assign a low transfer time to a "default" transfer
		f.schedule.getMinimalTransferTimes().set(stopId5, stopId18, 0.2);
		SwissRailRaptorData data4 = SwissRailRaptorData.create(f.schedule, null, raptorConfig, f.network, null);
		foundCorrectTransfer = false;
		for (SwissRailRaptorData.RTransfer t : data4.transfers) {
			TransitStopFacility fromStop = data4.routeStops[t.fromRouteStop].routeStop.getStopFacility();
			TransitStopFacility toStop = data4.routeStops[t.toRouteStop].routeStop.getStopFacility();
			if (fromStop.getId().equals(stopId5) && toStop.getId().equals(stopId18)) {
				Assertions.assertEquals(1, t.transferTime, "transfer has wrong transfer time."); // transferTime gets rounded up to int vlues
				foundCorrectTransfer = true;
			}
		}
		Assertions.assertTrue(foundCorrectTransfer, "did not find overwritten transfer");
		Assertions.assertEquals(data2.transfers.length, data4.transfers.length, "number of transfers should have stayed the same.");
	}


	@Test
	void testChainedDepartures() {

		String input = utils.getPackageInputDirectory();

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		new TransitScheduleReader(scenario).readFile(input + "schedule.xml");
		new MatsimNetworkReader(scenario.getNetwork()).readFile(input + "network.xml");

		RaptorStaticConfig raptorConfig = RaptorUtils.createStaticConfig(config);

		SwissRailRaptorData data = SwissRailRaptorData.create(scenario.getTransitSchedule(), null, raptorConfig, scenario.getNetwork(), null);

		assertThat(data.chainedDepartures)
			.hasSize(136);

		Map<Integer, Long> counts = data.chainedDepartures.values().stream()
			.collect(Collectors.groupingBy(
				d -> d.length,
				Collectors.counting()
			));

		assertThat(counts)
			.hasSize(2)
			.containsEntry(1, 117L)
			.containsEntry(2, 19L);

	}

}
