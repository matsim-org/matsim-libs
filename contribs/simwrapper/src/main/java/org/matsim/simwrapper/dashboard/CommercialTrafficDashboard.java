package org.matsim.simwrapper.dashboard;

import org.jspecify.annotations.NonNull;
import org.matsim.application.analysis.commercialTraffic.CommercialAnalysis;
import org.matsim.application.analysis.population.TripAnalysis;
import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.*;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.components.Marker;
import tech.tablesaw.plotly.traces.BarTrace;
import tech.tablesaw.plotly.traces.HistogramTrace;
import tech.tablesaw.plotly.traces.ScatterTrace;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Dashboard to show commercial traffic statistics.
 */
public class CommercialTrafficDashboard implements Dashboard {

	private final LinkedHashMap<String, List<String>> groupsOfCommercialSubpopulations = new LinkedHashMap<>();
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

	/** Set the groups of subpopulations for the commercial analysis. So it is possible to exclude person agents from this analysis, and also different subpopulations can be analyzed as one group.
	 * Different groups are separated by ';' and subpopulations within a group by ','. The delimiter between group name and subpopulations is '='.
	 * See {@link CommercialAnalysis}.
	 * @param groupsOfSubpopulations e.g. "commercialGroup1=smallScaleCommercialPersonTraffic,smallScaleGoodsTraffic;longDistanceFreight=freight"
	 */
	public CommercialTrafficDashboard setGroupsOfSubpopulationsForCommercialAnalysis(String... groupsOfSubpopulations) {
		String groupsOfCommercialSubpopulationsString = String.join(";", groupsOfSubpopulations);
		for (String part : groupsOfCommercialSubpopulationsString.split(";")) {
			if (part.isBlank()) continue;
			String[] kv = part.split("=", 2);
			String groupName = kv[0].trim();
			List<String> subpopulations = kv.length > 1 && !kv[1].isBlank()
				? Arrays.stream(kv[1].split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList())
				: new ArrayList<>();
			groupsOfCommercialSubpopulations.put(groupName, subpopulations);
		}
		return setAnalysisArgs("--groups-of-subpopulations-commercialAnalysis", groupsOfCommercialSubpopulationsString);
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
		header.description = getDescription();

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
				viz.dataset = data.compute(CommercialAnalysis.class, "commercialTraffic_travelDistancesShares_perGroup.csv", args);
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
			viz.height = 12.;
		});
		layout.row("trips_first", "Trips").el(Plotly.class, (viz, data) -> {
				viz.title = "Modal split by main mode";

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
					.build();
				Plotly.DataSet ds = viz.addDataset(
						data.computeWithPlaceholder(TripAnalysis.class, "mode_share_%s.csv", "total"))
					.constant("source", "Simulated")
					.aggregate(List.of("main_mode"), "share_commercialTraffic", Plotly.AggrFunc.SUM);

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).orientation(BarTrace.Orientation.HORIZONTAL).build(),
					ds.mapping()
						.name("main_mode")
						.y("source")
						.x("share_commercialTraffic")
				);
			})
			.el(Plotly.class, (viz, data) -> {
				viz.title = "Modal split by subpopulation";

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
					.build();
					Plotly.DataSet ds = viz.addDataset(
							data.computeWithPlaceholder(TripAnalysis.class, "mode_share_%s.csv", "total"))
						.constant("source", "Simulated")
						.aggregate(List.of("subpopulation"), "share_commercialTraffic", Plotly.AggrFunc.SUM);

					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).orientation(BarTrace.Orientation.HORIZONTAL).build(),
						ds.mapping()
							.name("subpopulation")
							.y("source")
							.x("share_commercialTraffic")
					);
			});

		layout.row("trips_second", "Trips").el(Plotly.class, (viz, data) -> {

				viz.title = "Trip distance distribution";
				viz.colorRamp = ColorScheme.Viridis;
				for (String group : groupsOfCommercialSubpopulations.keySet()) {
					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name(group).build(),
						viz.addDataset(data.computeWithPlaceholder(TripAnalysis.class, "mode_share_%s.csv", group))
							.aggregate(List.of("dist_group"), "share_"+group, Plotly.AggrFunc.SUM)
							.mapping()
							.x("dist_group")
							.y("share_"+group)
					);
				}
			})
			.el(Plotly.class, (viz, data) -> {

				viz.title = "Trip distance distribution by mode";
				viz.colorRamp = ColorScheme.Viridis;

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
					.build();

				viz.multiIndex = Map.of("dist_group", "source");
					var ds = viz.addDataset(
							data.computeWithPlaceholder(TripAnalysis.class, "mode_share_%s.csv", "total"))
						.aggregate(List.of("dist_group", "main_mode"), "share_commercialTraffic", Plotly.AggrFunc.SUM)
						.constant("source", "Sim")
						.mapping()
						.x("dist_group")
						.y("share_commercialTraffic");

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

				viz.multiIndex = Map.of("dist_group", "source");
					var ds = viz.addDataset(
							data.computeWithPlaceholder(TripAnalysis.class, "mode_share_%s.csv", "total"))
						.aggregate(List.of("dist_group", "subpopulation"), "share_commercialTraffic", Plotly.AggrFunc.SUM)
						.constant("source", "Sim")
						.mapping()
						.x("dist_group")
						.y("share_commercialTraffic");

					viz.addTrace(
						BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT)
							.orientation(BarTrace.Orientation.VERTICAL)
							.name("$dataset.subpopulation")
							.build(),
						ds
					);
			});

		layout.row("trips_fourth", "Trips").el(Table.class, (viz, data) -> {
			viz.title = "Mode Statistics of group: *commercialTraffic*";
			viz.description = "by main mode, over whole trip (including access & egress); not scaled by sample size";
			viz.dataset = data.computeWithPlaceholder(TripAnalysis.class, "trip_stats_%s.csv", "commercialTraffic");
			viz.showAllRows = true;
		});
		for (String group : groupsOfCommercialSubpopulations.keySet()) {
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
				headerPopStats.addAll(groupsOfCommercialSubpopulations.keySet());
				viz.show = headerPopStats;
			})
			.el(Plotly.class, (viz, data) -> {
//				viz.layout.barmode = "group";
			viz.title = "Mode usage by subpopulation";

			for (String group : groupsOfCommercialSubpopulations.keySet()) {
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

			viz.title = "Detailed mode share distance distribution";
			viz.description = "by mode.";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Distance [m]").build())
				.yAxis(Axis.builder().title("Share").build())
				.showLegend(true)
				.build();

			viz.colorRamp = ColorScheme.Viridis;
			viz.interactive = Plotly.Interactive.dropdown;
			for (String group : groupsOfCommercialSubpopulations.keySet()) {
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
		layout.row("distances", "Tours").el(TextBlock.class, (viz, data) -> {
			viz.backgroundColor = "transparent";
			viz.content = """
				### **Distance Analysis of the tours**
				""";
		});
		layout.row("veh-dist-hist", "Tours").el(Plotly.class, (viz, data) -> {

			viz.title = "Distance (km) per vehicle";
			viz.description = "Histogram of distances per vehicle tour by group of subpopulation.";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Distance [km]").build())
				.yAxis(Axis.builder().title("Count").build())
				.showLegend(true)
				.build();

			viz.colorRamp = ColorScheme.Viridis;

			Plotly.DataSet ds = viz.addDataset(
					data.compute(CommercialAnalysis.class, "commercialTraffic_tourAnalysis.csv"))

				.constant("source", "Veh");
			for (String group : groupsOfCommercialSubpopulations.keySet()) {
				viz.addTrace(
					tech.tablesaw.plotly.traces.HistogramTrace.builder(Plotly.INPUT).histNorm(HistogramTrace.HistNorm.PROBABILITY)
						.name(group)
						.build(),
					ds.mapping()
						.x("distanceInKm_" + group)
				);
			}
		});

		layout.row("veh-dist-box", "Tours").el(Plotly.class, (viz, data) -> {

			viz.title = "Distance (km) per vehicle type II";
			viz.description = "Boxplots per vehicleType, split by groups of subpopulation.";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Vehicle type").build())
				.yAxis(Axis.builder().title("Distance [km]").build())
				.showLegend(true)
				.build();

			viz.colorRamp = ColorScheme.Viridis;

			Plotly.DataSet ds = viz.addDataset(
					data.compute(CommercialAnalysis.class, "commercialTraffic_tourAnalysis.csv"))
				.constant("source", "Veh");

			viz.addTrace(
				tech.tablesaw.plotly.traces.BoxTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.build(),
				ds.mapping()
					.name("groupOfSubpopulation")
					.x("vehicleType")
					.y("distanceInKm")
			);
		});

		for (String group : groupsOfCommercialSubpopulations.keySet()) {
			layout.row("veh-dist-violin", "Tours").	el(Plotly.class, (viz, data) -> {
			viz.title = "Distance (km) per vehicle type in group: *" + group + "*";
			viz.description = "Violin blot per vehicleType, split by groups of subpopulation.";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Vehicle type").build())
				.yAxis(Axis.builder().title("Distance [km]").build())
				.showLegend(false)
				.build();

			viz.colorRamp = ColorScheme.Viridis;

			Plotly.DataSet ds = viz.addDataset(
					data.compute(CommercialAnalysis.class, "commercialTraffic_tourAnalysis.csv"))
				.constant("source", "Veh");

				viz.addTrace(
					tech.tablesaw.plotly.traces.ViolinTrace.builder(Plotly.INPUT, Plotly.INPUT)
						.build(),
					ds.mapping()
						.x("vehicleType")
						.y("distanceInKm_"+group)
				);
				viz.addTrace(
					tech.tablesaw.plotly.traces.ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
						.mode(ScatterTrace.Mode.MARKERS)
						.marker(Marker.builder().size(6).opacity(0.4).color("blue").build())
						.build(),
					ds.mapping()
						.name("vehicleId")      // <<< erzeugt pro vehicleId eine eigene (Ein-Punkt-)Serie
						.x("vehicleType")
						.y("distanceInKm_"+group)
				);
			});
		}
		layout.row("durations", "Tours").el(TextBlock.class, (viz, data) -> {
			viz.backgroundColor = "transparent";
			viz.content = """
				### **Duration Analysis of the tours**
				""";
		});
		layout.row("veh-duration-hist", "Tours").el(Plotly.class, (viz, data) -> {

			viz.title = "Duration (h) per vehicle";
			viz.description = "Histogram of distances per vehicle tour by group of subpopulation.";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Duration [h]").build())
				.yAxis(Axis.builder().title("Count").build())
				.showLegend(true)
				.build();

			viz.colorRamp = ColorScheme.Viridis;

			Plotly.DataSet ds = viz.addDataset(
					data.compute(CommercialAnalysis.class, "commercialTraffic_tourAnalysis.csv"))

				.constant("source", "Veh");
			for (String group : groupsOfCommercialSubpopulations.keySet()) {
				viz.addTrace(
					tech.tablesaw.plotly.traces.HistogramTrace.builder(Plotly.INPUT).histNorm(HistogramTrace.HistNorm.PROBABILITY)
						.name(group)
						.build(),
					ds.mapping()
						.x("tourDurationsInHours_" + group)
				);
			}
		});

		layout.row("veh-duration-box", "Tours").el(Plotly.class, (viz, data) -> {

			viz.title = "Duration (h) per vehicle type";
			viz.description = "Boxplots per vehicleType, split by groups of subpopulation.";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Vehicle type").build())
				.yAxis(Axis.builder().title("Duration [h]").build())
				.showLegend(true)
				.build();

			viz.colorRamp = ColorScheme.Viridis;

			Plotly.DataSet ds = viz.addDataset(
					data.compute(CommercialAnalysis.class, "commercialTraffic_tourAnalysis.csv"))
				.constant("source", "Veh");

			viz.addTrace(
				tech.tablesaw.plotly.traces.BoxTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.build(),
				ds.mapping()
					.name("groupOfSubpopulation")
					.x("vehicleType")
					.y("tourDurationsInHours")
			);
		});

		for (String group : groupsOfCommercialSubpopulations.keySet()) {
			layout.row("veh-duration-violin", "Tours").	el(Plotly.class, (viz, data) -> {
				viz.title = "Duration (h) per vehicle type *" + group + "*";
				viz.description = "Violin blot per vehicleType, split by groups of subpopulation.";
				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.xAxis(Axis.builder().title("Vehicle type").build())
					.yAxis(Axis.builder().title("Duration [h]").build())
					.showLegend(false)
					.build();

				viz.colorRamp = ColorScheme.Viridis;

				Plotly.DataSet ds = viz.addDataset(
						data.compute(CommercialAnalysis.class, "commercialTraffic_tourAnalysis.csv"))
					.constant("source", "Veh");

				viz.addTrace(
					tech.tablesaw.plotly.traces.ViolinTrace.builder(Plotly.INPUT, Plotly.INPUT)
						.build(),
					ds.mapping()
						.x("vehicleType")
						.y("tourDurationsInHours_"+group)
				);
				viz.addTrace(
					tech.tablesaw.plotly.traces.ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
						.mode(ScatterTrace.Mode.MARKERS)
						.marker(Marker.builder().size(6).opacity(0.4).color("blue").build())
						.build(),
					ds.mapping()
						.name("vehicleId")      // <<< erzeugt pro vehicleId eine eigene (Ein-Punkt-)Serie
						.x("vehicleType")
						.y("tourDurationsInHours_"+group)
				);
			});
		}

		layout.row("OD_first", "Activities").el(Hexagons.class, (viz, data) -> {
			viz.title = "Origin-Destination of commercial trips";
			viz.description = "The OD can be filtered according to defined groups of commercial subpopulations.";
			viz.file = data.compute(CommercialAnalysis.class, "commercialTraffic_relations.csv", args);
			for (String group : groupsOfCommercialSubpopulations.keySet()) {
				viz.addAggregation(group, "Origin",group+"_start_X",group+"_start_Y","Destination",group+"_act_X",group+"_act_Y");

			}
			viz.projection = crs;
			viz.center = data.context().getCenter();
			viz.zoom = data.context().getMapZoomLevel();
			viz.height = 15.;
			viz.radius = 1000.;
		});
		layout.row("Activities", "Activities").el(TextBlock.class, (viz, data) -> {
			viz.backgroundColor = "transparent";
			viz.content = """
				### **Number of Jobs Analysis**
				""";
		});
		layout.row("veh-Activities-hist", "Activities").el(Plotly.class, (viz, data) -> {

			viz.title = "Number of Jobs per vehicle (h)";
			viz.description = "Histogram of distances per vehicle tour by group of subpopulation.";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Duration [h]").build())
				.yAxis(Axis.builder().title("Count").build())
				.showLegend(true)
				.build();

			viz.colorRamp = ColorScheme.Viridis;

			Plotly.DataSet ds = viz.addDataset(
					data.compute(CommercialAnalysis.class, "commercialTraffic_tourAnalysis.csv"))

				.constant("source", "Veh");
			for (String group : groupsOfCommercialSubpopulations.keySet()) {
				viz.addTrace(
					tech.tablesaw.plotly.traces.HistogramTrace.builder(Plotly.INPUT).histNorm(HistogramTrace.HistNorm.PROBABILITY)
						.name(group)
						.build(),
					ds.mapping()
						.x("jobsPerTour_" + group)
				);
			}
		});

		layout.row("veh-Activities-box", "Activities").el(Plotly.class, (viz, data) -> {

			viz.title = "Number of Jobs  per vehicle type (h)";
			viz.description = "Boxplots per vehicleType, split by groups of subpopulation.";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Vehicle type").build())
				.yAxis(Axis.builder().title("Duration [h]").build())
				.showLegend(true)
				.build();

			viz.colorRamp = ColorScheme.Viridis;

			Plotly.DataSet ds = viz.addDataset(
					data.compute(CommercialAnalysis.class, "commercialTraffic_tourAnalysis.csv"))
				.constant("source", "Veh");

			viz.addTrace(
				tech.tablesaw.plotly.traces.BoxTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.build(),
				ds.mapping()
					.name("groupOfSubpopulation")
					.x("vehicleType")
					.y("jobsPerTour")
			);
		});

		for (String group : groupsOfCommercialSubpopulations.keySet()) {
			layout.row("veh-Activities-violin", "Activities").	el(Plotly.class, (viz, data) -> {
				viz.title = "Number of Jobs per vehicle type (h) Violin *" + group + "*";
				viz.description = "Violin blot per vehicleType, split by groups of subpopulation.";
				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.xAxis(Axis.builder().title("Vehicle type").build())
					.yAxis(Axis.builder().title("Duration [h]").build())
					.showLegend(false)
					.build();

				viz.colorRamp = ColorScheme.Viridis;

				Plotly.DataSet ds = viz.addDataset(
						data.compute(CommercialAnalysis.class, "commercialTraffic_tourAnalysis.csv"))
					.constant("source", "Veh");

				viz.addTrace(
					tech.tablesaw.plotly.traces.ViolinTrace.builder(Plotly.INPUT, Plotly.INPUT)
						.build(),
					ds.mapping()
						.x("vehicleType")
						.y("jobsPerTour_"+group)
				);
				viz.addTrace(
					tech.tablesaw.plotly.traces.ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
						.mode(ScatterTrace.Mode.MARKERS)
						.marker(Marker.builder().size(6).opacity(0.4).color("blue").build())
						.build(),
					ds.mapping()
						.name("vehicleId")      // <<< erzeugt pro vehicleId eine eigene (Ein-Punkt-)Serie
						.x("vehicleType")
						.y("jobsPerTour_"+group)
				);
			});
		}

		layout.row("ActivityDurations", "Activities").el(TextBlock.class, (viz, data) -> {
			viz.backgroundColor = "transparent";
			viz.content = """
				### **Activity Duration Analysis of the tours**
				""";
		});
		layout.row("veh-ActivityDurations-hist", "Activities").el(Plotly.class, (viz, data) -> {

			viz.title = "ActivityDurations per vehicle (km)";
			viz.description = "Histogram of distances per vehicle tour by group of subpopulation.";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Distance [km]").build())
				.yAxis(Axis.builder().title("Count").build())
				.showLegend(true)
				.build();

			viz.colorRamp = ColorScheme.Viridis;

			Plotly.DataSet ds = viz.addDataset(
					data.compute(CommercialAnalysis.class, "commercialTraffic_activities.csv"))

				.constant("source", "Act");
			for (String group : groupsOfCommercialSubpopulations.keySet()) {
				viz.addTrace(
					tech.tablesaw.plotly.traces.HistogramTrace.builder(Plotly.INPUT).histNorm(HistogramTrace.HistNorm.PROBABILITY)
						.name(group)
						.build(),
					ds.mapping()
						.x("activityDurationInMinutes_" + group)
				);
			}
		});

		layout.row("veh-ActivityDurations-box", "Activities").el(Plotly.class, (viz, data) -> {

			viz.title = "ActivityDurations per activityType (km)";
			viz.description = "Boxplots per activityType, split by groups of subpopulation.";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("activityType").build())
				.yAxis(Axis.builder().title("Distance [km]").build())
				.showLegend(true)
				.build();

			viz.colorRamp = ColorScheme.Viridis;

			Plotly.DataSet ds = viz.addDataset(
					data.compute(CommercialAnalysis.class, "commercialTraffic_activities.csv"))
				.constant("source", "Act");

			viz.addTrace(
				tech.tablesaw.plotly.traces.BoxTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.build(),
				ds.mapping()
					.name("groupOfSubpopulation")
					.x("activityType")
					.y("activityDurationInMinutes")
			);
		});

		for (String group : groupsOfCommercialSubpopulations.keySet()) {
			layout.row("veh-ActivityDurations-violin", "Activities").	el(Plotly.class, (viz, data) -> {
				viz.title = "ActivityDurations per vehicle type (km) Violin *" + group + "*";
				viz.description = "Violin blot per vehicleType, split by groups of subpopulation.";
				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.xAxis(Axis.builder().title("activityType").build())
					.yAxis(Axis.builder().title("Distance [km]").build())
					.showLegend(false)
					.build();

				viz.colorRamp = ColorScheme.Viridis;

				Plotly.DataSet ds = viz.addDataset(
						data.compute(CommercialAnalysis.class, "commercialTraffic_activities.csv"))
					.constant("source", "Act");

				viz.addTrace(
					tech.tablesaw.plotly.traces.ViolinTrace.builder(Plotly.INPUT, Plotly.INPUT)
						.build(),
					ds.mapping()
						.x("activityType")
						.y("activityDurationInMinutes_"+group)
				);
				viz.addTrace(
					tech.tablesaw.plotly.traces.ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
						.mode(ScatterTrace.Mode.MARKERS)
						.marker(Marker.builder().size(6).opacity(0.2).color("red").sizeMode(Marker.SizeMode.DIAMETER).build())
						.build(),
					ds.mapping()
						.name("activityId")      // <<< erzeugt pro vehicleId eine eigene (Ein-Punkt-)Serie
						.x("activityType")
						.y("activityDurationInMinutes_"+group)
				);
			});
		}
	}

	private @NonNull String getDescription() {
		if (groupsOfCommercialSubpopulations.isEmpty()) {
			return "No groups of commercial subpopulations have been defined for the analysis. " + "Please define at least one group of commercial subpopulations to see commercial traffic statistics.";
		}

		boolean allGroupsHaveSizeOne = groupsOfCommercialSubpopulations.values().stream().allMatch(v -> v.size() == 1);

		if (allGroupsHaveSizeOne) {
			String groups = String.join(", ", groupsOfCommercialSubpopulations.keySet());

			return "General information about modal share and trip distributions of the selected subpopulations of the commercial agents: **" + groups + "**.";
		}

		String groupsWithSubpops = groupsOfCommercialSubpopulations.entrySet().stream().map(
			e -> e.getKey() + " (" + String.join(", ", e.getValue()) + ")").collect(Collectors.joining("; "));

		return "General information about modal share and trip distributions of the selected groups and related subpopulations of the persons: **" + groupsWithSubpops + "**.";
	}
}
