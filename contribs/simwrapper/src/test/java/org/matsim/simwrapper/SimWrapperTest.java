package org.matsim.simwrapper;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.matsim.application.prepare.network.CreateAvroNetwork;
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

			layout.row("eleventh")
				.el(CarrierViewer.class, (viz, data) -> {
					viz.title = "carrierViewer";

					// Include a network that has not been filtered
					viz.network = data.withContext("all").compute(CreateAvroNetwork.class, "network.avro",
						"--mode-filter", "", "--shp", "none");

					viz.carriers = data.output("output_carriers.xml.gz");
				});

			layout.row("twelfth")
				.el(LogisticViewer.class, (viz, data) -> {
					viz.title = "logisticViewer";

					// Include a network that has not been filtered
					viz.network = data.withContext("all").compute(CreateAvroNetwork.class, "network.avro",
						"--mode-filter", "", "--shp", "none");

					viz.carriers = data.output("output_carriers.xml.gz");
					viz.lsps = data.output("output_lsps.xml.gz");
				});
			layout.row("thirteenth")
				.el(FlowMap.class, ((viz, data) -> {
					viz.title = "Flow Map";
					viz.description = "Visualize the flows of different metrics";
					FlowMap.Metrics metrics = new FlowMap.Metrics();
					metrics.setZoom(9.5);
					metrics.setLabel("headway metric");
					metrics.setDataset("analysis/pt/pt_headway_per_stop_area_pair_and_hour.csv");
					metrics.setOrigin("stopAreaOrStop");
					metrics.setDestination("stopAreaOrStopNext");
					metrics.setFlow("meanHeadway");
					metrics.setColorScheme("BurgYl");
					metrics.setValueTransform(FlowMap.Metrics.ValueTransform.INVERSE);
					viz.metrics.add(metrics);
				}));
			layout.row("fourteenth")
				.el(Vehicles.class, ((viz, data) -> {
					viz.title = "DRT Vehicle Animation";
					viz.description = "drt animation";
					viz.center = new double[]{13.45, 52.5};
					viz.zoom = 11.0;
					viz.drtTrips = "drt-vehicles.json";
					viz.projection = "EPSG:25832";
					viz.mapIsIndependent = true;
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
