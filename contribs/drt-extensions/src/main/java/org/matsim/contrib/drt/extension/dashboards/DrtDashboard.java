package org.matsim.contrib.drt.extension.dashboards;


import org.matsim.core.utils.io.IOUtils;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Data;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.CalculationTable;
import org.matsim.simwrapper.viz.Line;
import org.matsim.simwrapper.viz.MapPlot;
import org.matsim.simwrapper.viz.Table;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * General DRT dashboard for one specific service.
 */
public class DrtDashboard implements Dashboard {

	private final String mode;
	private final String crs;
	private final URL transitStopFile;
	private final URL serviceAreaShapeFile;

	public DrtDashboard(String mode, String crs, URL transitStopFile, URL serviceAreaShapeFile) {
		this.mode = mode;
		this.crs = crs;
		this.transitStopFile = transitStopFile;
		this.serviceAreaShapeFile = serviceAreaShapeFile;
	}

	/**
	 * Auto generate title.
	 */
	public String getTitle() {
		String name = context();
		return name.isBlank() ? "DRT" : "DRT - " + name;
	}

	@Override
	public String context() {
		return mode.replace("drt", "").replace("Drt", "");
	}

	private String postProcess(Data data, String file) {
		List<String> args = new ArrayList<>(List.of("--drt-mode", mode, "--input-crs", crs));

		if (transitStopFile != null) {
			args.addAll(List.of("--stop-file", transitStopFile.toString()));
		}

		return data.compute(DrtAnalysisPostProcessing.class, file, args.toArray(new String[0]));
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = getTitle();

		layout.row("overview")
//			.el(CalculationTable.class, (viz, data) -> {
//				viz.title = mode + " supply stats";
//				//TODO
//				viz.configFile = data.resource("table-drt-supply.yaml");
////				viz.dataset = postProcess(data, "kpi.csv");
////				viz.showAllRows = true;
//			})
			.el(MapPlot.class, (viz, data) -> {

				if (transitStopFile != null) {
					viz.title = "Map of stops";
					viz.setShape(postProcess(data, "stops.shp"), "id");
				} else if (serviceAreaShapeFile != null){
					viz.title = "Service area";

					//TODO this does not work. we either need to copy the service area shp file to the output or mb need to set the path relatively to the output dir ??
//					viz.setShape(data.resource(serviceAreaShapeFile.getFile()), "");
					viz.setShape(serviceAreaShapeFile.getPath(), "");
				}


			});
		layout.row("Vehicles")
			.el(Table.class, ((viz, data) -> {
					viz.dataset = data.output("*vehicle_stats_" + mode + ".csv");
					viz.enableFilter = true;
					viz.show = List.of("totalDistance", "emptyRatio", "totalPassengerDistanceTraveled");
			}))
			.el(Line.class, (viz, data) -> {
				viz.title = "Fleet size";
				viz.dataset = data.output("*vehicle_stats_" + mode + ".csv");
				viz.description = "per Iteration";
				viz.x = "iteration";
				viz.columns = List.of("vehicles");
				viz.xAxisName = "Iteration";
				viz.yAxisName = "Nr of vehicles";
			});


	}
}
