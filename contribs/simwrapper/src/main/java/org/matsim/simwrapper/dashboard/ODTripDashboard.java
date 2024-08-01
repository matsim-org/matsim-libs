package org.matsim.simwrapper.dashboard;

import org.apache.commons.lang3.StringUtils;
import org.matsim.application.analysis.population.FilterTripModes;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Data;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Hexagons;

import java.util.Set;


/**
 * Dashboard to show the OD of trips per mode.
 */
public class ODTripDashboard implements Dashboard {

	private final Set<String> modes;
	private final String crs;

	/**
	 * Create a dashboard to show aggregated OD information per mode.
	 *
	 * @param crs   Coordinate systems is needed because the aggregation is done in the front-end
	 * @param modes modes to show
	 */
	public ODTripDashboard(Set<String> modes, String crs) {
		this.modes = modes;
		this.crs = crs;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.tab = "OD";
		header.title = "OD Trips";
		header.description = "Shows the number or trips origins or destinations, aggregated into bins.";
		header.fullScreen = true;

		layout.row("all_od", "All Modes")
			.el(Hexagons.class, (viz, data) -> {
				viz.title = "OD";
				viz.description = "Total O/D of all modes";
				viz.file = "*output_trips.csv.gz";
				definePlot(viz, data);
			});


		for (String mode : modes) {
			layout.row(mode + "_od", StringUtils.capitalize(mode))
				.el(Hexagons.class, (viz, data) -> {
					viz.title = "OD";
					viz.description = "O/D trips for mode " + mode;
					viz.file = data.computeWithPlaceholder(FilterTripModes.class, "trips_per_mode_%s.csv", mode);
					definePlot(viz, data);
				});
		}

	}

	/**
	 * Default plot settings.
	 */
	private void definePlot(Hexagons viz, Data data) {
		viz.projection = this.crs;
		viz.center = data.context().getCenter();
		viz.zoom = data.context().mapZoomLevel;
		viz.height = 15.;

		viz.maxHeight = 200d;
		viz.radius = 500d;

		viz.addAggregation("OD Summary",
			"Origins", "start_x", "start_y",
			"Destinations", "end_x", "end_y");
	}
}
