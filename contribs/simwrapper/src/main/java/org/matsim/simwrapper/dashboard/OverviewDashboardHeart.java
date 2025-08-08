package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.accessibility.PrepareHouseholds;
import org.matsim.application.analysis.accessibility.PreparePois;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Data;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Hexagons;
import org.matsim.simwrapper.viz.MapPlot;
import org.matsim.simwrapper.viz.TextBlock;

import java.util.List;

/**
 * Dashboard with general overview.
 */
public class OverviewDashboardHeart implements Dashboard {

	private final List<String> pois;
	private String coordinateSystem;
	private double[] globalCenter;

	public OverviewDashboardHeart(List<String> pois, String coordinateSystem) {
		this.pois = pois;
		this.coordinateSystem = coordinateSystem;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Accessibility Overview";
		header.description = "General Overview of Accessibility Calculations";
		header.tab = "Intro";


		layout.row("info")
			.el(TextBlock.class, (viz, data) -> {
			viz.backgroundColor = "transparent";
			viz.width = 3d;
			viz.content = """
				### Econometric Accessibility
				  Accessibility quantifies how easily people can access opportunities; accessibility depends on (1) land-use (e.g. distribution of opportunities), (2) transport system, (3) individual (sociodemographic) characteristics and (4) temporal characteristics.
				  The Econometric accessibility of a measuring point $l$ to a set of opportunities K is defined as
				   $$A_l := ln \\sum_ke^{V_{lk}}.$$
				   We do this calculation separately for each mode of transport (see tabs on left sidebar). For each mode, we further show accessibility to different opportunity types (see subtabs).
				""";
		}).el(TextBlock.class, (viz, data) -> {
				viz.backgroundColor = "transparent";
				viz.width = 1d;
				viz.content = """
					### Corresponding Author<br>
					Jakob Rehmann<br>
					Research Associate<br>
					rehmann@vsp.tu-berlin.de<br>

					Technische Universität Berlin<br>
					Transportation System Planning and Telematics<br>
					http://tu.berlin/vsp<br>
					""";
			});;

//		 TAB 0: Population Density and POIs
		{
			layout.row("population")
				.el(Hexagons.class, ((viz, data) -> {
					this.globalCenter = data.context().getCenter();
					generateHouseholds(viz, data);
				}))
				.el(MapPlot.class, ((viz, data) -> generatePois(viz, data, pois.get(0))));


			for (int i = 1; i < pois.size(); i++) {
				String poi = pois.get(i);
				layout.row("pois")
					.el(MapPlot.class, ((viz, data) -> generatePois(viz, data, poi)));
			}

		}
//
//		layout.row("first").el(Table.class, (viz, data) -> {
//			viz.title = "Run Info";
//			viz.showAllRows = true;
//			viz.dataset = data.compute(LogFileAnalysis.class, "run_info.csv");
//			viz.width = 1d;
//		}).el(MapPlot.class, (viz, data) -> {
//
//			viz.title = "Simulated traffic volume";
//			viz.center = data.context().getCenter();
//			viz.zoom = data.context().mapZoomLevel;
//			viz.height = 7.5;
//			viz.width = 2.0;
//
//			viz.setShape(data.compute(CreateAvroNetwork.class, "network.avro", "--with-properties"), "linkId");
//			viz.addDataset("traffic", data.compute(TrafficAnalysis.class, "traffic_stats_by_link_daily.csv"));
//
//			viz.display.lineColor.dataset = "traffic";
//			viz.display.lineColor.columnName = "simulated_traffic_volume";
//			viz.display.lineColor.join = "link_id";
//			viz.display.lineColor.setColorRamp(ColorScheme.RdYlBu, 5, true);
//
//			viz.display.lineWidth.dataset = "traffic";
//			viz.display.lineWidth.columnName = "simulated_traffic_volume";
//			viz.display.lineWidth.scaleFactor = 20000d;
//			viz.display.lineWidth.join = "link_id";
//
//
//		});
//
//		// Info about the status of the run
//		layout.row("warnings").el(TextBlock.class, (viz, data) -> {
//			viz.file = data.compute(LogFileAnalysis.class, "status.md");
//		});
//
//		layout.row("config").el(XML.class, (viz, data) -> {
//			viz.file = data.output("(*.)?output_config.xml");
//			viz.height = 6d;
//			viz.width = 2d;
//			viz.unfoldLevel = 1;
//
//		}).el(PieChart.class, (viz, data) -> {
//			viz.title = "Mode Share";
//			viz.description = "at final Iteration";
//			viz.dataset = data.output("(*.)?modestats.csv");
//			viz.ignoreColumns = List.of("iteration");
//			viz.useLastRow = true;
//		});
//
//
//		layout.row("second").el(Line.class, (viz, data) -> {
//
//			viz.title = "Score";
//			viz.dataset = data.output("(*.)?scorestats.csv");
//			viz.description = "per Iteration";
//			viz.x = "iteration";
//			viz.columns = List.of("avg_executed", "avg_worst", "avg_best");
//			viz.xAxisName = "Iteration";
//			viz.yAxisName = "Score";
//
//		});
//
//		layout.row("third")
//			.el(Area.class, (viz, data) -> {
//				viz.title = "Mode Share Progression";
//				viz.description = "per Iteration";
//				viz.dataset = data.output("(*.)?modestats.csv");
//				viz.x = "iteration";
//				viz.xAxisName = "Iteration";
//				viz.yAxisName = "Share";
//				viz.width = 2d;
//			});
//
//		layout.row("perf").el(Bar.class, (viz, data) -> {
//			viz.title = "Runtime";
//			viz.x = "Iteration";
//			viz.xAxisName = "Iteration";
//			viz.yAxisName = "Runtime [s]";
//			viz.columns = List.of("seconds");
//			viz.dataset = data.compute(LogFileAnalysis.class, "runtime_stats.csv");
//
//		}).el(Plotly.class, (viz, data) -> {
//			viz.title = "Memory Usage";
//
//			viz.layout = tech.tablesaw.plotly.components.Layout.builder().xAxis(Axis.builder().title("Time").build()).yAxis(Axis.builder().title("MB").build()).barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK).build();
//
//			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(), viz.addDataset(data.compute(LogFileAnalysis.class, "memory_stats.csv")).pivot(List.of("time"), "names", "values").mapping().name("names").x("time").y("values"));
//		});
	}

		private void generatePois(MapPlot viz, Data data, String poi) {
		viz.title = "POIs: " + poi;
		viz.description = "Shows points of interest of type " + poi;
		viz.setShape(data.computeWithPlaceholder(PreparePois.class, "%s/pois.shp", poi));
		viz.display.fill.fixedColors = new String[]{"#f28e2c"};
//		viz.height = height;
		viz.center = globalCenter;
//		viz.zoom = globalZoom;
	}

	private void generateHouseholds(Hexagons viz, Data data) {
		viz.title = "Households";
		viz.description = "Shows households";
		viz.projection = this.coordinateSystem;
		viz.file = data.compute(PrepareHouseholds.class, "persons.csv");
		viz.addAggregation("Home", "Origins", "home_x", "home_y");
//		viz.height = height;
		viz.center = globalCenter;
//		viz.zoom = globalZoom;
	}

	@Override
	public double priority() {
		return 1;
	}
}
