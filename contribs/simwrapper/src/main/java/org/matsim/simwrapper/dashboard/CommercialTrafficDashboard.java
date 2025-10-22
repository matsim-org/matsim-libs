package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.commercialTraffic.CommercialAnalysis;
import org.matsim.application.analysis.population.TripAnalysis;
import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.*;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.traces.BarTrace;
import tech.tablesaw.plotly.traces.ScatterTrace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Dashboard to show commercial traffic statistics.
 */
public class CommercialTrafficDashboard implements Dashboard {

	private List<String> groupsOfCommercialSubpopulations = new ArrayList<>();
	private final String crs;
	private String[] args;

	/**
	 * Create a dashboard to show aggregated OD information per mode.
	 *
	 * @param crs   Coordinate system for the projection of the map
	 */
	public CommercialTrafficDashboard(String crs) {
		this.crs = crs;
		args = new String[0];
	}

	/** Set the groups of supopulations for the commercial analysis. So it is possible to exclude person agents from this analysis, and also different subpopulations can be analyzed as one group.
	 * Different groups are separated by ';' and subpopulations within a group by ','.
	 * See {@link CommercialAnalysis}.
	 * @param groupsOfSubpopulations e.g. "commercialGroup1=smallScaleCommercialPersonTraffic,smallScaleGoodsTraffic;longDistanceFreight=freight"
	 */
	public CommercialTrafficDashboard setGroupsOfSubpopulationsForCommercialAnalysis(String... groupsOfSubpopulations) {
		String joined = String.join(";", groupsOfSubpopulations);
		groupsOfCommercialSubpopulations = Arrays.stream(joined.split(";"))
			.map(entry -> entry.split("=")[0])
			.collect(Collectors.toList());
		return setAnalysisArgs("--groups-of-subpopulations-commercialAnalysis", joined);
	}
	/**
	 * Set an argument that will be passed to the analysis script. See {@link CommercialAnalysis}.
	 */
	public CommercialTrafficDashboard setAnalysisArgs(String... args) {
		this.args = this.args == null
			? args
			: Stream.concat(Stream.of(this.args), Stream.of(args)).toArray(String[]::new);
		return this;
	}

