package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.population.TripAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Plotly;
import tech.tablesaw.plotly.traces.BarTrace;
import tech.tablesaw.plotly.traces.PieTrace;

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

					viz.addTrace(PieTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
							Plotly.fromFile(data.compute(TripAnalysis.class, "mode_share.csv"))
									.text("main_mode")
									.x("share")
					);
				})
				.el(Plotly.class, (viz, data) -> {

					viz.title = "Trip distance distribution";

					// TODO: sum over main_mode
					// ignores main_mode

					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
							Plotly.fromFile(data.compute(TripAnalysis.class, "mode_share.csv"))
									.x("dist_group")
									.y("share")
					);

					// TODO: second trace with the reference data should work fine
				});

		// TODO: can probably be in the same plot together with reference data


		layout.row("modal").el(Plotly.class, (viz, data) -> {

			viz.title = "Modal distance distribution";

			// TODO: how to setup, each mode should be one trace ?
			// difficult to create even with simple transformations

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
					Plotly.fromFile(data.compute(TripAnalysis.class, "mode_share.csv"))
							.x("dist_group")
							.y("share")
			);

		});

		// mode_users.csv

		// //		trip_stats.csv
		// TODO: mode usage, bar plot


		layout.row("arrivals").el(Plotly.class, (viz, data) -> {

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
					Plotly.fromFile(data.compute(TripAnalysis.class, "trip_purposes_by_hour.csv"))
							.groupBy("main_mode")
							.x("dist_group")
							.y("share")
			);

		});

		layout.row("departure").el(Plotly.class, (viz, data) -> {

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
					Plotly.fromFile(data.compute(TripAnalysis.class, "trip_purposes_by_hour.csv"))
							.groupBy("main_mode")
							.x("dist_group")
							.y("share")
			);

		});

	}
}
