package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.emissions.AirPollutionAnalysis;
import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.DashboardUtils;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.GridMap;
import org.matsim.simwrapper.viz.MapPlot;
import org.matsim.simwrapper.viz.Table;

/**
 * Shows emission in the scenario.
 */
public class EmissionsDashboard implements Dashboard {

	private final String coordinateSystem;

	/**
	 * Best provide the crs from {@link org.matsim.core.config.groups.GlobalConfigGroup}
	 * @param coordinateSystem
	 */
	public EmissionsDashboard(String coordinateSystem) {
		this.coordinateSystem = coordinateSystem;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Air Pollution";
		header.description = "Shows the air pollution footprint and its spatial distribution. Shown values are already upscaled from simulated sample size.";

		layout.row("links")
			.el(Table.class, (viz, data) -> {
				viz.title = "Total Emission";
				viz.description = "by pollutant";
				viz.dataset = data.compute(AirPollutionAnalysis.class, "emissions_total.csv");
				viz.enableFilter = false;
				viz.showAllRows = true;
				viz.width = 1d;

			})
			.el(MapPlot.class, (viz, data) -> {
				viz.title = "Emission per Link per Meter";
				viz.description = "Displays the emissions for each link per meter.";
				viz.height = 12.;
				viz.addDataset("emissions_per_link_per_m", data.compute(AirPollutionAnalysis.class, "emissions_per_link_per_m.csv"));
				viz.setShape(data.compute(CreateAvroNetwork.class, "network.avro", "--with-properties"), "linkId");
				viz.display.lineColor.columnName = "CO2_TOTAL [g/m]";
				viz.display.lineColor.dataset = "emissions_per_link_per_m";
				viz.display.lineColor.setColorRamp("greenRed", 10, false);
				viz.display.lineColor.join = "linkId";
				viz.display.lineWidth.columnName = "CO2_TOTAL [g/m]";
				viz.display.lineWidth.scaleFactor = 2000.0;
				viz.display.lineWidth.dataset = "emissions_per_link_per_m";
				viz.display.lineWidth.join = "linkId";
				viz.center = data.context().getCenter();
				viz.width = 3d;
			});

		layout.row("second")
			.el(GridMap.class, (viz, data) -> {
				viz.title = "CO₂ Emissions";
				viz.unit = "CO₂ [g]";
				viz.description = "per day";
				DashboardUtils.setGridMapStandards(viz, data, this.coordinateSystem);
				viz.file = data.computeWithPlaceholder(AirPollutionAnalysis.class, "emissions_grid_per_day.%s", "avro");
			});

		layout.row("third")
			.el(GridMap.class, (viz, data) -> {
				viz.title = "CO₂ Emissions";
				viz.unit = "CO₂ [g]";
				viz.description = "per hour";
				DashboardUtils.setGridMapStandards(viz, data, this.coordinateSystem);
				viz.file = data.computeWithPlaceholder(AirPollutionAnalysis.class, "emissions_grid_per_hour.%s", "avro");
			});


		// TODO: emissions by vehicle type

	}
}
