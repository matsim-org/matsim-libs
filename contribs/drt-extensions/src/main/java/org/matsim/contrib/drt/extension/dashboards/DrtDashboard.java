package org.matsim.contrib.drt.extension.dashboards;


import org.matsim.application.analysis.population.TripAnalysis;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.config.ConfigGroup;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Data;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.*;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.traces.BarTrace;
import tech.tablesaw.plotly.traces.ScatterTrace;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * General DRT dashboard for one specific service.
 */
public class DrtDashboard implements Dashboard {

	private final String crs;
	private final URL matsimConfigContext;

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
				viz.title = drtConfigGroup.mode + "service results";
				viz.description = "final configuration and KPI. note that values are rounded";
				viz.dataset = postProcess(data, "supply_kpi.csv");
				viz.style = "topsheet";
				viz.showAllRows = true;
				viz.width = 1.;
			})
			.el(Table.class, (viz, data) -> {
				viz.title = drtConfigGroup.mode + " demand results";
				viz.description = "final demand statistics and KPI";
				viz.dataset = postProcess(data, "demand_kpi.csv");
				viz.style = "topsheet";
				viz.showAllRows = true;
				viz.width = 1.;
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
				viz.width = 2.;
			});

		//TODO: discuss: should we make XYTime plots out of those ??
		layout.row("Spatial demand distribution")
			.el(Hexagons.class, (viz, data) -> {
				viz.title = drtConfigGroup.mode + " spatial demand distribution";
				viz.description = "Origins and destinations.";
				viz.projection = this.crs;
				viz.file = data.output("*output_drt_legs_" + drtConfigGroup.mode + ".csv");
				viz.addAggregation("OD", "origins", "fromX", "fromY", "destinations", "toX", "toY");

				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
			})
			.el(Hexagons.class, (viz, data) -> {
				viz.title = drtConfigGroup.mode + " spatial rejection distribution";
				viz.description = "Requested (and rejected) origins and destinations.";
				viz.projection = this.crs;
				viz.file = data.output("ITERS/it." + lastIteration + "/*rejections_" +  drtConfigGroup.mode + ".csv");
				viz.addAggregation("rejections", "origins", "fromX", "fromY", "destinations", "toX", "toY");

				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
			})
			//TODO group rejections per time bin. better put it into the plot with wait stats over daty time.
//			.el(Line.class, (viz, data) -> {
//				viz.dataset = data.output("ITERS/it." + lastIteration + "/*rejections_" +  drtConfigGroup.mode + ".csv");
//				viz.x = "time";
//			})
		;

		//Final Demand stats
		layout.row("Final Demand And Wait Time Statistics")
			.el(Plotly.class, (viz, data) -> {
				viz.title = "Final Demand and Wait Stats over day time";
				viz.description = "Nr of rides (customers) is displayed in bars, wait statistics in lines";

				Plotly.DataSet dataset = viz.addDataset(data.output("*_waitStats_" + drtConfigGroup.mode + ".csv"));

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.xAxis(Axis.builder().title("Time Bin").build())
					.yAxis(Axis.builder().title("Wait Time [s]").build())
					.yAxis2(Axis.builder().title("Nr of Rides")
						.side(Axis.Side.right)
						.overlaying(ScatterTrace.YAxis.Y)
						.build())
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
					.build();

				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.mode(ScatterTrace.Mode.LINE)
					.name("Average")
					.build(),
					dataset.mapping()
						.x("timebin")
						.y("average_wait")
				);

				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
						.mode(ScatterTrace.Mode.LINE)
						.name("P5")
						.build(),
					dataset.mapping()
						.x("timebin")
						.y("p_5")
				);

				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
						.mode(ScatterTrace.Mode.LINE)
						.name("P95")
						.build(),
					dataset.mapping()
						.x("timebin")
						.y("p_95")
				);

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT)
						.opacity(0.3)
						.yAxis(ScatterTrace.YAxis.Y2.toString())
						.name("Rides")
						.build(),
					dataset.mapping()
						.x("timebin")
						.y("legs")
						.color(Plotly.ColorScheme.Cividis)
				);

			})
			.el(Area.class, (viz, data) -> {
				viz.title = "Vehicle Occupancy"; //actually, without title the area plot won't work
				viz.description = "Number of passengers on board at a time";
				viz.dataset = data.output("ITERS/it." + lastIteration + "/*occupancy_time_profiles_" +  drtConfigGroup.mode + ".txt");
				viz.x = "time";
				viz.xAxisName = "Time";
				viz.yAxisName = "Vehicles [1]";
			});


		//from here on, we show the 'evolution' of statistics over iterations
		//based on CL's feedback this is not helpful for low iteration numbers
		//this implementation actually does not account for situations where firstIteration > 0.
		if(lastIteration >= 3){

			//Demand stats over iterations
			layout.row("'Evolution' of Demand And Wait Time Statistics Over Iterations")
				.el(Plotly.class, (viz, data) -> {
					viz.title = "'Evolution' of Demand And Wait Time Statistics Over Iterations\"";
					viz.description = "Nr of Rides (customers) is displayed in bars, wait statistics in lines";

					Plotly.DataSet dataset = viz.addDataset(data.output("*_customer_stats_" + drtConfigGroup.mode + ".csv"));

					viz.layout = tech.tablesaw.plotly.components.Layout.builder()
						.xAxis(Axis.builder().title("Iteration").build())
						.yAxis(Axis.builder().title("Wait Time [s]").build())
						.yAxis2(Axis.builder().title("Nr of Rides")
							.side(Axis.Side.right)
							.overlaying(ScatterTrace.YAxis.Y)
							.build())
						.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
						.build();

					viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
							.mode(ScatterTrace.Mode.LINE)
							.name("Average")
							.build(),
						dataset.mapping()
							.x("iteration")
							.y("wait_average")
					);

					viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
							.mode(ScatterTrace.Mode.LINE)
							.name("Median")
							.build(),
						dataset.mapping()
							.x("iteration")
							.y("wait_median")
					);

					viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
							.mode(ScatterTrace.Mode.LINE)
							.name("P95")
							.build(),
						dataset.mapping()
							.x("iteration")
							.y("wait_p95")
					);

					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT)
							.opacity(0.5)
							.yAxis(ScatterTrace.YAxis.Y2.toString())
							.name("Rejections")
							.build(),
						dataset.mapping()
							.x("iteration")
							.y("rejections")
