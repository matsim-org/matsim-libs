package org.matsim.contrib.drt.extension.dashboards;

import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.AggregateOD;
import org.matsim.simwrapper.viz.Area;
import org.matsim.simwrapper.viz.Line;
import org.matsim.simwrapper.viz.Table;

import java.util.List;

/**
 * The simulation KEXI demand compared to real data
 */
public class DrtDemandDashboard implements Dashboard {
	@Override
	public void configure(Header header, Layout layout) {
		header.tab = "Demand";
		header.title = "Simulation demand compared to real data";
		header.description = "Only conventional KEXI vehicles are considered";

		layout.row("first")
				.el(Table.class,(viz, data) -> {
					viz.title = "Simulated demand statistics";
					viz.description = "only human-driven fleet";
					viz.width = 1d;
					viz.dataset = data.resource("topsheet-KEXI-base-demand.yaml");
				})
				.el(Table.class,(viz, data) -> {
					viz.title = "Real data demand statistics";
					viz.description = "Data from June '21 until Januar '22";
					viz.width = 1d;
					viz.dataset = data.resource("topsheet-KEXI-base-demand-real-data.yaml");
				});

		layout.row("second")
				.el(Line.class,(viz, data) -> {
					viz.title = "Simulated Demand Time Distribution";
					viz.description = "Requests/Rides over daytime";
					viz.width = 1d;
					viz.height = 12d;
					viz.dataset = data.resource("ITERS/it.999/*waitStats_drt.csv");
					viz.x = "timebin";
					viz.xAxisName = "Time";
					viz.yAxisName = "request per half an hour (req*2/h)";
					viz.columns = List.of("legs");
					viz.legendName = List.of("Ride requests");
				})
				.el(Area.class,(viz, data) -> {
					viz.title = "Real Demand Time Distribution";
					viz.description = "Aggregated Number of Rides per 5-Minute interval over the entire time span.";
					viz.width = 1d;
					viz.height = 12d;
					viz.dataset = data.resource("../../../VIA-Real-Data/KEXI_202106_202201_rides_daily_VIA.csv");
					viz.x = "ridesPerIntervals.interval5";
				});

		layout.row("third")
				.el(AggregateOD.class,(viz, data) -> {
					viz.title = "Simulation - Stop OD Flows";
					viz.description = "This depicts only one day";
					viz.width = 2d;
					viz.height =10d;
					viz.shpFile = "../../../VIA-Real-Data/KEXI_Haltestellen_Liste_Kelheim_utm32n_buffer10m.shp";
					viz.csvFile = "analysis-stop-2-stop/stop-2-stop-drt.csv";
					viz.dbfFile = "../../../VIA-Real-Data/KEXI_Haltestellen_Liste_Kelheim_utm32n_buffer10m.dbf";
					viz.projection = "EPSG:25832";
					viz.scaleFactor = 1d;
					viz.idColumn = "Id";
					viz.lineWidth = 500d;
				});

		layout.row("fourth")
				.el(AggregateOD.class,(viz, data) -> {
					viz.title = "Real Data - Stop OD Flows";
					viz.description = "This depicts a typical day in october 2021";
					viz.width = 2d;
					viz.height =10d;
					viz.shpFile = "../../../VIA-Real-Data/KEXI_Haltestellen_Liste_Kelheim_utm32n_buffer10m.shp";
					viz.csvFile = "../../../VIA-Real-Data/2021-10-12_Via-Origin-drt-count-Analysis-KEXI.csv";
					viz.dbfFile = "../../../VIA-Real-Data/KEXI_Haltestellen_Liste_Kelheim_utm32n_buffer10m.dbf";
					viz.projection = "EPSG:25832";
					viz.scaleFactor = 1d;
					viz.idColumn = "Id";
					viz.lineWidth = 500d;
				});
	}
}
