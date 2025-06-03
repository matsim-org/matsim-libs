package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.accessibility.AccessibilityAnalysis;
import org.matsim.application.analysis.accessibility.PrepareDrtStops;
import org.matsim.application.analysis.accessibility.PreparePois;
import org.matsim.application.analysis.accessibility.PrepareTransitSchedule;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Data;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.*;

import java.util.Iterator;
import java.util.List;

/**
 * Shows emission in the scenario.
 */
public class AccessibilityDashboardKelheim implements Dashboard {


	private final List<String> pois;
	private final String coordinateSystem;
	private final List<Modes4Accessibility> modes;


	/**
	 * Best provide the crs from {@link org.matsim.core.config.groups.GlobalConfigGroup}
	 *
	 * @param coordinateSystem
	 */
	public AccessibilityDashboardKelheim(String coordinateSystem, List<String> pois, List<Modes4Accessibility> modes) {

		this.coordinateSystem = coordinateSystem;
		this.pois = pois;
		this.modes = modes;

	}

	@Override
	public void configure(Header header, Layout layout) {


		header.title = "Accessibility";
		header.description = "Shows accessibility for different modes of transport to different points of interest.";


		for (String poi : pois) {



			layout.row("pois-" + poi).el(MapPlot.class, ((viz, data) -> {
				viz.title = "POIs: " + poi;
				viz.description = "Shows points of interest of type " + poi;
				viz.setShape(data.computeWithPlaceholder(PreparePois.class, "%s/pois.shp", poi));
				viz.display.fill.fixedColors = new String[]{"#f28e2c"};
				viz.height = 12.;

			}));


			layout.tab(poi).add("pois-" + poi);

			// ROW 1: CAR & WALK
			{
				Modes4Accessibility modeLeft = Modes4Accessibility.car;
				Modes4Accessibility modeRight = Modes4Accessibility.teleportedWalk;
				layout.row("row1-" + poi)
					.el(GridMap.class, (viz, data) -> {
						accessibilityDataGridMap(modeLeft.name(), modeLeft.name() + "_accessibility", poi, viz, data);
					}).el(GridMap.class, (viz, data) -> {
						accessibilityDataGridMap(modeRight.name(), modeRight.name() + "_accessibility", poi, viz, data);
					});

				layout.tab(poi).add("row1-" + poi);
			}
			// ROW 2: PT (network + accessibility)
			// TODO: ADD POIS

			{
				Modes4Accessibility mode = Modes4Accessibility.pt;
				layout.row("row2-" + poi)
					.el(TransitViewer.class, ((viz, data) -> {
					viz.title = "Public Transit";
					viz.description = "Shows public transit network";
//					viz.transitSchedule = "*output_transitSchedule.xml.gz";
//						viz.network = "*output_network.xml.gz";
					viz.transitSchedule = data.compute(PrepareTransitSchedule.class, "transit-schedule-reduced.xml.gz");
					viz.network = data.compute(PrepareTransitSchedule.class, "network-reduced.xml.gz");
				}))
					.el(GridMap.class, (viz, data) -> {
					String modeName = mode.name();
					viz.title = modeName + " accessibility to " + poi;
					viz.unit = "Utils";
					viz.description = "yellow: high accessibility; purple: low accessibility";
					viz.setColorRamp(ColorScheme.Viridis);
					viz.cellSize = 500;
					viz.opacity = 0.75;
					viz.maxHeight = 0;

					viz.projection = this.coordinateSystem;
					viz.center = data.context().getCenter();
					viz.zoom = data.context().mapZoomLevel;
					viz.file = "analysis/accessibility/" + poi + "/pt_accessibilities_simwrapper.csv";

					viz.valueColumn = mode.name() + "_accessibility";
					viz.height = 12.;
//					accessibilityDataGridMap(mode.name(), mode.name() + "_accessibility", poi, viz, data);
				})
				;
			}


			layout.tab(poi).add("row2-" + poi);


			// ROW 3: DRT (stops + accessibility)
//			{
//				Modes4Accessibility mode = Modes4Accessibility.estimatedDrt;
//				layout.row("row3-" + poi).el(MapPlot.class, ((viz, data) -> {
//					viz.title = "Demand Responsive Transit";
//					viz.description = "Shows DRT stops";
//					viz.setShape(data.compute(PrepareDrtStops.class, "stops.shp"));
//				})).el(GridMap.class, (viz, data) -> {
//					accessibilityDataGridMap(mode.name(), mode.name() + "_accessibility", poi, viz, data);
//				});
//			}
//			layout.tab(poi).add("row3-" + poi);
		}

	}

	private void accessibilityDataGridMap(String modeName, String columnName, String poi, GridMap viz, Data data) {
		viz.title = modeName + " accessibility to " + poi;
		viz.unit = "Utils";
		viz.description = "yellow: high accessibility; purple: low accessibility";
		viz.setColorRamp(ColorScheme.Viridis);
		viz.cellSize = 500;
		viz.opacity = 0.75;
		viz.maxHeight = 0;

		viz.projection = this.coordinateSystem;
		viz.center = data.context().getCenter();
		viz.zoom = data.context().mapZoomLevel;
		viz.file = data.computeWithPlaceholder(AccessibilityAnalysis.class, "%s/accessibilities_simwrapper.csv", poi);
		viz.valueColumn = columnName;
		viz.height = 12.;
	}
}
