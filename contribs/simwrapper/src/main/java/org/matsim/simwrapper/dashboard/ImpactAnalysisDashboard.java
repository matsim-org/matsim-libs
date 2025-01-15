package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.impact.ImpactAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Table;

import java.util.Set;

/**
 * Dashboard with general overview.
 */
public class ImpactAnalysisDashboard implements Dashboard {

	private final Set<String> modes;

	/**
	 * Constructor.
	 *
	 * @param modes The modes to display.
	 */
	public ImpactAnalysisDashboard(Set<String> modes) {
		this.modes = modes;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Impact Analysis";
		header.description = "Impact overview of the MATSim run.";

		modes.forEach(mode -> {
			layout.row(mode)
				.el(Table.class, (viz, data) -> {
					viz.title = "Central Traffic / Physical Effects (" + mode.substring(0, 1).toUpperCase() + mode.substring(1) + ")";
					viz.style = "topsheet";
					viz.dataset = data.computeWithPlaceholder(ImpactAnalysis.class, "total_%s.csv", mode);
					viz.enableFilter = false;
					viz.showAllRows = true;
					viz.width = 1d;
					viz.alignment = new String[]{"right", "right", "left"};
				});
		});
//
//		layout.row("bike")
//			.el(Table.class, (viz, data) -> {
//				viz.title = "Zentrale verkehrliche / physikalische Wirkungen (Fahrrad)";
//				viz.style = "topsheet";
//				viz.dataset = data.computeWithPlaceholder(ImpactAnalysis.class, "total_%s.csv", "bike");
//				viz.enableFilter = false;
//				viz.showAllRows = true;
//				viz.width = 1d;
//			});
//
//		layout.row("car")
//			.el(Table.class, (viz, data) -> {
//				viz.title = "Zentrale verkehrliche / physikalische Wirkungen (Auto)";
//				viz.style = "topsheet";
//				viz.dataset = data.computeWithPlaceholder(ImpactAnalysis.class, "total_%s.csv", "car");
//				viz.enableFilter = false;
//				viz.showAllRows = true;
//				viz.width = 1d;
//			});
//
//		layout.row("freight")
//			.el(Table.class, (viz, data) -> {
//				viz.title = "Zentrale verkehrliche / physikalische Wirkungen (Güterverkehr)";
//				viz.style = "topsheet";
//				viz.dataset = data.computeWithPlaceholder(ImpactAnalysis.class, "total_%s.csv", "freight");
//				viz.enableFilter = false;
//				viz.showAllRows = true;
//				viz.width = 1d;
//			});
//
//		layout.row("pt")
//			.el(Table.class, (viz, data) -> {
//				viz.title = "Zentrale verkehrliche / physikalische Wirkungen (ÖPNV)";
//				viz.style = "topsheet";
//				viz.dataset = data.computeWithPlaceholder(ImpactAnalysis.class, "total_%s.csv", "pt");
//				viz.enableFilter = false;
//				viz.showAllRows = true;
//				viz.width = 1d;
//			});
//
//		layout.row("ride")
//			.el(Table.class, (viz, data) -> {
//				viz.title = "Zentrale verkehrliche / physikalische Wirkungen (Ride)";
//				viz.style = "topsheet";
//				viz.dataset = data.computeWithPlaceholder(ImpactAnalysis.class, "total_%s.csv", "ride");
//				viz.enableFilter = false;
//				viz.showAllRows = true;
//				viz.width = 1d;
//			});
//
//		layout.row("walk")
//			.el(Table.class, (viz, data) -> {
//				viz.title = "Zentrale verkehrliche / physikalische Wirkungen (Zu Fuß)";
//				viz.style = "topsheet";
//				viz.dataset = data.computeWithPlaceholder(ImpactAnalysis.class, "total_%s.csv", "walk");
//				viz.enableFilter = false;
//				viz.showAllRows = true;
//				viz.width = 1d;
//			});

//		layout.row("time")
//			.el(Table.class, (viz, data) -> {
//				viz.title = "Travel Time";
//				viz.style = "topsheet";
//				viz.dataset = data.compute(ImpactAnalysis.class, "travel_time.csv");
//				viz.enableFilter = false;
//				viz.showAllRows = true;
//				viz.width = 1d;
//			})
//			.el(Table.class, (viz, data) -> {
//				viz.title = "Travel and Waiting Time";
//				viz.style = "topsheet";
//				viz.dataset = data.compute(ImpactAnalysis.class, "travel_and_waiting_time.csv");
//				viz.enableFilter = false;
//				viz.showAllRows = true;
//				viz.width = 1d;
//			});
//
//		layout.row("distance")
//			.el(Table.class, (viz, data) -> {
//				viz.title = "Distance per Mode";
//				viz.style = "topsheet";
//				viz.dataset = data.compute(ImpactAnalysis.class, "traveled_distance.csv");
//				viz.enableFilter = false;
//				viz.showAllRows = true;
//				viz.width = 1d;
//			});
//
//		layout.row("emissions")
//			.el(Table.class, (viz, data) -> {
//				viz.title = "Emissions per Network Mode";
//				viz.style = "topsheet";
//				viz.dataset = "analysis/emissions/emissions_per_network_mode.csv";
//				viz.enableFilter = false;
//				viz.showAllRows = true;
//				viz.width = 1d;
//			})
//			.el(Table.class, (viz, data) -> {
//				viz.title = "Emissions per Vehicle Type";
//				viz.style = "topsheet";
//				viz.dataset = "analysis/emissions/emissions_per_vehicle_type.csv";
//				viz.enableFilter = false;
//				viz.showAllRows = true;
//				viz.width = 1d;
//			});
	}

}
