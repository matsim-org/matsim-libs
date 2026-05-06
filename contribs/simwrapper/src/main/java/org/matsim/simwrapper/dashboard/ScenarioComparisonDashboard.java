package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.population.TripAnalysis;
import org.matsim.application.analysis.scenarioComparison.ScenarioComparisonAnalysis;
import org.matsim.simwrapper.ComparisonDashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.*;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.components.Line;
import tech.tablesaw.plotly.traces.BarTrace;
import tech.tablesaw.plotly.traces.ScatterTrace;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.IntStream;

public class ScenarioComparisonDashboard implements ComparisonDashboard {

	private Map<String, String> comparisonScenarios;
//	private boolean tripsComparison;

	/**
	 * Default scenario comparison dashboard constructor.
	 */
	// add logic for whcih types of comparison dashboard - right now, it's only trips, so don't need boolean tripsComparison
	public ScenarioComparisonDashboard(Map<String, String> comparisonScenarios) {
//		this.tripsComparison  = tripsComparison;
		this.comparisonScenarios = comparisonScenarios;
	}


	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Scenario Comparison: Policy to Base Case";
		header.description = "Shows the differences in a variety of metrics between the policy and base case.";
//		if (this.tripsComparison) {
		String tab = "Trips Comparison";

		Layout.Row first = layout.row("first", tab);
		first.el(Plotly.class, (viz, data) -> {
				viz.title = "Mode Split";
//				viz.width = 2d;

				for (Map.Entry<String, String> compScenario : this.comparisonScenarios.entrySet()) {
					Path source = Path.of(compScenario.getValue() + "/analysis/population/mode_share.csv");
					Path alias = source.getParent().resolve("mode_share_" + "scenarioComp_" + compScenario.getKey() + ".csv");
					try {
						Files.copy(source, alias, StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name(compScenario.getKey()).build(),
						viz.addDataset(data.resource(alias.toString()))
							.aggregate(List.of("main_mode"), "share", Plotly.AggrFunc.SUM)
							.mapping()
							.x("main_mode")
							.y("share")
					);
				}

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("sim").build(),
				viz.addDataset(data.compute(TripAnalysis.class, "mode_share.csv"))
					.aggregate(List.of("main_mode"), "share", Plotly.AggrFunc.SUM)
					.mapping()
					.x("main_mode")
					.y("share")
			);
		});

		first.el(Plotly.class, (viz, data) -> {

			viz.title = "Trip distance distribution";
			viz.colorRamp = ColorScheme.Viridis;

				for (Map.Entry<String, String> compScenario : this.comparisonScenarios.entrySet())
				{
					Path source = Path.of(compScenario.getValue() + "/analysis/population/mode_share.csv");
					Path alias = source.getParent().resolve("mode_share_" + "scenarioComp_" + compScenario.getKey() + ".csv");
					try {
						Files.copy(source, alias, StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name(compScenario.getKey()).build(),
						viz.addDataset(data.resource(alias.toString())).constant("source", compScenario.getKey())
							.aggregate(List.of("dist_group"), "share", Plotly.AggrFunc.SUM)
							.mapping()
							.x("dist_group")
							.y("share")
					);
				viz.multiIndex = Map.of("dist_group", "source");
			}

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("sim").build(),
				viz.addDataset(data.compute(TripAnalysis.class, "mode_share.csv")).constant("source", "sim")
					.aggregate(List.of("dist_group"), "share", Plotly.AggrFunc.SUM)
					.mapping()
					.x("dist_group")
					.y("share")
			);
		});


			layout.row("second", tab)
			.el(Table.class, (viz, data) -> {
				viz.title = "Mode Statistics";
				viz.description = "by main mode, over whole trip (including access & egress)";
				viz.dataset = data.compute(ScenarioComparisonAnalysis.class, "trip_stats_comparison.csv",
					"--input-comp-paths=" + String.join(",", this.comparisonScenarios.values()),
					"--input-comp-names=" + String.join(",", this.comparisonScenarios.keySet()));
				// we can't find out how many columns are in the final datset, so setting this to high number as a hacky solution.
				viz.alignment = IntStream.range(0, 100)
					.mapToObj(i -> i == 0 ? "left" : "right")
					.toArray(String[]::new);
				viz.showAllRows = true;
			});

				createModeDistanceBarPlot(layout, tab);

		layout.row("third", tab)
			.el(Table.class, (viz, data) -> {
				viz.title = "Population statistics";
				viz.description = "over simulated persons (not scaled by sample size)";
				viz.dataset = data.compute(ScenarioComparisonAnalysis.class, "population_trip_stats_comparison.csv",
					"--input-comp-paths=" + String.join(",", this.comparisonScenarios.values()),
					"--input-comp-names=" + String.join(",", this.comparisonScenarios.keySet()));
				// we can't find out how many columns are in the final datset, so setting this to high number as a hacky solution.
				viz.alignment = IntStream.range(0, 100)
					.mapToObj(i -> i == 0 ? "left" : "right")
					.toArray(String[]::new);
				viz.showAllRows = true;
			})

