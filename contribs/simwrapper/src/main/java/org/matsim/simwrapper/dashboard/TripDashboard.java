package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.population.TripAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Plotly;
import tech.tablesaw.plotly.traces.ScatterTrace;

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

		Layout.Row row = layout.row("general");

		header.title = "Trips";
		header.description = "General information about modal share and trip distributions.";

		row.el(Plotly.class, (viz, data) -> {

			viz.title = "Modal split";

			viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT).build(),
					Plotly.fromFile(data.compute(TripAnalysis.class, "mode_share.csv"))
							.y("main_mode")
							.x("share")
			);

			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
					.build();

		});


	}

}