	@Override
	public void configure(Header header, Layout layout) {
		header.title = "Commercial Traffic";
		header.description = "Commercial Traffic related analyses.";

		layout.row("General_first","General").el(PieChart.class, (viz, data) -> {
				double sampleSize = data.config().getSampleSize();
				if (data.context().getShp() != null) { //TODO was soll das?
					setAnalysisArgs("--sampleSize", String.valueOf(sampleSize));
				} else {
					setAnalysisArgs("--sampleSize", String.valueOf(sampleSize));
				}
				viz.dataset = data.compute(CommercialAnalysis.class, "commercialTraffic_travelDistancesShares_perMode.csv", args);
				viz.title = "Travel Distance Shares by Mode";
				viz.description = "at final iteration";
				viz.useLastRow = true;
			})
			.el(PieChart.class, (viz, data) -> {
				viz.dataset = data.compute(CommercialAnalysis.class, "commercialTraffic_travelDistancesShares_perSubpopulation.csv", args);
				viz.title = "Travel Distance Shares by subpopulation";
				viz.useLastRow = true;
				viz.description = "at final iteration";

			})
			.el(PieChart.class, (viz, data) -> {
				viz.dataset = data.compute(CommercialAnalysis.class, "commercialTraffic_travelDistancesShares_perType.csv", args);
				viz.title = "Travel Distance Shares by model type";
				viz.useLastRow = true;
				viz.description = "at final iteration";

			});
		layout.row("General_second", "General").el(Links.class, (viz, data) -> {
			viz.title = "Link volumes of the commercial traffic";
			viz.description = "The volumes are scaled to 100% sample size.";
			viz.datasets.csvFile = data.compute(CommercialAnalysis.class, "commercialTraffic_link_volume.csv", args);
			viz.network = data.compute(CreateAvroNetwork.class, "network.avro", "--with-properties"); //, "--match-id", "linkId", "--mode-filter", "none"
			viz.description = "The volumes can be filtered according to different types of traffic and vehicle types.";
			viz.height = 8.;
		});
		layout.row("trips_first", "Trips").el(Plotly.class, (viz, data) -> {
				viz.title = "Modal split by main mode";

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
					.build();

				Plotly.DataSet ds = viz.addDataset(data.computeWithPlaceholder(TripAnalysis.class, "mode_share_%s.csv", "commercialPersonTraffic"))
					.constant("source", "Simulated")
					.aggregate(List.of("main_mode"), "share", Plotly.AggrFunc.SUM);

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).orientation(BarTrace.Orientation.HORIZONTAL).build(),
					ds.mapping()
						.name("main_mode")
						.y("source")
						.x("share")
				);

				Plotly.DataSet ds2 = viz.addDataset(data.computeWithPlaceholder(TripAnalysis.class, "mode_share_%s.csv", "smallScaleGoodsTraffic"))
					.constant("source", "Simulated")
					.aggregate(List.of("main_mode"), "share", Plotly.AggrFunc.SUM);

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).orientation(BarTrace.Orientation.HORIZONTAL).build(),
					ds2.mapping()
						.name("main_mode")
						.y("source")
						.x("share")
				);
			})
			.el(Plotly.class, (viz, data) -> {
				viz.title = "Modal split by subpopulation";

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
					.build();

				Plotly.DataSet ds = viz.addDataset(data.computeWithPlaceholder(TripAnalysis.class, "mode_share_%s.csv", "commercialPersonTraffic"))
					.constant("source", "Simulated")
					.aggregate(List.of("subpopulation"), "share", Plotly.AggrFunc.SUM);

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).orientation(BarTrace.Orientation.HORIZONTAL).build(),
					ds.mapping()
						.name("subpopulation")
						.y("source")
						.x("share")
				);
			});

		layout.row("trips_second", "Trips").el(Plotly.class, (viz, data) -> {

				viz.title = "Trip distance distribution";
				viz.colorRamp = ColorScheme.Viridis;

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("Simulated").build(),
					viz.addDataset(data.computeWithPlaceholder(TripAnalysis.class, "mode_share_%s.csv","commercialPersonTraffic"))
						.aggregate(List.of("dist_group"), "share", Plotly.AggrFunc.SUM)
						.mapping()
						.x("dist_group")
						.y("share")
				);
			})
			.el(Plotly.class, (viz, data) -> {

				viz.title = "Trip distance distribution by mode";
				viz.colorRamp = ColorScheme.Viridis;

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
					.build();

				viz.mergeDatasets = true;
				viz.multiIndex = Map.of("dist_group", "source");

				var ds = viz.addDataset(
						data.computeWithPlaceholder(TripAnalysis.class, "mode_share_%s.csv","commercialPersonTraffic"))
					.aggregate(List.of("dist_group", "main_mode"), "share", Plotly.AggrFunc.SUM)
					.constant("source", "Sim")
					.mapping()
					.x("dist_group")
					.y("share");

				viz.addTrace(
					BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT)
						.orientation(BarTrace.Orientation.VERTICAL)
						.name("$dataset.main_mode")
						.build(), ds
				);
			})
			.el(Plotly.class, (viz, data) -> {

				viz.title = "Trip distance distribution by subpopulation";
				viz.colorRamp = ColorScheme.Viridis;

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK).xAxis(Axis.builder().categoryOrder(Axis.CategoryOrder.TRACE).build())
					.build();

				viz.mergeDatasets = true;
				viz.multiIndex = Map.of("dist_group", "source");

				var ds = viz.addDataset(
						data.computeWithPlaceholder(TripAnalysis.class, "mode_share_%s.csv","commercialPersonTraffic"))
					.aggregate(List.of("dist_group", "subpopulation"), "share", Plotly.AggrFunc.SUM)
					.constant("source", "Sim")
					.mapping()
					.x("dist_group")
					.y("share");

				viz.addTrace(
					BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT)
						.orientation(BarTrace.Orientation.VERTICAL)
						.name("$dataset.subpopulation")
						.build(),
					ds
				);
			});
