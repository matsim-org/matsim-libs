package org.matsim.simwrapper.dashboard;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsModule;
import org.matsim.counts.CountsWriter;
import org.matsim.examples.ExamplesUtils;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.TestScenario;
import org.matsim.testcases.MatsimTestUtils;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.DataFrameReader;
import tech.tablesaw.io.ReaderRegistry;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.SplittableRandom;

public class TrafficCountsDashboardTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void generate() {

		Config config = TestScenario.loadConfig(utils);

		generateDummyCounts(config);

		SimWrapperConfigGroup simWrapperConfigGroup = new SimWrapperConfigGroup();
		SimWrapperConfigGroup.ContextParams contextParams = simWrapperConfigGroup.get("");
		contextParams.mapCenter = "12, 48.95";
		contextParams.sampleSize = "0.01";
		contextParams.mapZoomLevel = 9.0;

		SimWrapper sw = SimWrapper.create(simWrapperConfigGroup)
				.addDashboard(new TrafficCountsDashboard(List.of(0.0, 0.3, 1.7, 2.5, Double.MAX_VALUE), List.of("less", "exact", "too much", "way too much")))
				.addDashboard(new PlotlyDashboard());

		Controler controler = MATSimApplication.prepare(new TestScenario(sw), config);
		controler.addOverridingModule(new CountsModule());
		controler.run();

		Table total = new DataFrameReader(new ReaderRegistry()).csv(utils.getOutputDirectory() + "analysis/traffic/count_comparison_total.csv");
		Assert.assertFalse(total.isEmpty());

		Table byHour = new DataFrameReader(new ReaderRegistry()).csv(utils.getOutputDirectory() + "analysis/traffic/count_comparison_by_hour.csv");
		Assert.assertFalse(byHour.isEmpty());
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

			if(counts.getCounts().containsKey(link.getId()))
				continue;

			Count<Link> count = counts.createAndAddCount(link.getId(), link.getId().toString() + "_count_station");
			for (int hour = 1; hour < 25; hour++)
				count.createVolume(hour, random.nextInt(75));
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
