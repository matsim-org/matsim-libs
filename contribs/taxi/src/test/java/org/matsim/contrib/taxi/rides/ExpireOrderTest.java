package org.matsim.contrib.taxi.rides;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.benchmark.RunTaxiBenchmark;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;

public class ExpireOrderTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testExpireOrder() {
		// TODO: create test scenario. Should be reserved for automated tests only.
		//       Grid network, dynamic vehicles, dynamic orders.
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("taxi-test-base"), "config.xml");

		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeTaxiConfigGroup(), new DvrpConfigGroup());
		TaxiConfigGroup taxiCfg = TaxiConfigGroup.getSingleModeTaxiConfig(config);
		taxiCfg.setBreakSimulationIfNotAllRequestsServed(false);
		taxiCfg.setMaxSearchDuration(15.0);
		// TODO: more config overrides

		// NOTE: These are already set in config.xml
		//config.plans().setInputFile("population_1.xml");
		//taxiCfg.setTaxisFile("vehicles_1.xml");

		config.controler().setOutputDirectory("abc");

		Controler controler = RunTaxiBenchmark.createControler(config, 1);
		controler.run();
	}
}
