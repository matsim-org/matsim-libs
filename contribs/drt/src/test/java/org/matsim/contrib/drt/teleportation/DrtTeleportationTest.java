package org.matsim.contrib.drt.teleportation;

import com.google.common.collect.Iterables;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.drt.estimator.DrtEstimatorModule;
import org.matsim.contrib.drt.estimator.impl.PessimisticDrtEstimator;
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.optimizer.constraints.DefaultDrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DrtTeleportationTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testTeleportationEngine() throws IOException {
		URL url = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(url, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup());
		config.network().setInputFile("network.xml");
		config.plans().setInputFile("plans_only_drt_1.0.xml.gz");
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(1);

		Controler controler = DrtControlerCreator.createControler(config, false);
		DrtConfigGroup drtConfigGroup = DrtConfigGroup.getSingleModeDrtConfig(config);
		DefaultDrtOptimizationConstraintsSet defaultConstraintsSet = (DefaultDrtOptimizationConstraintsSet) drtConfigGroup
				.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet();
		defaultConstraintsSet.maxTravelTimeAlpha = 1.2;
		defaultConstraintsSet.maxTravelTimeBeta = 600;
		defaultConstraintsSet.maxWaitTime = 300;
		DrtFareParams fareParams = new DrtFareParams();
		fareParams.baseFare = 1.0;
		fareParams.distanceFare_m = 0.001;
		drtConfigGroup.addParameterSet(fareParams);

		// Setup to enable estimator and teleportation
		drtConfigGroup.simulationType = DrtConfigGroup.SimulationType.estimateAndTeleport;

		// This uses the helper method to bind an estimator. Alternatively a separate modal module could also be created.
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				DrtEstimatorModule.bindEstimator(binder(), drtConfigGroup.mode).toInstance(new PessimisticDrtEstimator(drtConfigGroup));
			}
		});

		controler.run();

		Path csvPath = Path.of(utils.getOutputDirectory()).resolve("drt_customer_stats_drt.csv");

		try (CSVParser csv = new CSVParser(IOUtils.getBufferedReader(csvPath.toString()),
			CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setDelimiter(';').build())) {

			CSVRecord row = Iterables.getLast(csv);

			double waitAvg = Double.parseDouble(row.get("wait_average"));

			assertThat(waitAvg).isEqualTo(defaultConstraintsSet.maxWaitTime);

			double distMean = Double.parseDouble(row.get("distance_m_mean"));
			double directDistMean = Double.parseDouble(row.get("directDistance_m_mean"));

			assertThat(distMean / directDistMean).isCloseTo(defaultConstraintsSet.maxTravelTimeAlpha, Offset.offset(0.0001));

		}

	}
}
