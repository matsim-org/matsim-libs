package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.accessibility.AccessibilityAnalysis;
import org.matsim.application.analysis.accessibility.PrepareDrtStops;
import org.matsim.application.analysis.accessibility.PrepareTransitSchedule;
import org.matsim.application.analysis.traffic.TrafficAnalysis;
import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Data;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.*;

import java.util.List;

/**
 * Shows emission in the scenario.
 */
public class AccessibilityBerlinDashboard implements Dashboard {


	private final List<String> pois;
	private final String coordinateSystem;
	private final Modes4Accessibility mode;
	private final String modeStylized;

	public double[] globalCenter;
	public Double globalZoom = 10d;

	public double height = 15d;


	/**
	 * Best provide the crs from {@link org.matsim.core.config.groups.GlobalConfigGroup}
	 *
	 * @param coordinateSystem
	 */
	public AccessibilityBerlinDashboard(String coordinateSystem, List<String> pois, Modes4Accessibility mode) {

		this.coordinateSystem = coordinateSystem;
		this.pois = pois;
		this.mode = mode;

		if (mode.equals(Modes4Accessibility.estimatedDrt)) {
			this.modeStylized = "DRT";
		} else if (mode.equals(Modes4Accessibility.pt)) {
			this.modeStylized = "PT";
		} else {
			this.modeStylized = mode.name().substring(0, 1).toUpperCase() + mode.name().substring(1).toLowerCase();
		}

	}

	@Override
	public void configure(Header header, Layout layout) {


		header.title = "Accessibility: " + modeStylized;
		header.description = "Shows accessibility for " + modeStylized + " to different points of interest.";
		header.tab = modeStylized;

		header.fullScreen = true;



		for(String poi : pois) {


			layout.row("accessibility-" + poi)
				.el(GridMap.class, (viz, data) -> {
					globalCenter = data.context().getCenter();
					generateAccessibilityGridMap(modeStylized, mode.name() + "_accessibility", poi, viz, data);

				});

//			layout.row("equity-" + poi).el(Bar.class, (viz, data) -> {
//
//				viz.title = "Accessibility Distribution " + modeStylized + " to " + poi;
//				viz.description = "xxx";
//				viz.height = height;
//				viz.dataset = data.computeWithPlaceholder(AccessibilityDistributionAnalysis.class, "%s/accessibilities_distribution.csv", poi);
//				viz.x = "bin";
//				viz.columns = List.of(mode.name() + "_accessibility");
//				viz.xAxisName = "Accessibility Bin (utils)";
//				viz.yAxisName = "Population";
//			}).el(Table.class, (viz, data) -> {
//				viz.title = "Accessibility Distribution " + modeStylized + " to " + poi;
//				viz.description = "xxx";
//				viz.height = height;
//				viz.dataset = data.computeWithPlaceholder(AccessibilityDistributionAnalysis.class, "%s/accessibilities_distribution.csv", poi);
//				viz.show = List.of("bin", mode.name() + "_accessibility");
//			} );

			layout.tab(poi).add("accessibility-" + poi);

		}

		if (mode.equals(Modes4Accessibility.car)) {

			layout.row("transport system").el(MapPlot.class, (viz, data) -> {

				viz.title = "Traffic statistics";
				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;

				viz.setShape(data.compute(CreateAvroNetwork.class, "network.avro"), "id");

				viz.addDataset("traffic", data.compute(TrafficAnalysis.class, "traffic_stats_by_link_daily.csv", "--transport-modes", "car"));

				viz.display.lineColor.dataset = "traffic";
				viz.display.lineColor.columnName = "avg_speed";
				viz.display.lineColor.join = "link_id";
				viz.display.lineColor.setColorRamp(ColorScheme.RdYlBu, 5, false);

				viz.display.lineWidth.dataset = "traffic";
				viz.display.lineWidth.columnName = "simulated_traffic_volume";
				viz.display.lineWidth.scaleFactor = 200000d;
				viz.display.lineWidth.join = "link_id";

				viz.height = 12d;
			});
		} else if (mode.equals(Modes4Accessibility.pt)) {
			layout.row("transport system").el(TransitViewer.class, (viz, data) -> {
				viz.title = "Transit Viewer";
				viz.height = 12d;
				viz.description = "Visualize the transit schedule.";

				// Include a network that has not been filtered
				viz.network = data.withContext("all").compute(CreateAvroNetwork.class, "network.avro",
					"--mode-filter", "", "--shp", "none");

				viz.transitSchedule = data.compute(PrepareTransitSchedule.class, "transit-schedule-reduced.xml.gz");
				viz.network = data.compute(PrepareTransitSchedule.class, "network-reduced.xml.gz");
			});
		} else if (mode.equals(Modes4Accessibility.estimatedDrt)) {
			layout.row("transport system").el(MapPlot.class, (viz, data) -> {
				viz.title = "DRT Stops";
				viz.description = "Shows DRT stops";
				viz.setShape(data.compute(PrepareDrtStops.class, "stops.shp"));
				viz.height = height;
				viz.center = globalCenter;
			});
		}

	}

	private void generateAccessibilityGridMap(String modeName, String columnName, String poi, GridMap viz, Data data) {
		viz.title = modeName + " accessibility to " + poi;
		viz.unit = "Utils";
		viz.description = "Plot shows " + modeName + "'s improvement in accessibility over walk. Blue (positive) values indicate " + modeName + " is beneficial; red is the opposite";
		viz.height = height;
		viz.projection = this.coordinateSystem;
		viz.center = globalCenter;
		viz.zoom = globalZoom;
		viz.cellSize = 500;
		viz.opacity = 1.0;
		viz.maxHeight = 0;


		viz.file = data.computeWithPlaceholder(AccessibilityAnalysis.class, "%s/accessibilities_simwrapper.csv", poi);
		viz.valueColumn = columnName;
		viz.secondValueColumn = "teleportedWalk_accessibility";
		viz.diff = !modeName.equals(Modes4Accessibility.teleportedWalk.toString());

		// Color Ramp
		viz.setColorRamp(ColorScheme.RdBu, 11, false).
			setColorRampBounds(true, -10, 10);

	}
}
