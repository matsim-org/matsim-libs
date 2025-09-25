package org.matsim.simwrapper.dashboard;

import jakarta.annotation.Nullable;
import org.matsim.application.analysis.commercialTraffic.CommercialAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.PieChart;
import org.matsim.simwrapper.viz.TextBlock;

/**
 * Dashboard to show commercial traffic statistics.
 */
public class CommercialTrafficDashboard implements Dashboard {


	private final String shpFile;

	/**
	 * Create a new activity dashboard using the given shape file.
	 *
	 * @param shpFile Path to the shape file.
	 */
	public CommercialTrafficDashboard(@Nullable String shpFile) {
		this.shpFile = shpFile;
	}

	@Override
	public void configure(Header header, Layout layout) {
		header.title = "Commercial Traffic";
		header.description = "Commercial Traffic related analyses.";

		layout.row("info").el(TextBlock.class, (viz, data) -> {
			viz.backgroundColor = "transparent";
			viz.content = """
				### Notes
				This dashboard analyzes commercial traffic. The commercial traffic contains traffic from vehicles with a commercial purpose, e.g. freight transport, but also vehicles of the small-scale commercial traffic, e.g. service vehicles, care services, etc.
				<br><br>
				""";
		});

		layout.row("first").el(PieChart.class, (viz, data) -> {
				String[] args = new String[]{
					"--shapeFileInvestigationArea", shpFile
				};
				viz.dataset = data.compute(CommercialAnalysis.class, "commercialTraffic_travelDistancesShares_perMode.csv", args);
				viz.title = "Travel Distance Shares by Mode";
				viz.useLastRow = true;
			})
			.el(PieChart.class, (viz, data) -> {
				;
				viz.dataset = data.compute(CommercialAnalysis.class, "commercialTraffic_travelDistancesShares_perSubpopulation.csv");
				viz.title = "Travel Distance Shares by subpopulation";
				viz.useLastRow = true;
			})
			.el(PieChart.class, (viz, data) -> {
				;
				viz.dataset = data.compute(CommercialAnalysis.class, "commercialTraffic_travelDistancesShares_perType.csv");
				viz.title = "Travel Distance Shares by model type";
				viz.useLastRow = true;
			});

	}
}
