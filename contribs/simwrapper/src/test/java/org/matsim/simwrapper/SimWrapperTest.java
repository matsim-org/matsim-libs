package org.matsim.simwrapper;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.application.analysis.traffic.LinkStats;
import org.matsim.simwrapper.viz.PieChart;
import org.matsim.simwrapper.viz.TextBlock;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.nio.file.Path;

public class SimWrapperTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void dashboard() throws IOException {

		SimWrapper simWrapper = SimWrapper.create();

		simWrapper.addDashboard((header, layout) -> {
			header.title = "Test";

			layout.row("first", TextBlock.class, (viz, data) -> {
				viz.title = "Title here";
				viz.description = "Detailed explanation.";

			});

			layout.row("name", PieChart.class, (viz, data) -> {

				viz.title = "Shows ...";
				viz.height = 8.;

				viz.dataset = data.output("link-stats", "trips.csv");

//				viz.dataset = data.compute(LinkStats.class, "link-stats.csv");

			});

		});

		simWrapper.generate(Path.of(utils.getOutputDirectory()));

		// TODO: assert in the test

	}
}
