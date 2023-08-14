package org.matsim.contrib.drt.extension.dashboards;


import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.config.ConfigGroup;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Data;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * General DRT dashboard for one specific service.
 */
public class DrtDashboard implements Dashboard {

//	private final String mode;
	private final String crs;
	private final URL matsimConfigContext;
//	private final URL transitStopFile;
//	private final URL serviceAreaShapeFile;

	private final DrtConfigGroup drtConfigGroup;
	private final int lastIteration;

	public DrtDashboard(DrtConfigGroup drtConfigGroup, URL matsimConfigContext, String crs, int lastIteration){
		this.drtConfigGroup = drtConfigGroup;
		this.matsimConfigContext = matsimConfigContext;
		this.crs = crs;
		this.lastIteration = lastIteration;
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
		return drtConfigGroup.getMode().replace("drt", "").replace("Drt", "");
	}

	private String postProcess(Data data, String file) {
		List<String> args = new ArrayList<>(List.of("--drt-mode", drtConfigGroup.getMode(), "--input-crs", crs));

		//it might be, that a serviceArea file is provided in the config, but the drt service is configured to be stopbased, nevertheless
		switch (drtConfigGroup.operationalScheme) {
			case stopbased -> {
//				drtConfigGroup.transitStopFile can not be null, otherwise simulation crashed, earlier
				args.addAll(List.of("--stops-file", ConfigGroup.getInputFileURL(matsimConfigContext, drtConfigGroup.transitStopFile).toString()));
			}
			case door2door -> {
				//TODO potentially show the entire drt network (all drt links have stops)
			}
			case serviceAreaBased -> {
//				drtConfigGroup.drtServiceAreaShapeFile can not be null, otherwise simulation crashed, earlier
				//this copies the input shape file into the output directoy. might not be ideal. but the input flle might be anywhere (web, different local partition, ...) and simwrapper might not have access....
				args.addAll(List.of("--area-file", ConfigGroup.getInputFileURL(matsimConfigContext, drtConfigGroup.drtServiceAreaShapeFile).toString()));
			}
		}

		return data.compute(DrtAnalysisPostProcessing.class, file, args.toArray(new String[0]));
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = getTitle();

		layout.row("Results")
			.el(Table.class, (viz, data) -> {
				viz.title = drtConfigGroup.mode + " supply statistics";
				viz.description = "from last iteration";
				//TODO
				viz.dataset = postProcess(data, "supply_kpi.csv");
				viz.style = "topsheet";
				viz.showAllRows = true;
			})
			.el(MapPlot.class, (viz, data) -> {
				switch (drtConfigGroup.operationalScheme){
					case stopbased -> {
						viz.title = "Map of stops";
						viz.setShape(postProcess(data, "stops.shp"), "id");
					}
					case door2door -> {
						//TODO add drtNetwork !?
					}
					case serviceAreaBased -> {
						viz.title = "Service area";
						viz.setShape(postProcess(data, "serviceArea.shp"), "something");
					}
				}
				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
			});
		layout.row("Vehicles")
			.el(Line.class, (viz, data) -> {
				viz.title = "Fleet size";
				viz.dataset = data.output("*vehicle_stats_" + drtConfigGroup.mode + ".csv");
				viz.description = "per Iteration";
				viz.x = "iteration";
				viz.columns = List.of("vehicles");
				viz.xAxisName = "Iteration";
				viz.yAxisName = "Nr of vehicles";
			});
		layout.row("travel time") //TODO: will be put in another dashboard later
			.el(Line.class, (viz, data) -> {
				viz.title = "Travel Time stats - WILL GET MOVED TO OTHER DASHBOARD";
				viz.dataset = data.output("*customer_stats_" + drtConfigGroup.mode + ".csv");
				viz.description = "per Iteration";
				viz.x = "iteration";
				viz.columns = List.of("totalTravelTime_mean", "inVehicleTravelTime_mean", "wait_average");
				viz.xAxisName = "Iteration";
				viz.yAxisName = "Time [s]";
			});
		layout.row("rides") //TODO: will be put in another dashboard later
			.el(Line.class, (viz, data) -> {
				viz.title = "Rides - WILL GET MOVED TO OTHER DASHBOARD";
				viz.dataset = data.output("*customer_stats_" + drtConfigGroup.mode + ".csv");
				viz.description = "per Iteration";
				viz.x = "iteration";
				viz.columns = List.of("rides");
				viz.xAxisName = "Iteration";
				viz.yAxisName = "rides [1]";
			});
		layout.row("Vehicle occupancy")
			.el(Area.class, (viz, data) -> {
				viz.title = "Vehicle Occupancy"; //actually, without title the area plot won't work
				viz.description = "WILL BE MOVED TO OTHER DASHBOARD";
				viz.dataset = data.output("ITERS/it." + lastIteration + "/*occupancy_time_profiles_" +  drtConfigGroup.mode + ".txt");
				viz.x = "time";
				viz.xAxisName = "Time";
				viz.yAxisName = "Vehicles [1]";
		});
		layout.row("Spatial demand distribution")
			.el(Hexagons.class, ((viz, data) -> {
				viz.title = drtConfigGroup.mode + " spatial demand distribution";
				viz.description = "Origins and destinations. --- will be put into other dashboard, as well ---";
				viz.projection = this.crs;
				viz.file = data.output("*output_drt_legs_" + drtConfigGroup.mode + ".csv");
				viz.addAggregation("OD", "origins", "fromX", "fromY", "destinations", "toX", "toY");

				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
			}));

	}
}
