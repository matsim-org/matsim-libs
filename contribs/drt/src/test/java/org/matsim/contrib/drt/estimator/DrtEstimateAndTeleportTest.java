package org.matsim.contrib.drt.estimator;

import com.google.common.collect.Iterables;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.drt.estimator.impl.DirectTripBasedDrtEstimator;
import org.matsim.contrib.drt.estimator.impl.acceptance_estimation.UniformRejectionEstimator;
import org.matsim.contrib.drt.estimator.impl.distribution.NormalDistributionGenerator;
import org.matsim.contrib.drt.estimator.impl.trip_estimation.ConstantRideDurationEstimator;
import org.matsim.contrib.drt.estimator.impl.waiting_time_estimation.ConstantWaitingTimeEstimator;
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

public class DrtEstimateAndTeleportTest {
	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testDrtEstimateAndTeleport() throws IOException {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
			new OTFVisConfigGroup());
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		DrtConfigGroup drtConfigGroup = DrtConfigGroup.getSingleModeDrtConfig(config);
		drtConfigGroup.setSimulationType(DrtConfigGroup.SimulationType.estimateAndTeleport);
		drtConfigGroup.addParameterSet(new DrtEstimatorParams());

		Controler controler = DrtControlerCreator.createControler(config, false);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				DrtEstimatorModule.bindEstimator(binder(), drtConfigGroup.getMode()).toInstance(
					new DirectTripBasedDrtEstimator.Builder()
						.setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(300))
						.setWaitingTimeDistributionGenerator(new NormalDistributionGenerator(1, 0.4))
						.setRideDurationEstimator(new ConstantRideDurationEstimator(1.25, 300))
						.setRideDurationDistributionGenerator(new NormalDistributionGenerator(2, 0.3))
						.build()
				);
			}
		});
		controler.run();
		Path csvPath = Path.of(utils.getOutputDirectory()).resolve("drt_customer_stats_drt.csv");
		try (CSVParser csv = new CSVParser(IOUtils.getBufferedReader(csvPath.toString()),
			CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setDelimiter(';').build())) {
			CSVRecord row = Iterables.getLast(csv);
			double meanWaitTime = Double.parseDouble(row.get("wait_average"));
			assertThat(meanWaitTime).isCloseTo(300., Offset.offset(30.));
		}

	}

	@Test void testRejectionEstimator() throws IOException {
		double targetRejectionRate = 0.4;
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
			new OTFVisConfigGroup());
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		DrtConfigGroup drtConfigGroup = DrtConfigGroup.getSingleModeDrtConfig(config);
		drtConfigGroup.setSimulationType(DrtConfigGroup.SimulationType.estimateAndTeleport);
		drtConfigGroup.addParameterSet(new DrtEstimatorParams());

		Controler controler = DrtControlerCreator.createControler(config, false);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				DrtEstimatorModule.bindEstimator(binder(), drtConfigGroup.getMode()).toInstance(
					new DirectTripBasedDrtEstimator.Builder()
						.setWaitingTimeEstimator(new ConstantWaitingTimeEstimator(300))
						.setWaitingTimeDistributionGenerator(new NormalDistributionGenerator(1, 0.4))
						.setRideDurationEstimator(new ConstantRideDurationEstimator(1.25, 300))
						.setRideDurationDistributionGenerator(new NormalDistributionGenerator(2, 0.3))
						.setRejectionRateEstimator(new UniformRejectionEstimator(targetRejectionRate))
						.build()
				);
			}
		});
		controler.run();

		Path csvPath = Path.of(utils.getOutputDirectory()).resolve("drt_customer_stats_drt.csv");
		try (CSVParser csv = new CSVParser(IOUtils.getBufferedReader(csvPath.toString()),
			CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setDelimiter(';').build())) {
			CSVRecord row = Iterables.getLast(csv);
			double rejectionRate = Double.parseDouble(row.get("rejectionRate"));
			assertThat(rejectionRate).isCloseTo(targetRejectionRate, Offset.offset(0.05));
		}
	}
}
