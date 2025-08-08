package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.accessibility.AccessibilityAnalysis;
import org.matsim.application.analysis.accessibility.AccessibilityDistributionAnalysis;
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
public class EquityBerlinDashboard implements Dashboard {


	private final List<String> pois;
	private final String coordinateSystem;

	public double[] globalCenter;
	public Double globalZoom = 10d;

	public double height = 15d;


	/**
	 * Best provide the crs from {@link org.matsim.core.config.groups.GlobalConfigGroup}
	 *
	 * @param coordinateSystem
	 */
	public EquityBerlinDashboard(String coordinateSystem, List<String> pois) {

		this.coordinateSystem = coordinateSystem;
		this.pois = pois;

	}

	@Override
	public void configure(Header header, Layout layout) {


		header.title = "Equity";
		header.description = "Shows distribution of accessibility (= transport equity) to different points of interest.";



		for(String poi : pois) {


			layout.row("equity-chart-" + poi).el(Bar.class, (viz, data) -> {

				viz.title = "Accessibility Distribution to " + poi;
				viz.height = height;
				viz.dataset = data.computeWithPlaceholder(AccessibilityDistributionAnalysis.class, "%s/accessibilities_distribution.csv", poi);
				viz.x = "bin";
				viz.columns = List.of("car_accessibility", "pt_accessibility", "teleportedWalk_accessibility","max_accessibility");
				viz.xAxisName = "Accessibility Bin (utils)";
				viz.yAxisName = "Population";
			});

			layout.row("equity-table-" + poi).el(Table.class, (viz, data) -> {
				viz.title = "Accessibility Distribution to " + poi;
				viz.height = height;
				viz.dataset = data.computeWithPlaceholder(AccessibilityDistributionAnalysis.class, "%s/accessibilities_distribution.csv", poi);
				viz.showAllRows = true;

//				viz.show = List.of("bin", mode.name() + "_accessibility");
			} );

			layout.tab(poi).add("equity-chart-" + poi).add("equity-table-" + poi);

		}

	}

}