//						.color(Plotly.ColorScheme.RdBu)
					);

					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT)
							.opacity(0.5)
							.yAxis(ScatterTrace.YAxis.Y2.toString())
							.name("Rides")
							.build(),
						dataset.mapping()
							.x("iteration")
							.y("rides")
//						.color(Plotly.ColorScheme.RdBu)
					);

				})
				.el(Line.class, (viz, data) -> {
					viz.title = "Travel Distance";
					viz.dataset = data.output("*customer_stats_" + drtConfigGroup.mode + ".csv");
					viz.description = "traveled distance versus direct distance";
					viz.x = "iteration";
					viz.columns = List.of("directDistance_m_mean", "distance_m_mean");
					viz.xAxisName = "Iteration";
					viz.yAxisName = "distance [m]";
				});

			layout.row("'Evolution' of Demand And Travel Time Statistics Over Iterations")
				.el(Plotly.class, (viz, data) -> {
					viz.title = "'Evolution' of Demand And Travel Time Components\"";
					viz.description = "Nr of rides (customers) is displayed as a line, travel time components as bars";

					Plotly.DataSet dataset = viz.addDataset(data.output("*_customer_stats_" + drtConfigGroup.mode + ".csv"));

					viz.layout = tech.tablesaw.plotly.components.Layout.builder()
						.xAxis(Axis.builder().title("Iteration").build())
						.yAxis(Axis.builder().title("Average value of travel time component [s]")
//					.overlaying(ScatterTrace.YAxis.Y2)
							.build())
						.yAxis2(Axis.builder().title("Nr of rides")
							.side(Axis.Side.right)
							.overlaying(ScatterTrace.YAxis.Y)
							.build())
						.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
						.build();

					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT)
							.opacity(0.5)
							.name("In Vehicle")
							.build(),
						dataset.mapping()
							.x("iteration")
							.y("inVehicleTravelTime_mean")
//						.color(Plotly.ColorScheme.RdBu)
					);

					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT)
							.opacity(0.5)
							.name("Wait")
							.build(),
						dataset.mapping()
							.x("iteration")
							.y("wait_average")
//						.color(Plotly.ColorScheme.RdBu)
					);

					viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
							.mode(ScatterTrace.Mode.LINE)
							.yAxis(ScatterTrace.YAxis.Y2.toString())
							.name("Rides")
							.build(),
						dataset.mapping()
							.x("iteration")
							.y("rides")
					);

				});


			//Evolution of fleet stats
			layout.row("'Evolution' of Fleet Stats Over Iterations")
				.el(Plotly.class, (viz, data) -> {
					viz.title = "'Evolution' of Fleet Stats";
					viz.description = "Nr of " + drtConfigGroup.mode + " vehicles (customers) is displayed as bars, distance stats as lines";

					Plotly.DataSet dataset = viz.addDataset(data.output("*_vehicle_stats_" + drtConfigGroup.mode + ".csv"));

					viz.layout = tech.tablesaw.plotly.components.Layout.builder()
						.xAxis(Axis.builder().title("Iteration").build())
						.yAxis(Axis.builder().title("Total Distance [m]")
//					.overlaying(ScatterTrace.YAxis.Y2)
							.build())
						.yAxis2(Axis.builder().title("Nr Of Vehicles")
							.side(Axis.Side.right)
							.overlaying(ScatterTrace.YAxis.Y)
							.build())
						.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
						.build();

					viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
							.mode(ScatterTrace.Mode.LINE)
							.name("Passeng. Dist.")
							.build(),
						dataset.mapping()
							.x("iteration")
							.y("totalPassengerDistanceTraveled")
					);

					viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
							.mode(ScatterTrace.Mode.LINE)
							.name("Vehicle mileage")
							.build(),
						dataset.mapping()
							.x("iteration")
							.y("totalDistance")
					);

					viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
							.mode(ScatterTrace.Mode.LINE)
							.name("Empty mileage")
							.build(),
						dataset.mapping()
							.x("iteration")
							.y("totalEmptyDistance")
					);

					viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT)
							.opacity(0.2)
							.yAxis(ScatterTrace.YAxis.Y2.toString())
							.name("Vehicles")
							.build(),
						dataset.mapping()
							.x("iteration")
							.y("vehicles")
//						.color(Plotly.ColorScheme.RdBu)
					);
				})
				.el(Line.class, (viz, data) -> {
					viz.title = "Relative Statistics";
					viz.dataset = data.output("*vehicle_stats_" + drtConfigGroup.mode + ".csv");
					viz.description = "per Iteration";
					viz.x = "iteration";
					viz.columns = List.of("d_p/d_t", "l_det", "emptyRatio");
					viz.xAxisName = "Iteration";
					viz.yAxisName = "Value";
				});
		}





	}
}
