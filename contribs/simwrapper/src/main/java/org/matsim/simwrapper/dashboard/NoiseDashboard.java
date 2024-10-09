package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.noise.NoiseAnalysis;
import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.simwrapper.*;
import org.matsim.simwrapper.viz.ColorScheme;
import org.matsim.simwrapper.viz.GridMap;
import org.matsim.simwrapper.viz.MapPlot;
import org.matsim.simwrapper.viz.Tile;

/**
 * Shows emission in the scenario.
 */
public class NoiseDashboard implements Dashboard {

	private double minDb = 40;
	private double maxDb = 80;

	private final String coordinateSystem;

	/**
	 * Best provide the crs from {@link org.matsim.core.config.groups.GlobalConfigGroup}
	 * @param coordinateSystem for the {@link GridMap}
	 */
	public NoiseDashboard(String coordinateSystem) {
		this.coordinateSystem = coordinateSystem;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Noise";
		header.description = "Shows the noise footprint and spatial distribution.";

		layout.row("stats")
			.el(Tile.class, (viz, data) -> {
				viz.dataset = data.compute(NoiseAnalysis.class, "noise_stats.csv");
				viz.height = 0.1;
			});
		layout.row("emissions")
			.el(MapPlot.class, (viz, data) -> {
				viz.title = "Noise Emissions (Link)";
				viz.description = "Maximum Noise Level per day [dB]";
				viz.height = 12.0;
				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
				viz.minValue = minDb;
				viz.maxValue = maxDb;
				viz.setShape(data.compute(CreateAvroNetwork.class, "network.avro", "--with-properties"), "id");
				viz.addDataset("noise", data.compute(NoiseAnalysis.class, "emission_per_day.csv"));
				viz.display.lineColor.dataset = "noise";
				viz.display.lineColor.columnName = "value";
				viz.display.lineColor.join = "Link Id";
				//viz.display.lineColor.fixedColors = new String[]{"#1175b3", "#95c7df", "#f4a986", "#cc0c27"};
				viz.display.lineColor.setColorRamp(ColorScheme.Oranges, 8, false, "35, 45, 55, 65, 75, 85, 95");
				viz.display.lineWidth.dataset = "noise";
				viz.display.lineWidth.columnName = "value";
				viz.display.lineWidth.scaleFactor = 8d;
				viz.display.lineWidth.join = "Link Id";
			});
		layout.row("imissions")
			.el(GridMap.class, (viz, data) -> {
				viz.title = "Noise Immissions (Grid)";
				viz.description = "Total Noise Immissions per day";
				DashboardUtils.setGridMapStandards(viz, data, this.coordinateSystem);
				viz.file = data.computeWithPlaceholder(NoiseAnalysis.class, "immission_per_day.%s", "avro");
			})
			.el(GridMap.class, (viz, data) -> {
				viz.title = "Hourly Noise Immissions (Grid)";
				viz.description = "Noise Immissions per hour";
				DashboardUtils.setGridMapStandards(viz, data, this.coordinateSystem);
				viz.file = data.computeWithPlaceholder(NoiseAnalysis.class, "immission_per_hour.%s", "avro");
			});
		layout.row("damages")
			.el(GridMap.class, (viz, data) -> {
				viz.title = "Daily Noise Damages (Grid)";
				viz.description = "Total Noise Damages per day [€]";
				DashboardUtils.setGridMapStandards(viz, data, this.coordinateSystem);
				viz.file = data.computeWithPlaceholder(NoiseAnalysis.class, "damages_receiverPoint_per_day.%s", "avro");
			})
			.el(GridMap.class, (viz, data) -> {
				viz.title = "Hourly Noise Damages (Grid)";
				viz.description = "Noise Damages per hour [€]";
				DashboardUtils.setGridMapStandards(viz, data, this.coordinateSystem);
				viz.file = data.computeWithPlaceholder(NoiseAnalysis.class, "damages_receiverPoint_per_hour.%s", "avro");
			});


	}

}
