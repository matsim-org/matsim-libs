package org.matsim.simwrapper.dashboard;

import org.apache.commons.lang3.StringUtils;
import org.matsim.application.analysis.population.FilterTripModes;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Hexagons;

import java.util.Set;

public class ODFlowDashboard implements Dashboard {

	private final Set<String> modes;
	private String crs;

	public ODFlowDashboard(Set<String> modes, String crs) {
		this.modes = modes;
		this.crs = crs;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "OD FLow";
		header.description = "Shows the Origin-Destination Flow";
		header.fullScreen = true;


			layout.row("all_od", "All Modes")
				.el(Hexagons.class, (viz, data) -> {
					viz.title = "OD";
					viz.description = "Total O/D of all modes";
					viz.file = "*output_trips.csv.gz";
					viz.projection = this.crs;
					viz.center = data.context().getCenter();
					viz.height = 15.;
					viz.addAggregation("OD Summary", "Origins", "start_x", "start_y", "Destinations", "end_x", "end_y");
				});


		for (String mode : modes) {
			layout.row(mode + "_od", StringUtils.capitalize(mode))
				.el(Hexagons.class, (viz, data) -> {
					viz.title = "OD";
					viz.description = "Total O/D of all modes";
					viz.file = data.computeWithPlaceholder(FilterTripModes.class, "trips_per_mode_%s.csv", mode);
					viz.projection = this.crs;
					viz.center = data.context().getCenter();
					viz.zoom = data.context().mapZoomLevel;
					viz.height = 15.;
					viz.addAggregation("OD Summary", "Origins", "start_x", "start_y", "Destinations", "end_x", "end_y");
				});
		}

	}
}
