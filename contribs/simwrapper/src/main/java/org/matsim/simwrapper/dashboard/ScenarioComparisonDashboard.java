package org.matsim.simwrapper.dashboard;

import jakarta.annotation.Nullable;
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

import java.util.List;
import java.util.Map;

public class ScenarioComparisonDashboard implements ComparisonDashboard {

	private String[] comparisonPaths;

	private boolean tripsComparison;

	/**
	 * Default scenario comparison dashboard constructor.
	 */
	public ScenarioComparisonDashboard(String[] scenarioPaths,  boolean tripsComparison) {
		this.tripsComparison  = tripsComparison;
		comparisonPaths = scenarioPaths;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Scenario Comparison: Policy to Base Case";
		header.description = "Shows the differences in a variety of metrics between the policy and base case.";

		String tab = "Trips Comparison";

		Layout.Row first = layout.row("first", tab);
		first.el(Plotly.class, (viz, data) -> {
			viz.title = "Modal split";

				if (!this.tripsComparison) {
					viz.layout = tech.tablesaw.plotly.components.Layout.builder()
						.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
						.build();

					Plotly.DataSet ds = viz.addDataset(data.compute(TripAnalysis.class, "mode_share.csv"))
						.constant("source", "Simulated")
						.aggregate(List.of("main_mode"), "share", Plotly.AggrFunc.SUM);

					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).orientation(BarTrace.Orientation.HORIZONTAL).build(),
						ds.mapping()
							.name("main_mode")
							.y("source")
							.x("share")
					);
				}

			if (this.tripsComparison) {
				viz.title = "Mode Split";
				viz.width = 2d;

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("sim").build(),
					viz.addDataset(data.compute(TripAnalysis.class, "mode_share.csv"))
						.aggregate(List.of("main_mode"), "share", Plotly.AggrFunc.SUM)
						.mapping()
						.x("main_mode")
						.y("share")
				);

				int i = 0;
				for (String path : this.comparisonPaths) {
					String modeShareTripCsv = path + "/analysis/population/mode_share.csv";

					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("Base_" + i).build(),
						viz.addDataset(data.resource(modeShareTripCsv))
							.aggregate(List.of("main_mode"), "share", Plotly.AggrFunc.SUM)
							.mapping()
							.x("main_mode")
							.y("share")
					);
					i++;
				}
			}

		});

		first.el(Plotly.class, (viz, data) -> {

			viz.title = "Trip distance distribution";
			viz.colorRamp = ColorScheme.Viridis;

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("Simulated").build(),
				viz.addDataset(data.compute(TripAnalysis.class, "mode_share.csv")).constant("source", "sim")
					.aggregate(List.of("dist_group"), "share", Plotly.AggrFunc.SUM)
					.mapping()
					.x("dist_group")
					.y("share")
			);

			if (this.tripsComparison) {
				int i = 0;
				for (String path : this.comparisonPaths)
				{
					String modeShareTripCsv =  path + "/analysis/population/mode_share.csv";

					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("Base_" + i).build(),
						viz.addDataset(data.resource(modeShareTripCsv)).constant("source", "Base_" + i)
							.aggregate(List.of("dist_group"), "share", Plotly.AggrFunc.SUM)
							.mapping()
							.x("dist_group")
							.y("share")
					);
					i++;
				}
				viz.multiIndex = Map.of("dist_group", "source");

			}
		});

		layout.row("second", tab)
//			.el(Table.class, (viz, data) -> {
//				viz.title = "Mode Statistics";
//				viz.description = "by main mode, over whole trip (including access & egress)";
//				viz.dataset = data.compute(ScenarioComparisonAnalysis.class, "trip_stats_comparison.csv",
//					"--input-base-path=" + this.comparisonPaths);
//				viz.showAllRows = true;
//			})
			.el(Plotly.class, (viz, data) -> {

				viz.title = "Modal distance distribution";

//				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
//					.xAxis(Axis.builder().title("Distance group").build())
//					.yAxis(Axis.builder().title("Share").build())
//					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
//					.build();

				Plotly.DataSet ds = viz.addDataset(data.compute(TripAnalysis.class, "mode_share_per_dist.csv")).constant("dist_group_source", "Sim");

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("Sim").build(),
					ds.mapping()

						.name("main_mode", "Viridis")
						.x("dist_group")
						.y("share")
				);

				if (tripsComparison) {
					int i = 0;
					for (String path : this.comparisonPaths) {
						String modeShareDistTripCsv = path + "/analysis/population/mode_share_per_dist.csv";

						viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("Base_" + i).showLegend(false).build(),
							viz.addDataset(data.resource(modeShareDistTripCsv)).constant("dist_group_source", "Base_"+i)
								.mapping()
								.name("main_mode", "Viridis")
								.x("dist_group")
								.y("share")
						);
						i++;
					}
				}
//				viz.multiIndex = Map.of("dist_group", "source");

			});

		layout.row("third", tab)
//			.el(Table.class, (viz, data) -> {
//				viz.title = "Population statistics";
//				viz.description = "over simulated persons (not scaled by sample size)";
//				viz.showAllRows = true;
//				viz.dataset = data.compute(TripAnalysis.class, "population_trip_stats.csv");
//				viz.dataset = data.compute(ScenarioComparisonAnalysis.class, "population_trip_stats_comparison.csv",
//					"--input-base-path=" + this.comparisonPaths);
//			})
			.el(Plotly.class, (viz, data) -> {

				viz.title = "Mode usage";
				viz.description = "Share of persons using a main mode at least once per day";
				viz.width = 2d;

				Plotly.DataSet ds = viz.addDataset(data.compute(TripAnalysis.class, "mode_users.csv"));
				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("sim").build(), ds.mapping()
					.x("main_mode")
					.y("user")
				);

				if (tripsComparison) {
					int i = 0;
					for   (String path : this.comparisonPaths)
					{

						String modeUsersTripCsv =  path + "/analysis/population/mode_users.csv";

						viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("base_" + i).build(),
							viz.addDataset(data.resource(modeUsersTripCsv)).mapping()
							.x("main_mode")
							.y("user")
						);

						i++;
					}
				}
			});

		createDistancePlot(layout, tab);


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
				.showLegend(false)
				.build();

			viz.colorRamp = ColorScheme.Viridis;
			viz.interactive = Plotly.Interactive.dropdown;

			Plotly.DataSet ds = viz.addDataset(data.compute(TripAnalysis.class, "mode_share_distance_distribution.csv"))
				.pivot(List.of("dist"), "main_mode", "share")
				.constant("source", "Sim");

			viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.mode(ScatterTrace.Mode.LINE)
					.build(),
				ds.mapping()
					.name("main_mode")
					.x("dist")
					.y("share")
			);

			if (tripsComparison) {
				int i = 0;
				for (String path : this.comparisonPaths) {
					String modeUsersTripCsv =  path + "/analysis/population/mode_share_distance_distribution.csv";

					viz.description += " Dashed line represents the reference data.";

					Plotly.DataSet ref = viz.addDataset(data.resource(modeUsersTripCsv))
						.pivot(List.of("dist"), "main_mode", "share")
						.constant("source", "Base_" + i);

					viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
							.mode(ScatterTrace.Mode.LINE)
							.line(tech.tablesaw.plotly.components.Line.builder().dash(Line.Dash.DASH).color("Base_" + i).build())
							.build(),
						ref.mapping()
							.name("main_mode")
							.text("source")
							.x("dist")
							.y("share")
					);
					i++;
				}
			}

		});

	}

}
