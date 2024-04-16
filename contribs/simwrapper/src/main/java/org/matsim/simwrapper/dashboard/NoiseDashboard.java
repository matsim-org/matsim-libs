package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.noise.NoiseAnalysis;
import org.matsim.application.prepare.network.CreateGeoJsonNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.ColorScheme;
import org.matsim.simwrapper.viz.GridMap;
import org.matsim.simwrapper.viz.MapPlot;

/**
 * Shows emission in the scenario.
 */
public class NoiseDashboard implements Dashboard {

	private double minDb = 40;
	private double maxDb = 80;

	/**
	 * Set the min and max values for the noise map.
	 */
	public NoiseDashboard withMinMaxDb(double minDb, double maxDb) {
		this.minDb = minDb;
		this.maxDb = maxDb;
		return this;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Noise";
		header.description = "Shows the noise footprint and spatial distribution.";

		layout.row("links")
			.el(GridMap.class, (viz, data) -> {
				viz.title = "Noise Immissions";
				viz.description = "Noise Immissions per hour";
				viz.height = 12.0;
				viz.cellSize = 250;
				viz.opacity = 0.2;
				viz.maxHeight = 20;
				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
				viz.setColorRamp("greenRed", 10, false);
				viz.file = data.computeWithPlaceholder(NoiseAnalysis.class, "immission_per_hour.%s", "avro.gz");
			});
		layout.row("links2")
			.el(GridMap.class, (viz, data) -> {
				viz.title = "Noise Immissions";
				viz.description = "Noise Immissions per day";
				viz.height = 12.0;
				viz.cellSize = 250;
				viz.opacity = 0.2;
				viz.maxHeight = 20;
				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
				viz.setColorRamp("greenRed", 10, false);
				viz.file = data.computeWithPlaceholder(NoiseAnalysis.class, "immission_per_day.%s", "avro.gz");
			});

		layout.row("links3")
			.el(MapPlot.class, (viz, data) -> {
				viz.title = "Noise Emissions";
				viz.description = "Noise Emmissions per day";
				viz.height = 12.0;
				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
				viz.minValue = minDb;
				viz.maxValue = maxDb;
				viz.setShape(data.compute(CreateGeoJsonNetwork.class, "network.geojson", "--with-properties"), "id");
				viz.addDataset("noise", data.compute(NoiseAnalysis.class, "emission_per_day.csv"));
				viz.display.lineColor.dataset = "noise";
				viz.display.lineColor.columnName = "value";
				viz.display.lineColor.join = "Link Id";
				viz.display.lineColor.setColorRamp(ColorScheme.RdYlBu, 5, true);
				viz.display.lineWidth.dataset = "noise";
				viz.display.lineWidth.columnName = "value";
				viz.display.lineWidth.scaleFactor = 8d;
				viz.display.lineWidth.join = "Link Id";
			});
	}
}
