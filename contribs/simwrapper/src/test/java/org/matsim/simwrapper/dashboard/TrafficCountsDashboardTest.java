package org.matsim.simwrapper.dashboard;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.counts.*;
import org.matsim.examples.ExamplesUtils;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.TestScenario;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

public class TrafficCountsDashboardTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void generate() {

		Config config = TestScenario.loadConfig(utils);

		generateDummyCounts(config);

		SimWrapperConfigGroup simWrapperConfigGroup = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		simWrapperConfigGroup.sampleSize = 0.01;

		SimWrapperConfigGroup.ContextParams contextParams = simWrapperConfigGroup.defaultParams();
		contextParams.mapCenter = "12,48.95";
		contextParams.mapZoomLevel = 9.0;

		SimWrapper sw = SimWrapper.create(config)
			.addDashboard(new TrafficCountsDashboard()
				.withModes("car", Set.of(TransportMode.car))
				.withModes("truck", Set.of(TransportMode.truck, "freight"))
			)
			.addDashboard(new TrafficCountsDashboard()
				.withQualityLabels(
					List.of(0.0, 0.3, 1.7, 2.5),
					List.of("way too few", "fewer", "exact", "too much", "way too much")
				)
			);

		Controler controler = MATSimApplication.prepare(new TestScenario(sw), config);
		controler.addOverridingModule(new CountsModule());
		controler.run();

		Path defaultDir = Path.of(utils.getOutputDirectory(), "analysis", "traffic");
		Path carDir = Path.of(utils.getOutputDirectory(), "analysis", "traffic-car");
		Path truckDir = Path.of(utils.getOutputDirectory(), "analysis", "traffic-truck");

		for (Path dir : List.of(defaultDir, carDir, truckDir)) {
			Assertions.assertThat(dir)
				.isDirectoryContaining("glob:**count_comparison_daily.csv")
				.isDirectoryContaining("glob:**count_comparison_by_hour.csv");
		}
	}

	public void generateDummyCounts(Config config) {

		SplittableRandom random = new SplittableRandom(1234);

		URL context = ExamplesUtils.getTestScenarioURL("kelheim");
		Network network = NetworkUtils.readNetwork(context + config.network().getInputFile());

		List<? extends Link> links = List.copyOf(network.getLinks().values());
		int size = links.size();

		Counts<Link> counts = new Counts<>();

		for (int i = 0; i <= 100; i++) {
			Link link = links.get(random.nextInt(size));

			if (counts.getMeasureLocations().containsKey(link.getId()))
				continue;

			MeasurementLocation<Link> station = counts.createAndAddMeasureLocation(link.getId(), link.getId().toString() + "_count_station");

			Measurable carVolume = station.createVolume(TransportMode.car);
			Measurable freightVolume = station.createVolume(TransportMode.truck);

			for (int hour = 0; hour < 24; hour++) {
				carVolume.setAtHour(hour, random.nextInt(500));
				freightVolume.setAtHour(hour, random.nextInt(100));
			}
		}

		try {
			Files.createDirectories(Path.of(utils.getPackageInputDirectory()));
			String absolutPath = Path.of(utils.getPackageInputDirectory()).normalize().toAbsolutePath() + "/dummy_counts.xml";

			config.counts().setInputFile(absolutPath);
			new CountsWriter(counts).write(absolutPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
