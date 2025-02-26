package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.noise.NoiseAnalysis;
import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.DashboardUtils;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.GridMap;
import org.matsim.simwrapper.viz.MapPlot;
import org.matsim.simwrapper.viz.TextBlock;
import org.matsim.simwrapper.viz.Tile;

/**
 * Shows emission in the scenario.
 */
public class NoiseDashboard implements Dashboard {

	private final String coordinateSystem;
	private final double minDb = 40;
	private final double maxDb = 80;

	/**
	 * Best provide the crs from {@link org.matsim.core.config.groups.GlobalConfigGroup}
	 *
	 * @param coordinateSystem for the {@link GridMap}
	 */
	public NoiseDashboard(String coordinateSystem) {
		this.coordinateSystem = coordinateSystem;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Noise";
		header.description = "Shows the noise footprint and spatial distribution.";

		layout.row("stats")
			.el(Tile.class, (viz, data) -> {
				viz.dataset = data.compute(NoiseAnalysis.class, "noise_stats.csv");
				viz.height = 0.1;
			});
		layout.row("emissions")
			.el(MapPlot.class, (viz, data) -> {
				viz.title = "Noise Assessment for the day/evening/night in dB(A)";
				viz.description = "Shows the noise assessment for the day/evening/night in dB(A) calculated by the CNOSSOS-EU and the RLS-16 method.";
				viz.height = 12.0;
				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
				viz.minValue = minDb;
				viz.maxValue = maxDb;
				viz.setShape(data.compute(CreateAvroNetwork.class, "network.avro", "--with-properties"), "id");
				viz.addDataset("noise", data.compute(NoiseAnalysis.class, "emissions.csv"));
				viz.display.lineColor.dataset = "noise";
				viz.display.lineColor.columnName = "L_DEN (dB(A))";
				viz.display.lineColor.join = "Link Id";
				viz.display.lineColor.fixedColors = new String[]{"#FFFFFF", "#E2F1BF", "#F3C683", "#CD463D", "#75075C", "#430A4A"};
				viz.display.lineColor.setColorRamp(6, "55, 60, 65, 70, 75");
				viz.display.lineWidth.dataset = "noise";
				viz.display.lineWidth.columnName = "L_DEN (dB(A))";
				viz.display.lineWidth.scaleFactor = 8d;
				viz.display.lineWidth.join = "Link Id";
			});
		layout.row("emissions")
			.el(MapPlot.class, (viz, data) -> {
				viz.title = "Noise Assessment for the night in dB(A)";
				viz.description = "Shows the noise assessment for the night in dB(A) calculated by the CNOSSOS-EU and the RLS-16 method.";
				viz.height = 12.0;
				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
				viz.minValue = minDb;
				viz.maxValue = maxDb;
				viz.setShape(data.compute(CreateAvroNetwork.class, "network.avro", "--with-properties"), "id");
				viz.addDataset("noise", data.compute(NoiseAnalysis.class, "emissions.csv"));
				viz.display.lineColor.dataset = "noise";
				viz.display.lineColor.columnName = "L_night (dB(A))";
				viz.display.lineColor.join = "Link Id";
				viz.display.lineColor.fixedColors = new String[]{"#FFFFFF", "#A0BABF", "#B8D6D1", "#E2F3BF", "#F3C683", "#CD463F", "#75075D"};
				viz.display.lineColor.setColorRamp(7, "45, 55, 60, 65, 70, 75");
				viz.display.lineWidth.dataset = "noise";
				viz.display.lineWidth.columnName = "L_night (dB(A))";
				viz.display.lineWidth.scaleFactor = 8d;
				viz.display.lineWidth.join = "Link Id";
			});
		layout.row("imissions")
			.el(GridMap.class, (viz, data) -> {
				viz.title = "Noise Immissions (Grid)";
				viz.description = "Total Noise Immissions per day";
				DashboardUtils.setGridMapStandards(viz, data, this.coordinateSystem);
				viz.file = data.computeWithPlaceholder(NoiseAnalysis.class, "immission_per_day.%s", "avro");
				viz.cellSize = 250;
				viz.setColorRamp(new double[]{55, 60, 65, 70, 75}, new String[]{"#FFFFFF", "#E2F1BF", "#F3C683", "#CD463D", "#75075C", "#430A4A"}
				);
			})
			.el(GridMap.class, (viz, data) -> {
				viz.title = "Hourly Noise Immissions (Grid)";
				viz.description = "Noise Immissions per hour";
				DashboardUtils.setGridMapStandards(viz, data, this.coordinateSystem);
				viz.file = data.computeWithPlaceholder(NoiseAnalysis.class, "immission_per_hour.%s", "avro");
				viz.cellSize = 250;
				viz.setColorRamp(new double[]{55, 60, 65, 70, 75}, new String[]{"#FFFFFF", "#E2F1BF", "#F3C683", "#CD463D", "#75075C", "#430A4A"}
				);
			});
		layout.row("damages")
			.el(GridMap.class, (viz, data) -> {
				viz.title = "Daily Noise Damages (Grid)";
				viz.description = "Total Noise Damages per day [€]";
				DashboardUtils.setGridMapStandards(viz, data, this.coordinateSystem);
				viz.file = data.computeWithPlaceholder(NoiseAnalysis.class, "damages_receiverPoint_per_day.%s", "avro");
				viz.cellSize = 250;
				viz.setColorRamp(new double[]{55, 60, 65, 70, 75}, new String[]{"#FFFFFF", "#E2F1BF", "#F3C683", "#CD463D", "#75075C", "#430A4A"}
				);
			})
			.el(GridMap.class, (viz, data) -> {
				viz.title = "Hourly Noise Damages (Grid)";
				viz.description = "Noise Damages per hour [€]";
				DashboardUtils.setGridMapStandards(viz, data, this.coordinateSystem);
				viz.file = data.computeWithPlaceholder(NoiseAnalysis.class, "damages_receiverPoint_per_hour.%s", "avro");
				viz.cellSize = 250;
				viz.setColorRamp(new double[]{55, 60, 65, 70, 75}, new String[]{"#FFFFFF", "#E2F1BF", "#F3C683", "#CD463D", "#75075C", "#430A4A"}
				);
			});


		layout.row("disclaimer")
			.el(TextBlock.class, (viz, data) -> {
				viz.content = """
					# Disclaimer

					The sound level values per hour are calculated using the noise analysis ([noise-contrib](https://github.com/matsim-org/matsim-libs/tree/master/contribs/noise)) [1, 2, 3]. The night-time sound level ($L_{night}$) and the calculated total level ${L_{DEN}}$ are shown, which is derived from the values $L_{day}$, $L_{evening}$ and $L_{night}$.

					The calculation of ${L_{DEN}}$ is based on the following formula:

					$$
					L_{DEN} = 10 \\cdot \\log_{10}\\left(\\frac{12 \\cdot 10^{L_{day}/10} + 4 \\cdot 10^{(L_{evening}+5)/10} + 8 \\cdot 10^{(L_{night}+10)/10}}{24}\\right)
					$$

					The averaging levels $L_{day}$, $L_{evening}$ and $L_{night}$ are calculated using the formula

					$$
					L_{avg} = 10 \\cdot \\log_{10}\\left(\\frac{1}{n} \\sum 10^{L/10}\\right)
					$$

					where $n$ is the number of measured values and $L$ is the respective sound level. The following applies to the individual day sections

					- **day ($L_{day}$)**: Period from 06:00 to 18:00
					- **Evening ($L_{evening}$)**: Period from 18:00 to 22:00
					- **Night ($L_{night}$)**: Period from 22:00 to 06:00

					These indicators are based on [DIRECTIVE 2002/49/EC](https://eur-lex.europa.eu/eli/dir/2002/49/oj/eng) of the European Parliament and of the Council of June 25, 2002 relating to the assessment and management of environmental noise. The method described in DIN 45641 is used to determine the averaging level, as also described in the Wikipedia explanation of **averaging level** (as of: 19.02.2025).

					[1] I. Kaddoura, L. Kroeger, and K. Nagel. User-specific and dynamic internalization of road traffic noise exposures. Networks and Spatial Economics, 2016. DOI: 10.1007/s11067-016-9321-2. Preprint available [ here](https://svn.vsp.tu-berlin.de/repos/public-svn/publications/vspwp/2015/15-12/). \s
					[2] I. Kaddoura and K. Nagel. Activity-based computation of marginal noise exposure costs: Implications for traffic management. Transportation Research Record 2597, 2016. DOI: 10.3141/2597-15. Preprint available [ here](https://svn.vsp.tu-berlin.de/repos/public-svn/publications/vspwp/2015/15-13/). \s
					[3] N. Kuehnel, I. Kaddoura and R. Moeckel. Noise Shielding in an Agent-Based Transport Model Using Volunteered Geographic Data. Procedia Computer Science Volume 151, 2019. Pages 808-813 DOI: 10.1016/j.procs.2019.04.110. Available from <a href="https://www.sciencedirect.com/science/article/pii/S1877050919305745">here</a>.
					""";
				viz.backgroundColor = "white";
			});


	}

}
