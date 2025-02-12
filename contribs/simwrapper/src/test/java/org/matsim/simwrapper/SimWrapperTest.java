package org.matsim.simwrapper;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.matsim.simwrapper.viz.*;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class SimWrapperTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void vizElementsTest() throws IOException {

		SimWrapper simWrapper = SimWrapper.create();

		simWrapper.addDashboard((header, layout) -> {

			header.title = "Simwrapper Test Dashboard";
			header.description = "Test All Simwrapper Plug-Ins Dashboard";
			header.tab = "Header Tab";
			header.triggerPattern = "*example.csv";

			layout.row("first")
					.el(Area.class, (viz, data) -> {
						viz.title = "Area";
						viz.dataset = "example.csv";
						viz.x = "column with x-values";
						viz.columns = List.of("column1", "column2");
					});

			layout.row("second")
					.el(Bubble.class, (viz, data) -> {
						viz.title = "Bubble";
						viz.dataset = "example.csv";
					});

			layout.row("third")
					.el(CalculationTable.class, ((viz, data) -> {
						viz.title = "CalculationTable";
						viz.configFile = "example.csv";
					}));

			layout.row("fourth")
					.el(Heatmap.class, ((viz, data) -> {
						viz.title = "Heatmap";
						viz.dataset = "example.csv";
						viz.y = "column1";
						viz.columns = List.of("column1", "column2");
					}));

			layout.row("fifth")
					.el(Line.class, ((viz, data) -> {
						viz.title = "Line";
						viz.dataset = "example.csv";
						viz.x = "column1";
						viz.columns = List.of("column1", "column2");
					}));

			layout.row("sixth")
					.el(Links.class, ((viz, data) -> {
						viz.title = "Links";
						viz.network = "network_example.xml.gz";
						viz.datasets.csvFile = "example.csv";
						viz.display.width.dataset = "example2.csv";
						viz.display.width.columnName = "column1";
						viz.display.color.fixedColors = "red";
					}));

			layout.row("seventh")
					.el(PieChart.class, ((viz, data) -> {
						viz.title = "PieChart";
						viz.dataset = "example.csv";
					}));

			layout.row("eighth")
					.el(Scatter.class, ((viz, data) -> {
						viz.title = "Scatter";
						viz.dataset = "example.csv";
					}));

			layout.row("nineth")
					.el(Table.class, ((viz, data) -> {
						viz.title = "Table";
						viz.dataset = "example.csv";
					}));

			layout.row("nineth")
					.el(TextBlock.class, ((viz, data) -> {
						viz.title = "TextBlock";
						viz.file = "example.csv";
					}));

			layout.row("nineth")
					.el(Tile.class, ((viz, data) -> {
						viz.title = "Tile";
						viz.dataset = "example.csv";
					}));

			layout.row("tenth")
					.el(Hexagons.class, ((viz, data) -> {
						viz.title = "Hexagon";
						viz.file = "drt_trips_drt.csv.gz";
						viz.projection = "EPSG:31468";
						viz.addAggregation("O/D Summary", "Origins", "fromX", "fromY", "Destinations", "toX", "toY");
					}));
		});

		String outputDirectory = utils.getOutputDirectory();

		simWrapper.generate(Path.of(outputDirectory));

		Assertions.assertThat(new File(outputDirectory, "dashboard-1.yaml"))
				.hasSameTextualContentAs(new File(utils.getPackageInputDirectory(), "dashboard-0.yaml"));

	}

	@Test
	public void subTabs() throws IOException {

		SimWrapper simWrapper = SimWrapper.create();

		simWrapper.addDashboard((header, layout) -> {

			header.title = "Simwrapper Test Dashboard";
			header.fullScreen = true;

			layout.row("first", "Tab #1").el(TextBlock.class, (viz, data) -> {
				viz.title = "TextBlock";
				viz.file = "example.csv";
			});

			layout.row("second").el(TextBlock.class, (viz, data) -> {
				viz.title = "TextBlock";
				viz.file = "example.csv";
			});

			layout.tab("Tab #2", "Zweiter Tab")
				.add("second");

		});

		String outputDirectory = utils.getOutputDirectory();

		simWrapper.generate(Path.of(outputDirectory));

		Assertions.assertThat(new File(outputDirectory, "dashboard-1.yaml"))
			.hasSameTextualContentAs(new File(utils.getPackageInputDirectory(), "dashboard-1.yaml"));

	}
}