			.el(Plotly.class, (viz, data) -> {

				viz.title = "Mode usage";
				viz.description = "Share of persons using a main mode at least once per day";
				viz.width = 2d;

				Plotly.DataSet ds = viz.addDataset(data.compute(TripAnalysis.class, "mode_users.csv"));

					for   (Map.Entry<String, String> compScenario : this.comparisonScenarios.entrySet())
					{

						Path source = Path.of(compScenario.getValue() + "/analysis/population/mode_users.csv");
						Path alias = source.getParent().resolve("mode_users_" + "scenarioComp_" + compScenario.getKey() + ".csv");
						try {
							Files.copy(source, alias, StandardCopyOption.REPLACE_EXISTING);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}

						viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name(compScenario.getKey()).build(),
							viz.addDataset(data.resource(alias.toString())).mapping()
							.x("main_mode")
							.y("user")
						);
					}

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("sim").build(), ds.mapping()
					.x("main_mode")
					.y("user")

				);
			});

		createDistancePlot(layout, tab);

//	}
		}

	//	priority is set to a lower number in order to force this class to be executed after population and emissions folders are already generated
	@Override
	public double priority() {
		return -2;
	}

	private void createDistancePlot(Layout layout, String tab) {

		layout.row("dist-dist", tab).el(Plotly.class, (viz, data) -> {

			viz.title = "Detailed distance distribution";
			viz.description = "by mode.";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Distance [m]").build())
				.yAxis(Axis.builder().title("Share").build())
				.build();

			viz.colorRamp = ColorScheme.Viridis;
			viz.interactive = Plotly.Interactive.dropdown;

			Plotly.DataSet ds = viz.addDataset(data.compute(TripAnalysis.class, "mode_share_distance_distribution.csv"))
				.pivot(List.of("dist"), "main_mode", "share")
				.constant("source", "Sim");

				for (Map.Entry<String, String> compScenario : this.comparisonScenarios.entrySet()) {

					Path source = Path.of(compScenario.getValue() + "/analysis/population/mode_share_distance_distribution.csv");
					Path alias = source.getParent().resolve("mode_share_distance_distribution_" + "scenarioComp_" + compScenario.getKey() + ".csv");
					try {
						Files.copy(source, alias, StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}

					Plotly.DataSet ref = viz.addDataset(data.resource(alias.toString()))
						.pivot(List.of("dist"), "main_mode", "share")
						.constant("source", compScenario.getKey());

					viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT).name(compScenario.getKey())
							.mode(ScatterTrace.Mode.LINE)
							.line(tech.tablesaw.plotly.components.Line.builder().dash(Line.Dash.DASH).build())
							.build(),
						ref.mapping().name("main_mode").x("dist").y("share")
					);
				}

			viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT).name("sim")
				.showLegend(true)
				.mode(ScatterTrace.Mode.LINE).build(), ds.mapping().name("main_mode")
				.x("dist")
				.y("share")
			);
		});
	}


	private void createModeDistanceBarPlot(Layout layout, String tab) {

		layout.row("second", tab).el(Plotly.class, (viz, data) -> {

			viz.title = "Distance group";
			viz.description = "by mode.";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Distance Groups [m]").build())
				.yAxis(Axis.builder().title("Share").build())
				.build();

			viz.colorRamp = ColorScheme.Viridis;
			viz.interactive = Plotly.Interactive.dropdown;

			Plotly.DataSet ds = viz.addDataset(data.compute(TripAnalysis.class, "mode_share_per_dist.csv"))
//				.pivot(List.of("dist_group"), "main_mode", "share")
				.constant("source", "Sim");

			for (Map.Entry<String, String> compScenario : this.comparisonScenarios.entrySet()) {

				Path source = Path.of(compScenario.getValue() + "/analysis/population/mode_share_per_dist.csv");
				Path alias = source.getParent().resolve("mode_share_per_dist_" + "scenarioComp_" + compScenario.getKey() + ".csv");
				try {
					Files.copy(source, alias, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				Plotly.DataSet ref = viz.addDataset(data.resource(alias.toString()))
//					.pivot(List.of("dist_group"), "main_mode", "share")
					.constant("source", compScenario.getKey());

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name(compScenario.getKey()).build(),
					ref.mapping().name("main_mode").x("dist_group").y("share")
				);
			}

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("sim")
				.showLegend(true).build(),
				ds.mapping().name("main_mode").x("dist_group").y("share")
			);
		});
	}
}