//		layout.row("trips_third", "Trips").el(Plotly.class, (viz, data) -> {
//
//				viz.title = "Trip distance distribution by mode";
//				viz.description = "Distribution within each distance group.";
//
//
//				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
//					.xAxis(Axis.builder().title("Distance group").build())
//					.yAxis(Axis.builder().title("Share").build())
//					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
//					.build();
//
//				Plotly.DataSet sim = viz.addDataset(data.compute(TripAnalysis.class, "mode_share_per_dist.csv"))
//					.aggregate(List.of("dist_group", "main_mode"), "share", Plotly.AggrFunc.SUM)
//					.constant("source", "Sim");
//
//				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
//					sim.mapping()
//						.name("main_mode")
//						.x("dist_group")
//						.y("share")
//				);
//			})
//			.el(Plotly.class, (viz, data) -> {
//
//				viz.title = "Trip distance distribution by subpopulation";
//				viz.description = "Distribution within each distance group.";
//
//				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
//					.xAxis(Axis.builder().title("Distance group").build())
//					.yAxis(Axis.builder().title("Share").build())
//					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
//					.build();
//
//				Plotly.DataSet sim = viz.addDataset(data.compute(TripAnalysis.class, "mode_share_per_dist.csv"))
//					.aggregate(List.of("dist_group", "subpopulation"), "share", Plotly.AggrFunc.SUM)
//					.constant("source", "Sim");
//
//				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
//					sim.mapping()
//						.name("subpopulation")
//						.x("dist_group")
//						.y("share")
//				);
//			});

			for (String group : groupsOfCommercialSubpopulations) {
				layout.row("trips_fourth", "Trips").el(Table.class, (viz, data) -> {
					viz.title = "Mode Statistics of group: *" + group + "*";
					viz.description = "by main mode, over whole trip (including access & egress); not scaled by sample size";
					viz.dataset = data.computeWithPlaceholder(TripAnalysis.class, "trip_stats_%s.csv", group);
					viz.showAllRows = true;
				});
			}
		layout.row("trips_fifth", "Trips")
			.el(Table.class, (viz, data) -> {
				viz.title = "Population statistics";
				viz.description = "over simulated persons (not scaled by sample size)";
				viz.showAllRows = true;
				viz.dataset = data.compute(TripAnalysis.class, "population_trip_stats.csv");
				List<String> headerPopStats = new ArrayList<>(List.of("Group"));
				headerPopStats.addAll(groupsOfCommercialSubpopulations);
				viz.show = headerPopStats;
			})
			.el(Plotly.class, (viz, data) -> {
//				viz.layout.barmode = "group";
			viz.title = "Mode usage by subpopulation";

			for (String group : groupsOfCommercialSubpopulations) {
				Plotly.DataSet dsSub = viz.addDataset(
					data.computeWithPlaceholder(TripAnalysis.class, "mode_users_%s.csv", group));
				viz.addTrace(
					BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
					dsSub.mapping()
						.x("main_mode")
						.y("user")
						.name("group")
				);
			}
		});
		layout.row("dist-dist", "Trips").el(Plotly.class, (viz, data) -> {

			viz.title = "Detailed distance distribution";
			viz.description = "by mode.";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Distance [m]").build())
				.yAxis(Axis.builder().title("Share").build())
				.showLegend(true)
				.build();

			viz.colorRamp = ColorScheme.Viridis;
			viz.interactive = Plotly.Interactive.dropdown;
			for (String group : groupsOfCommercialSubpopulations) {
				Plotly.DataSet ds = viz.addDataset(data.computeWithPlaceholder(TripAnalysis.class, "mode_share_distance_distribution_%s.csv", group))
					.pivot(List.of("dist"), "main_mode", "share")
					.constant("source", "Sim");

				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
						.mode(ScatterTrace.Mode.LINE)
						.name(group)
						.build(),
					ds.mapping()
						.name("main_mode")
						.x("dist")
						.y("share")
				);
			}
		});

	private static void addNotesBlock(Layout layout, String tab) {
		layout.row(tab+"info", tab).el(TextBlock.class, (viz, data) -> {
			viz.backgroundColor = "transparent";
			viz.content = """
				### Notes
				This dashboard analyzes commercial traffic. The commercial traffic contains traffic from vehicles with a commercial purpose, e.g. freight transport, but also vehicles of the small-scale commercial traffic, e.g. service vehicles, care services, etc.
				The simulation results are with **sample size\s"""
				+data.config().getSampleSize()+"""
				**. If the sample size is < 1.0 the visualized results are **scale up to the 100% level**.
				<br><br>
				""";
		});
	}
}
