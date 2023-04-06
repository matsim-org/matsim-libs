package org.matsim.simwrapper;

import org.junit.Rule;
import org.junit.Test;

import org.matsim.simwrapper.dashboard.StuckAgentDashboard;
import org.matsim.simwrapper.viz.*;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.nio.file.Path;

public class SimWrapperTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void dashboard() throws IOException {

		SimWrapper simWrapper = SimWrapper.create();

		simWrapper.addDashboard(new StuckAgentDashboard());

		simWrapper.addDashboard((header, layout) -> {
			header.title = "Area Header";
			header.tab = "Area Tab";
			header.description = "Area Description";

			layout.row("first").el(Area.class, (viz, data) -> {
				viz.title = "Stats";
			});
		});

		simWrapper.addDashboard((header, layout) -> {
			header.title = "Bar Header";
			header.tab = "Bar Tab";
			header.description = "Bar Description";

			layout.row("first").el( Bar.class, (viz, data) -> {
				viz.title = "Stats";
			});
		});

		simWrapper.addDashboard((header, layout) -> {
			header.title = "Bubble Header";
			header.tab = "Bubble Tab";
			header.description = "Bubble Description";

			layout.row("first").el( Bubble.class, (viz, data) -> {
				viz.title = "Stats";
			});
		});

		simWrapper.addDashboard((header, layout) -> {
			header.title = "CalculationTable Header";
			header.tab = "CalculationTable Tab";
			header.description = "CalculationTable Description";

			layout.row("first").el( CalculationTable.class, (viz, data) -> {
				viz.title = "CalculationTable";
			});
		});

		simWrapper.addDashboard((header, layout) -> {
			header.title = "Heatmap Header";
			header.tab = "Heatmap Tab";
			header.description = "Heatmap Description";

			layout.row("first").el( Heatmap.class, (viz, data) -> {
				viz.title = "Heatmap";
			});
		});

		simWrapper.addDashboard((header, layout) -> {
			header.title = "Line Header";
			header.tab = "Line Tab";
			header.description = "Line Description";

			layout.row("first").el( Line.class, (viz, data) -> {
				viz.title = "Line";
			});
		});

		simWrapper.addDashboard((header, layout) -> {
			header.title = "PieChart Header";
			header.tab = "PieChart Tab";
			header.description = "PieChart Description";

			layout.row("first").el( PieChart.class, (viz, data) -> {
				viz.title = "PieChart";
			});
		});

		simWrapper.addDashboard((header, layout) -> {
			header.title = "Scatter Header";
			header.tab = "Scatter Tab";
			header.description = "Scatter Description";

			layout.row("first").el( Scatter.class, (viz, data) -> {
				viz.title = "Scatter";
			});
		});

		simWrapper.addDashboard((header, layout) -> {
			header.title = "TextBlock Header";
			header.tab = "TextBlock Tab";
			header.description = "TextBlock Description";

			layout.row("first").el( TextBlock.class, (viz, data) -> {
				viz.title = "TextBlock";
			});
		});

		simWrapper.generate(Path.of(utils.getOutputDirectory()));

		// TODO: assert in the test

	}
}
