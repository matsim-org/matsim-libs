package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.LogFileAnalysis;
import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.*;

import java.util.List;

/**
 * Standard dashboard for the carrier viewer.
 */
public class CarrierDashboard implements Dashboard  {

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Carrier Viewer";
		header.tab = "";

		layout.row("KPI", "Carrier Overview").el(Tile.class, (viz, data) -> {
			viz.title = "Carrier KPIs";
			viz.dataset = data.output("analysis/freight/Carriers_KPIs.tsv");
			viz.description = "Key Performance Indicators of the Carrier";
		});
		layout.row("first", "Carrier Overview").el(Table.class, (viz, data) -> {
			viz.title = "Run Info for MATSim";
			viz.showAllRows = true;
			viz.dataset = data.compute(LogFileAnalysis.class, "run_info.csv");
			viz.width = 1.d;
			viz.height = 6.0;
		});
		layout.row("first", "Carrier Overview").el(Bar.class, (viz, data) -> {
			viz.title = "Resulting Fleet";
			viz.dataset = "analysis/freight/TimeDistance_perVehicleType.tsv";
			viz.description = "Number of Vehicles per Vehicle Type";
			viz.columns = List.of("nuOfVehicles");
			viz.x = "vehicleTypeId";
			viz.xAxisName = "Vehicle Type ID";
		});
		layout.row("Scores", "Carrier Overview")
			.el(Line.class, (viz, data) -> {
				viz.title = "Development of the Jsprit Score";
				viz.dataset = data.output("analysis/freight/VRP_Solution_Stats.csv");
				viz.description = "per Iteration";
				viz.x = "jsprit_iteration";
				viz.columns = List.of("sumJspritScores");
				viz.xAxisName = "Jsprit Iteration";
				viz.yAxisName = "Jsprit Score";
			})
			.el(Line.class, (viz, data) -> {
				viz.title = "Running Carrier in this Jsprit Iteration";
				viz.dataset = data.output("analysis/freight/VRP_Solution_Stats.csv");
				viz.description = "per Iteration";
				viz.x = "jsprit_iteration";
				viz.columns = List.of("runCarrier");
				viz.xAxisName = "Jsprit Iteration";
				viz.yAxisName = "Carrier within Jsprit Iteration";
			})
			.el(Plotly.class, (viz, data) -> {
				viz.title = "Number of Vehicles in best Solution per Jsprit Iteration";
				viz.description = "per Iteration";
				Plotly.DataSet ds = viz.addDataset("analysis/freight/VRP_Solution_Stats_perCarrier.csv")
					.aggregate(List.of("jsprit_iteration"), "currentBestSolutionNumberOfVehicles", Plotly.AggrFunc.SUM);
				viz.addTrace(
					tech.tablesaw.plotly.traces.ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
						.mode(tech.tablesaw.plotly.traces.ScatterTrace.Mode.LINE)
						.build(),
					ds.mapping()
						.x("jsprit_iteration")
						.y("currentBestSolutionNumberOfVehicles")
				);
			});

		layout.row("viewer", "Tour Visualizer").el(CarrierViewer.class, (viz, data) -> {
			viz.title = "Carrier Viewer";
			viz.height = 12d;
			viz.description = "Visualize the carrier's routes";

			// Include a network that has not been filtered
			viz.network = data.withContext("all").compute(CreateAvroNetwork.class, "network.avro",
				"--mode-filter", "", "--shp", "none");

			viz.carriers = data.output("(*.)?output_carriers.xml.gz");
		});

		layout.row("veh-dist-violin", "Tour Analysis").el(Plotly.class, (viz, data) -> {
			viz.title = "Distance per Tour [km]";
			viz.description = "different by vehicle types";

			viz.colorRamp = ColorScheme.Viridis;

			Plotly.DataSet ds = viz.addDataset("analysis/freight/TimeDistance_perVehicle.tsv");

			viz.addTrace(
				tech.tablesaw.plotly.traces.ViolinTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.build(),
				ds.mapping()
					.x("vehicleTypeId")
					.y("travelDistance[km]")
			);
		});

		layout.row("veh-duration-violin", "Tour Analysis").el(Plotly.class, (viz, data) -> {
			viz.title = "Duration per Tour [km]";
			viz.description = "different by vehicle types";

			viz.colorRamp = ColorScheme.Viridis;

			Plotly.DataSet ds = viz.addDataset("analysis/freight/TimeDistance_perVehicle.tsv");

			viz.addTrace(
				tech.tablesaw.plotly.traces.ViolinTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.build(),
				ds.mapping()
					.x("vehicleTypeId")
					.y("tourDuration[h]")
			);
		});

		layout.row("veh-costs-violin", "Tour Analysis").el(Plotly.class, (viz, data) -> {
			viz.title = "Costs per Tour [km]";
			viz.description = "different by vehicle types";

			viz.colorRamp = ColorScheme.Viridis;

			Plotly.DataSet ds = viz.addDataset("analysis/freight/TimeDistance_perVehicle.tsv");

			viz.addTrace(
				tech.tablesaw.plotly.traces.ViolinTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.build(),
				ds.mapping()
					.x("vehicleTypeId")
					.y("totalCosts[EUR]")
			);
		});

		layout.row("VehicleCostOverview", "Vehicle Specifications").el(Table.class, (viz, data) -> {
			viz.title = "Vehicle Cost Overview per Vehicle Type";
			viz.description = "Overview of the cost parameters per vehicle type.";
			viz.dataset = "analysis/freight/TimeDistance_perVehicleType.tsv";
			viz.showAllRows = true;
			viz.show = List.of(
				"vehicleTypeId",
				"costPerSecond[EUR/s]",
				"costPerMeter[EUR/m]",
				"fixedCosts[EUR/veh]"
			);
		});
		layout.row("VehicleCostOverview", "Vehicle Specifications").el(Table.class, (viz, data) -> {
			viz.title = "Result: Total Vehicle Cost per Vehicle Type";
			viz.description = "Based on the VRP solution, the total costs per vehicle type are calculated.";
			viz.dataset = "analysis/freight/TimeDistance_perVehicleType.tsv";
			viz.showAllRows = true;
			viz.show = List.of(
				"vehicleTypeId",
				"varCostsTime[EUR]",
				"varCostsDist[EUR]",
				"fixedCosts[EUR]",
				"totalCosts[EUR]"
			);
		});
	}
}
