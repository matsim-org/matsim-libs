/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package contrib.baseline.counts;

import com.vividsolutions.jts.util.*;
import org.junit.*;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Testing NetworkAdapter.
 *
 * @author boescpa
 */
public class NetworkAdapterIT {

	private static boolean setUpIsDone = false;
	private static NetworkSpiderCreator spiderCreator;
	private static Network network;

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Before
	public void prepareTests() {
		if (setUpIsDone) return;
		// prepare spider creator
		List<String> links = new ArrayList<>(2);
		links.add("3323");links.add("4232");
		spiderCreator = new NetworkSpiderCreator(links);
		// run simulation
		final Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("pt-tutorial"), "0.config.xml"));
		config.controler().setLastIteration(0);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		network = scenario.getNetwork();
		final Controler controler = new Controler(scenario);
		controler.getEvents().addHandler(spiderCreator);
		controler.run();
		setUpIsDone = true;
	}

	@Test
	public void testNetworkAdaptorOneCountStation() {
		NetworkAdapter networkAdapter = new NetworkAdapter(network);
		Map<String, Integer> expectedCounts = new HashMap<>();
		expectedCounts.put("3323", 150);
		Map<String, Integer> observedCounts = new HashMap<>();
		observedCounts.put("3323", spiderCreator.createAbsoluteSpider("3323").get("3323"));
		List<NetworkChangeEvent> changeEvents =
				networkAdapter.identifyNetworkChanges(expectedCounts, observedCounts, spiderCreator.createAllRelativeSpiders());
		// Output:
		System.out.println("Expected: " + expectedCounts.get("3323") + "; Observed: " + observedCounts.get("3323"));
		for (NetworkChangeEvent changeEvent : changeEvents) {
			System.out.println("Link: " + changeEvent.getLinks().toString() + ";" +
					" Change Factor: " + changeEvent.getFlowCapacityChange().getValue());
		}
	}

	@Test
	public void testNetworkAdaptorTwoCountStations() {
		NetworkAdapter networkAdapter = new NetworkAdapter(network);
		Map<String, Integer> expectedCounts = new HashMap<>();
		expectedCounts.put("3323", 150); expectedCounts.put("4232", expectedCounts.get("3323"));
		Map<String, Integer> observedCounts = new HashMap<>();
		observedCounts.put("3323", spiderCreator.createAbsoluteSpider("3323").get("3323"));
		observedCounts.put("4232", spiderCreator.createAbsoluteSpider("4232").get("4232"));
		List<NetworkChangeEvent> changeEvents =
				networkAdapter.identifyNetworkChanges(expectedCounts, observedCounts, spiderCreator.createAllRelativeSpiders());
		// Output:
		System.out.println("3323 - Expected: " + expectedCounts.get("3323") + "; Observed: " + observedCounts.get("3323"));
		System.out.println("4232 - Expected: " + expectedCounts.get("4232") + "; Observed: " + observedCounts.get("4232"));
		for (NetworkChangeEvent changeEvent : changeEvents) {
			System.out.println("Link: " + changeEvent.getLinks().toString() + ";" +
					" Change Factor: " + changeEvent.getFlowCapacityChange().getValue());
		}
	}

	@Test
	public void testScalingOfCountStationImportance() {
		NetworkAdapter networkAdapter = new NetworkAdapter(network);
		Map<String, Integer> expectedCounts = new HashMap<>();
		expectedCounts.put("3323", 150);
		Map<String, Integer> observedCounts = new HashMap<>();
		observedCounts.put("3323", spiderCreator.createAbsoluteSpider("3323").get("3323"));
		observedCounts.put("4232", spiderCreator.createAbsoluteSpider("4232").get("4232"));
		expectedCounts.put("4232", observedCounts.get("4232") - (expectedCounts.get("3323") - observedCounts.get("4232")));
		List<NetworkChangeEvent> changeEvents =
				networkAdapter.identifyNetworkChanges(expectedCounts, observedCounts, spiderCreator.createAllRelativeSpiders());
		// Output:
		System.out.println("3323 - Expected: " + expectedCounts.get("3323") + "; Observed: " + observedCounts.get("3323"));
		System.out.println("4232 - Expected: " + expectedCounts.get("4232") + "; Observed: " + observedCounts.get("4232"));
		for (NetworkChangeEvent changeEvent : changeEvents) {
			System.out.println("Link: " + changeEvent.getLinks().toString() + ";" +
					" Change Factor: " + changeEvent.getFlowCapacityChange().getValue());
		}
	}
}
