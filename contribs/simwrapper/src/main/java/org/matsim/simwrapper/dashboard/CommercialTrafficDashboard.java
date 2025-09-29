package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.commercialTraffic.CommercialAnalysis;
import org.matsim.application.analysis.population.TripAnalysis;
import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Links;
import org.matsim.simwrapper.viz.PieChart;
import org.matsim.simwrapper.viz.Plotly;
import org.matsim.simwrapper.viz.TextBlock;
import tech.tablesaw.plotly.traces.BarTrace;

import java.util.List;

/**
 * Dashboard to show commercial traffic statistics.
 */
public class CommercialTrafficDashboard implements Dashboard {

	@Override
	public void configure(Header header, Layout layout) {
		header.title = "Commercial Traffic";
		header.description = "Commercial Traffic related analyses.";

		addNotesBlock(layout, "General");

		layout.row("first","General").el(PieChart.class, (viz, data) -> {
				String[] args;
				double sampleSize = data.config().getSampleSize();
				if (data.context().getShp() != null) {
					args = new String[]{
						"--sampleSize", String.valueOf(sampleSize)
					};
				} else {
					args = new String[]{"--sampleSize", String.valueOf(sampleSize)};
				}
				viz.dataset = data.compute(CommercialAnalysis.class, "commercialTraffic_travelDistancesShares_perMode.csv", args);
				viz.title = "Travel Distance Shares by Mode";
				viz.description = "at final iteration";
				viz.useLastRow = true;
			})
			.el(PieChart.class, (viz, data) -> {
				viz.dataset = data.compute(CommercialAnalysis.class, "commercialTraffic_travelDistancesShares_perSubpopulation.csv");
				viz.title = "Travel Distance Shares by subpopulation";
				viz.useLastRow = true;
				viz.description = "at final iteration";

			})
			.el(PieChart.class, (viz, data) -> {
				viz.dataset = data.compute(CommercialAnalysis.class, "commercialTraffic_travelDistancesShares_perType.csv");
				viz.title = "Travel Distance Shares by model type";
				viz.useLastRow = true;
				viz.description = "at final iteration";

			});
		layout.row("second", "General").el(Links.class, (viz, data) -> {
			viz.title = "Link volumes of the commercial traffic";
			viz.datasets.csvFile = data.compute(CommercialAnalysis.class, "commercialTraffic_link_volume.csv");
			viz.network = data.compute(CreateAvroNetwork.class, "network.avro", "--with-properties"); //, "--match-id", "linkId", "--mode-filter", "none"
			viz.description = "The volumes can be filtered according to different types of traffic and vehicle types.";
			viz.height = 8.;
		});
		addNotesBlock(layout, "Trips");
		layout.row("trips", "Trips").el(Plotly.class, (viz, data) -> {
			viz.title = "Modal split";

			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
				.build();

			Plotly.DataSet ds = viz.addDataset(data.compute(TripAnalysis.class, "mode_share.csv", "--shp-filter", "none"))
				.constant("source", "Simulated")
				.aggregate(List.of("main_mode"), "share", Plotly.AggrFunc.SUM);

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).orientation(BarTrace.Orientation.HORIZONTAL).build(),
				ds.mapping()
					.name("main_mode")
					.y("source")
					.x("share")
			);
		});
	}

	private static void addNotesBlock(Layout layout, String tab) {
		layout.row(tab+"info", tab).el(TextBlock.class, (viz, data) -> {
			viz.backgroundColor = "transparent";
			viz.content = """
				### Notes
				This dashboard analyzes commercial traffic. The commercial traffic contains traffic from vehicles with a commercial purpose, e.g. freight transport, but also vehicles of the small-scale commercial traffic, e.g. service vehicles, care services, etc.
				The simulation results are with **sample size\s"""
				+data.config().getSampleSize()+"""
				**. If the sample size is < 1.0 the visualized results are **scale up to the 100% level**.
				<br><br>
				""";
		});
	}
}
