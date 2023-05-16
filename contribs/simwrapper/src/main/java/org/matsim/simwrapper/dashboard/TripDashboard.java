package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.population.TripAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Plotly;
import org.matsim.simwrapper.viz.Table;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.traces.BarTrace;
import tech.tablesaw.plotly.traces.PieTrace;

import javax.annotation.Nullable;
import java.util.List;

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
	 * Only include agents that match this id. See {@link TripAnalysis}.
	 */
	public TripDashboard setMatchAgentId(String pattern) {
		// TODO: needs to be changed if there are more args
		args = new String[]{"--match-id", pattern};
		return this;
	}

	// TODO: dist groups configurable

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

			Plotly.DataSet ds = viz.addDataset(data.compute(TripAnalysis.class, "mode_share.csv"))
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
			viz.colorRamp = Plotly.ColorScheme.Viridis;

			// TODO: some color as static fields

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("Simulated").build(),
				viz.addDataset(data.compute(TripAnalysis.class, "mode_share.csv"))
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

				// TODO: second trace with the reference data should work fine
		});

		// TODO: can probably be in the same plot together with reference data

		layout.row("second")
			.el(Table.class, (viz, data) -> {
				viz.title = "Mode Statistics";
				viz.description = "by main mode, over whole trip (including access & egress)";
				viz.dataset = data.compute(TripAnalysis.class, "trip_stats.csv");
				viz.showAllRows = true;
			})
			.el(Plotly.class, (viz, data) -> {

				viz.title = "Modal distance distribution";

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.xAxis(Axis.builder().title("Distance group").build())
					.yAxis(Axis.builder().title("Share").build())
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
					.build();

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
					viz.addDataset(data.compute(TripAnalysis.class, "mode_share_per_dist.csv")).mapping()
						.name("main_mode")
						.x("dist_group")
						.y("share")
				);

			});

		layout.row("third")
			.el(Table.class, (viz, data) -> {
				viz.title = "Population statistics";
				viz.description = "over all simulated persons (not scaled by sample size)";
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

			});


		layout.row("arrivals").el(Plotly.class, (viz, data) -> {

			viz.title = "Departures";
			viz.description = "by hour and purpose";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Hour").build())
				.yAxis(Axis.builder().title("Share").build())
				.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
				.build();

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
				viz.addDataset(data.compute(TripAnalysis.class, "trip_purposes_by_hour.csv")).mapping()
					.name("purpose", Plotly.ColorScheme.Spectral)
					.x("h")
					.y("arrival")
			);

		});

		layout.row("departure").el(Plotly.class, (viz, data) -> {

			viz.title = "Arrivals";
			viz.description = "by hour and purpose";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Hour").build())
				.yAxis(Axis.builder().title("Share").build())
				.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
				.build();

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
				viz.addDataset(data.compute(TripAnalysis.class, "trip_purposes_by_hour.csv")).mapping()
					.name("purpose", Plotly.ColorScheme.Spectral)
					.x("h")
					.y("departure")
			);

		});

	}
}
