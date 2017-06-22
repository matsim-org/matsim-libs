/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

import com.vividsolutions.jts.util.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import playground.sebhoerl.av.framework.InteractionConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Testing network spider creator.
 *
 * @author boescpa
 */
public class NetworkSpiderCreatorIT {

	private List<String> links = new ArrayList<>(2);
	private NetworkSpiderCreator spiderCreator;
	private Network network;

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Before
	public void prepareTests() {
		// prepare spider creator
		links.add("3233"); links.add("3242");
		spiderCreator = new NetworkSpiderCreator(links);
		// run simulation
		final Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("pt-tutorial"), "0.config.xml"));
		config.controler().setLastIteration(0);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		network = scenario.getNetwork();
		final Controler controler = new Controler(scenario);
		controler.getEvents().addHandler(spiderCreator);
		controler.run();
	}

	@Test
	public void testSpiderCreation() {
		// create SHP-spider:
		if (new File(utils.getOutputDirectory() + File.separator + "shpSpider3323").mkdir()) {
			spiderCreator.createSpiderSHP("3233",
					network,
					utils.getOutputDirectory() + File.separator + "shpSpider3323" + File.separator,
					"EPSG:4326");
		}
		// test spider:
		Map<String, Integer> spider3233 = spiderCreator.createAbsoluteSpider("3233");
		System.out.print(spider3233.toString());
		int val3233 = spider3233.get("3233");
		int valIn = spider3233.get("4232") + spider3233.get("3132");
		Assert.equals(val3233, valIn);
	}
}
