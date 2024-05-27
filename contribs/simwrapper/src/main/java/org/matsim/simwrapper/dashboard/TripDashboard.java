package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.population.TripAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.ColorScheme;
import org.matsim.simwrapper.viz.Plotly;
import org.matsim.simwrapper.viz.Table;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.traces.BarTrace;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Shows trip information, optionally against reference data.
 */
public class TripDashboard implements Dashboard {

	@Nullable
	private final String modeShareRefCsv;
	@Nullable
	private final String modeShareDistRefCsv;
	@Nullable
	private final String modeUsersRefCsv;

	private String[] args;

	/**
	 * Default trip dashboard constructor.
	 */
	public TripDashboard() {
		this(null, null, null);
	}

	/**
	 * Create a dashboard containing reference data. If any of the reference data is not available it can also be null
	 * Data format needs to be the same as produced by the analysis. Please refer to the dashboard output.
	 * All given argument must be resources in the classpath.
	 *
	 * @param modeShareRefCsv     resource containing the mode share per distance group and mode, summing to a total of one
	 * @param modeShareDistRefCsv resource with mode share, where each group sums to 1.
	 * @param modeUsersRefCsv     resource with mode users data
	 */
	public TripDashboard(@Nullable String modeShareRefCsv, @Nullable String modeShareDistRefCsv, @Nullable String modeUsersRefCsv) {
		this.modeShareRefCsv = modeShareRefCsv;
		this.modeShareDistRefCsv = modeShareDistRefCsv;
		this.modeUsersRefCsv = modeUsersRefCsv;
		args = new String[0];
	}

	/**
	 * Set argument that will be passed to the analysis script. See {@link TripAnalysis}.
	 */
	public TripDashboard setAnalysisArgs(String... args) {
		this.args = args;
		return this;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Trips";
		header.description = "General information about modal share and trip distributions.";

		Layout.Row first = layout.row("first");
		first.el(Plotly.class, (viz, data) -> {
			viz.title = "Modal split";

			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
				.build();

			Plotly.DataSet ds = viz.addDataset(data.compute(TripAnalysis.class, "mode_share.csv", args))
				.constant("source", "Simulated")
				.aggregate(List.of("main_mode"), "share", Plotly.AggrFunc.SUM);

			if (modeShareRefCsv != null) {
				viz.addDataset(data.resource(modeShareRefCsv))
					.constant("source", "Reference")
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
				viz.addDataset(data.compute(TripAnalysis.class, "mode_share.csv", args))
					.aggregate(List.of("dist_group"), "share", Plotly.AggrFunc.SUM)
					.mapping()
					.x("dist_group")
					.y("share")
			);

			if (modeShareRefCsv != null) {
				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("Reference").build(),
					viz.addDataset(data.resource(modeShareRefCsv))
						.aggregate(List.of("dist_group"), "share", Plotly.AggrFunc.SUM)
						.mapping()
						.x("dist_group")
						.y("share")
				);
			}
		});

		layout.row("second")
			.el(Table.class, (viz, data) -> {
				viz.title = "Mode Statistics";
				viz.description = "by main mode, over whole trip (including access & egress)";
				viz.dataset = data.compute(TripAnalysis.class, "trip_stats.csv", args);
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

				if (modeShareDistRefCsv != null) {

					Plotly.DataSet ref = viz.addDataset(data.resource(modeShareDistRefCsv))
						.constant("source", "Ref");

					viz.multiIndex = Map.of("dist_group", "source");
					viz.mergeDatasets = true;
				}

			});

		layout.row("third")
			.el(Table.class, (viz, data) -> {
				viz.title = "Population statistics";
				viz.description = "over simulated persons (not scaled by sample size)";
				viz.showAllRows = true;
				viz.dataset = data.compute(TripAnalysis.class, "population_trip_stats.csv");
			})
			.el(Plotly.class, (viz, data) -> {

				viz.title = "Mode usage";
				viz.description = "Share of persons using a main mode at least once per day.";
				viz.width = 2d;

				Plotly.DataSet ds = viz.addDataset(data.compute(TripAnalysis.class, "mode_users.csv"));
				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(), ds.mapping()
					.x("main_mode")
					.y("user")
					.name("main_mode")
				);

				if (modeUsersRefCsv != null) {
					ds.constant("source", "sim");

					viz.addDataset(data.resource(modeUsersRefCsv))
						.constant("source", "ref");

					viz.multiIndex = Map.of("main_mode", "source");
					viz.mergeDatasets = true;
				}

			});


		layout.row("departures").el(Plotly.class, (viz, data) -> {

			viz.title = "Departures";
			viz.description = "by hour and purpose";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Hour").build())
				.yAxis(Axis.builder().title("Share").build())
				.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
				.build();

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
				viz.addDataset(data.compute(TripAnalysis.class, "trip_purposes_by_hour.csv")).mapping()
					.name("purpose", ColorScheme.Spectral)
					.x("h")
					.y("departure")
			);

		});

		layout.row("arrivals").el(Plotly.class, (viz, data) -> {

			viz.title = "Arrivals";
			viz.description = "by hour and purpose";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Hour").build())
				.yAxis(Axis.builder().title("Share").build())
				.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
				.build();

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
				viz.addDataset(data.compute(TripAnalysis.class, "trip_purposes_by_hour.csv")).mapping()
					.name("purpose", ColorScheme.Spectral)
					.x("h")
					.y("arrival")
			);

		});

	}
}
