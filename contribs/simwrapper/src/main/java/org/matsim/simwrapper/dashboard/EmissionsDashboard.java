package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.emissions.AirPollutionAnalysis;
import org.matsim.application.prepare.network.CreateGeoJsonNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.GridMap;
import org.matsim.simwrapper.viz.Links;
import org.matsim.simwrapper.viz.Table;

/**
 * Shows emission in the scenario.
 */
public class EmissionsDashboard implements Dashboard {
	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Emissions";
		header.description = "Shows the emissions footprint and spatial distribution.";


		layout.row("links")
			.el(Table.class, (viz, data) -> {

				viz.title = "Emissions";
				viz.description = "by pollutant";
				viz.dataset = data.compute(AirPollutionAnalysis.class, "emissions_total.csv");
				viz.enableFilter = false;
				viz.showAllRows = true;

				viz.width = 1d;

			})
			.el(Links.class, (viz, data) -> {
				viz.title = "Emissions per Link per Meter";
				viz.description = "Displays the emissions for each link per meter.";
				viz.height = 12.;
				viz.datasets.csvFile = data.compute(AirPollutionAnalysis.class, "emissions_per_link_per_m.csv");
				viz.network = data.compute(CreateGeoJsonNetwork.class, "network.geojson");
				viz.display.color.columnName = "CO2_TOTAL [g/m]";
				viz.display.color.dataset = "csvFile";
				viz.display.width.scaleFactor = 1;
				viz.display.width.columnName = "CO2_TOTAL [g/m]";
				viz.display.width.dataset = "csvFile";

				viz.center = data.context().getCenter();
				viz.width = 3d;
			});

		layout.row("second")
			.el(GridMap.class, (viz, data) -> {
				viz.title = "CO₂ Emissions";
				viz.description = "per day";
				viz.height = 12.;
				viz.cellSize = 100;
				viz.opacity = 0.2;
				viz.maxHeight = 100;
				viz.projection = "EPSG:25832";
				viz.zoom = data.context().mapZoomLevel;
				viz.center = data.context().getCenter();

				viz.setColorRamp("greenRed", 10, false);
				viz.file = data.compute(AirPollutionAnalysis.class, "emissions_grid_per_day.csv");
			});

		layout.row("third")
			.el(GridMap.class, (viz, data) -> {
				viz.title = "CO₂ Emissions";
				viz.description = "per hour";
				viz.height = 12.;
				viz.cellSize = 100;
				viz.opacity = 0.2;
				viz.maxHeight = 100;
				viz.projection = "EPSG:25832";
				viz.zoom = data.context().mapZoomLevel;
				viz.center = data.context().getCenter();

				viz.setColorRamp("greenRed", 10, false);
				viz.file = data.compute(AirPollutionAnalysis.class, "emissions_grid_per_hour.csv");
			});


		// TODO: emissions by vehicle type

	}
}
