//package org.matsim.simwrapper.dashboard;
//
//import org.matsim.application.analysis.accessibility.*;
//import org.matsim.contrib.accessibility.Modes4Accessibility;
//import org.matsim.simwrapper.Dashboard;
//import org.matsim.simwrapper.Data;
//import org.matsim.simwrapper.Header;
//import org.matsim.simwrapper.Layout;
//import org.matsim.simwrapper.viz.*;
//
//import java.util.List;
//
///**
// * Shows emission in the scenario.
// */
//public class AccessibilityDashboardKelheim implements Dashboard {
//
//
//	private final String poi;
//	private final String coordinateSystem;
//	private final List<Modes4Accessibility> modes;
//	public double[] globalCenter;
//	public Double globalZoom = 10d;
//
//	public double height = 15d;
//
//
//	/**
//	 * Best provide the crs from {@link org.matsim.core.config.groups.GlobalConfigGroup}
//	 *
//	 * @param coordinateSystem
//	 */
//	public AccessibilityDashboardKelheim(String coordinateSystem, String poi, List<Modes4Accessibility> modes) {
//
//		this.coordinateSystem = coordinateSystem;
//		this.poi = poi;
//		this.modes = modes;
//
//	}
//
//	@Override
//	public void configure(Header header, Layout layout) {
//
//
//		header.title = "Accessibility: " + poi;
//		header.description = "Shows accessibility for different modes of transport to different points of interest.";
//		header.tab = "Acc: " + poi;
//		header.fullScreen = false;
//
//
//		// TAB 0: Population Density and POIs
//		{
//			layout.row("inputs")
//				.el(Hexagons.class, (this::generateHouseholds))
//				.el(MapPlot.class, (this::generatePois));
//
//			layout.tab("Overview").add("inputs");
//		}
//
//		// TAB 1: CAR
//
//		{
//			Modes4Accessibility mode = Modes4Accessibility.car;
//
//
//			layout.row("accessibility-" + mode)
//				.el(GridMap.class, (viz, data) -> {
//					globalCenter = data.context().getCenter();
//					generateAccessibilityGridMap(mode.name(), mode.name() + "_accessibility", poi, viz, data);
//
//				});
////			.el(Links.class, ((viz, data) -> {
////					generateNetwork(viz);
////				}));
//
//			layout.tab(mode.name()).add("accessibility-" + mode);
//
////			layout.row("inputs-" + mode).el(Hexagons.class, (this::generateHouseholds))
////				.el(MapPlot.class, ((viz, data) -> {
////					generatePois(viz, data);
////
////			}));
////
////			layout.tab(mode.name()).add("inputs-" + mode);
//
//
//		}
//
//		// TAB 2: WALK
//		{
//			Modes4Accessibility mode = Modes4Accessibility.teleportedWalk;
//
//			layout.row("accessibility-" + mode)
//				.el(GridMap.class, (viz, data) -> {
//					generateAccessibilityGridMap(mode.name(), mode.name() + "_accessibility", poi, viz, data);
//					viz.diff = false;
//				});
////				.el(Links.class, ((viz, data) -> {
////					generateNetwork(viz);
////				}));
//
//			layout.tab(mode.name()).add("accessibility-" + mode);
//
////			layout.row("inputs-" + mode)
////				.el(Hexagons.class, (this::generateHouseholds))
////				.el(MapPlot.class, (this::generatePois));
////
////			layout.tab(mode.name()).add("inputs-" + mode);
//
//		}
//		// TAB 3: PT (network + accessibility)
//
//		{
//			Modes4Accessibility mode = Modes4Accessibility.pt;
//
//			layout.row("accessibility-" + mode)
//				.el(GridMap.class, (viz, data) -> {
//					generateAccessibilityGridMap(mode.name(), mode.name() + "_accessibility", poi, viz, data);
//				});
////				.el(TransitViewer.class, ((viz, data) -> {
////					viz.title = "Public Transit";
////					viz.description = "Shows public transit network";
////					viz.transitSchedule = data.compute(PrepareTransitSchedule.class, "transit-schedule-reduced.xml.gz");
////					viz.network = data.compute(PrepareTransitSchedule.class, "network-reduced.xml.gz");
////					viz.height = height;
////				}));
//
//			layout.tab(mode.name()).add("accessibility-" + mode);
////
////			layout.row("inputs-" + mode)
////				.el(Hexagons.class, (this::generateHouseholds))
////				.el(MapPlot.class, (this::generatePois));
////			layout.tab(mode.name()).add("inputs-" + mode);
//
//		}
//
//			// TAB 4: DRT (stops + accessibility)
//		{
//
//			Modes4Accessibility mode = Modes4Accessibility.estimatedDrt;
//
//			layout.row("accessibility-" + mode)
//				.el(GridMap.class, (viz, data) -> {
//					generateAccessibilityGridMap(mode.name(), mode.name() + "_accessibility", poi, viz, data);
//				});
//
//
//			layout.tab(mode.name()).add("accessibility-" + mode);
//
//			layout.row("transport-" + mode).el(MapPlot.class, ((viz, data) -> {
//				viz.title = "Demand Responsive Transit";
//				viz.description = "Shows DRT stops";
//				viz.setShape(data.compute(PrepareDrtStops.class, "stops.shp"));
//				viz.height = height;
//				viz.center = globalCenter;
//			}));
//
//			layout.tab(mode.name()).add("transport-" + mode);
//
////			layout.row("inputs-" + mode)
////				.el(Hexagons.class, (this::generateHouseholds))
////				.el(MapPlot.class, (this::generatePois));
////
////			layout.tab(mode.name()).add("inputs-" + mode);
//
//		}
//	}
//
//	private void generatePois(MapPlot viz, Data data) {
//		viz.title = "POIs: " + poi;
//		viz.description = "Shows points of interest of type " + poi;
//		viz.setShape(data.computeWithPlaceholder(PreparePois.class, "%s/pois.shp", poi));
//		viz.display.fill.fixedColors = new String[]{"#f28e2c"};
//		viz.height = height;
//		viz.center = globalCenter;
//		viz.zoom = globalZoom;
//	}
//
//	private void generateHouseholds(Hexagons viz, Data data) {
//		viz.title = "Households";
//		viz.description = "Shows households";
//		viz.projection = this.coordinateSystem;
//		viz.file = data.compute(PrepareHouseholds.class, "persons.csv");
//		viz.addAggregation("Home", "Origins", "home_x", "home_y");
//		viz.height = height;
//		viz.center = globalCenter;
//		viz.zoom = globalZoom;
//	}
//
//	private void generateNetwork(Links viz) {
//		viz.title = "Network";
//		viz.description = "Shows network";
//		viz.network = "*output_network.xml.gz";
//		viz.center = globalCenter;
//		viz.height = height;
//	}
//
//
//	private void generateAccessibilityGridMap(String modeName, String columnName, String poi, GridMap viz, Data data) {
//		viz.title = modeName + " accessibility to " + poi;
//		viz.unit = "Utils";
//		viz.description = "Plot shows " + modeName + "'s improvement in accessibility over walk. Blue (positive) values indicate " + modeName + " is beneficial; red is the opposite";
//		viz.height = height;
//		viz.projection = this.coordinateSystem;
//		viz.center = globalCenter;
//		viz.zoom = globalZoom;
//		viz.cellSize = 500;
//		viz.opacity = 1.0;
//		viz.maxHeight = 0;
//
//
//		viz.file = data.computeWithPlaceholder(AccessibilityAnalysis.class, "%s/accessibilities_simwrapper.csv", poi);
//		viz.valueColumn = columnName;
//		viz.secondValueColumn = "teleportedWalk_accessibility";
//		viz.diff = true;
//
//		// Color Ramp
//		viz.setColorRamp(ColorScheme.RdBu, 11, false).
//			setColorRampBounds(true, -10, 10);
//
//	}
//}
