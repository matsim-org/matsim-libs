package org.matsim.simwrapper.dashboard;

import jakarta.annotation.Nullable;
import org.matsim.application.analysis.population.TripAnalysis;
import org.matsim.application.analysis.scenarioComparison.ScenarioComparisonAnalysis;
import org.matsim.simwrapper.ComparisonDashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.*;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.traces.BarTrace;

import java.util.List;
import java.util.Map;

public class ScenarioComparisonDashboard implements ComparisonDashboard {

	private String pathToBaseCase;

	@Override
	public String getPathToBaseCase() {
		return pathToBaseCase;
	}

	@Override
	public void setPathToBaseCase(String path) {
		this.pathToBaseCase = path;
	}

	private String constructorBasePath;

	private boolean tripsComparison;
	private boolean scenarioComparison;
	private ScenarioComparisonAnalysis scenarioComparisonAnalysis;

	/**
	 * Default scenario comparison dashboard constructor.
	 */
	public ScenarioComparisonDashboard(String basePath,  boolean tripsComparison) {
		this.tripsComparison  = tripsComparison;
		constructorBasePath = basePath;
	}

	@Nullable
	private String modeShareTripCsv;
	@Nullable
	private String modeShareDistTripCsv;
	@Nullable
	private String modeUsersTripCsv;

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Scenario Comparison: Policy to Base Case";
		header.description = "Shows the differences in a variety of metrics between the policy and base case.";

		String tab = "Trips Comparison";

		Layout.Row first = layout.row("first", tab);
		first.el(Plotly.class, (viz, data) -> {
			viz.title = "Modal split";

			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
				.build();

			Plotly.DataSet ds = viz.addDataset(data.compute(TripAnalysis.class, "mode_share.csv"))
				.constant("source", "Simulated")
				.aggregate(List.of("main_mode"), "share", Plotly.AggrFunc.SUM);

			if (this.tripsComparison) {
				this.modeShareTripCsv = this.constructorBasePath + "/analysis/population/mode_share.csv";
				viz.addDataset(data.resource(modeShareTripCsv))
					.constant("source", "Base")
					.aggregate(List.of("main_mode"), "share", Plotly.AggrFunc.SUM);

				viz.mergeDatasets = true;
			}


			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).orientation(BarTrace.Orientation.HORIZONTAL).build(),
				ds.mapping()
					.name("main_mode")
					.y("source")
					.x("share")
			);
		});

		first.el(Plotly.class, (viz, data) -> {

			viz.title = "Trip distance distribution";
			viz.colorRamp = ColorScheme.Viridis;

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("Simulated").build(),
				viz.addDataset(data.compute(TripAnalysis.class, "mode_share.csv"))
					.aggregate(List.of("dist_group"), "share", Plotly.AggrFunc.SUM)
					.mapping()
					.x("dist_group")
					.y("share")
			);

			if (this.tripsComparison) {
				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("Base").build(),
					viz.addDataset(data.resource(modeShareTripCsv))
						.aggregate(List.of("dist_group"), "share", Plotly.AggrFunc.SUM)
						.mapping()
						.x("dist_group")
						.y("share")
				);
			}
		});

		layout.row("second", tab)
			.el(Table.class, (viz, data) -> {
				viz.title = "Mode Statistics";
				viz.description = "by main mode, over whole trip (including access & egress)";
				viz.dataset = data.compute(ScenarioComparisonAnalysis.class, "trip_stats_comparison.csv",
					"--input-base-path=" + constructorBasePath);
				viz.showAllRows = true;
			})
			.el(Plotly.class, (viz, data) -> {

				viz.title = "Modal distance distribution";

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.xAxis(Axis.builder().title("Distance group").build())
					.yAxis(Axis.builder().title("Share").build())
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
					.build();

				Plotly.DataSet sim = viz.addDataset(data.compute(TripAnalysis.class, "mode_share_per_dist.csv"))
					.constant("source", "Sim");

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
					sim.mapping()
						.name("main_mode")
						.x("dist_group")
						.y("share")
				);

				if (tripsComparison) {
					this.modeShareDistTripCsv = this.constructorBasePath + "/analysis/population/mode_share_per_dist.csv";
					Plotly.DataSet ref = viz.addDataset(data.resource(modeShareDistTripCsv))
						.constant("source", "Base");

					viz.multiIndex = Map.of("dist_group", "source");
					viz.mergeDatasets = true;
				}

			});

		layout.row("third", tab)
			.el(Table.class, (viz, data) -> {
				viz.title = "Population statistics";
				viz.description = "over simulated persons (not scaled by sample size)";
				viz.showAllRows = true;
				viz.dataset = data.compute(TripAnalysis.class, "population_trip_stats.csv");
				viz.dataset = data.compute(ScenarioComparisonAnalysis.class, "population_trip_stats_comparison.csv",
					"--input-base-path=" + constructorBasePath);
			})
			.el(Plotly.class, (viz, data) -> {

				viz.title = "Mode usage";
				viz.description = "Share of persons using a main mode at least once per day";
				viz.width = 2d;

				Plotly.DataSet ds = viz.addDataset(data.compute(TripAnalysis.class, "mode_users.csv"));
				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(), ds.mapping()
					.x("main_mode")
					.y("user")
					.name("main_mode")
				);

				if (tripsComparison) {
					ds.constant("source", "sim");

					modeUsersTripCsv =  this.constructorBasePath + "/analysis/population/mode_users.csv";
					viz.addDataset(data.resource(modeUsersTripCsv))
						.constant("source", "Base");

					viz.multiIndex = Map.of("main_mode", "source");
					viz.mergeDatasets = true;
				}

			});

	}

	//	priority is set to a lower number in order to force this class to be executed after population and emissions folders are already generated
	@Override
	public double priority() {
		return -2;
	}

}
