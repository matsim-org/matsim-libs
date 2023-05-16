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

import java.util.List;

/**
 * Shows trip information, optionally against reference data.
 */
public class TripDashboard implements Dashboard {

	public TripDashboard() {

		// TODO: path to reference data
		// SHP file and filtering
		// mode filter

	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Trips";
		header.description = "General information about modal share and trip distributions.";

		// TODO: cmp to reference data?, via arguments and separate dashboard?

		layout.row("first")
			.el(Plotly.class, (viz, data) -> {

				viz.title = "Modal split";
				viz.description = "simulated";

				viz.addTrace(PieTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
					viz.addDataset(data.compute(TripAnalysis.class, "mode_share.csv")).mapping()
						.text("main_mode")
						.x("share")
				);
			})
			.el(Plotly.class, (viz, data) -> {

				viz.title = "Trip distance distribution";

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("simulated").build(),
					viz.addDataset(data.compute(TripAnalysis.class, "mode_share.csv"))
						.aggregate(List.of("dist_group"), "share", Plotly.AggrFunc.SUM)
						.mapping()
						.x("dist_group")
						.y("share")
				);

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

				// TODO: how to setup, each mode should be one trace ?
				// difficult to create even with simple transformations
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
					.name("purpose")
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
					.name("purpose")
					.x("h")
					.y("departure")
			);

		});

	}
}
