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
				viz.backgroundColor = "white";
				viz.content = """

					# Disclaimer

					Die in dieser Analyse verwendeten Schallpegelwerte basieren auf den Vorgaben von **CNOSSOS-EU** sowie der deutschen Norm **RLS-16**. Es werden der nächtliche Schallpegel ($L_{night}$) sowie der berechnete Gesamtpegel $\\mathbf{L_{DEN}}$ angezeigt, der aus den Werten $L_{day}$, $L_{evening}$ und $L_{night}$ abgeleitet wird.

					Die Berechnung von $\\mathbf{L_{DEN}}$ erfolgt gemäß folgender Formel:

					$$
					L_{DEN} = 10 \\cdot \\log_{10}\\left(\\frac{12 \\cdot 10^{L_{day}/10} + 4 \\cdot 10^{(L_{evening}+5)/10} + 8 \\cdot 10^{(L_{night}+10)/10}}{24}\\right)
					$$

					Die Mittelungspegel $L_{day}$, $L_{evening}$ und $L_{night}$ werden mittels der Formel

					$$
					L_{avg} = 10 \\cdot \\log_{10}\\left(\\frac{1}{n} \\sum 10^{L/10}\\right)
					$$

					berechnet, wobei $n$ die Anzahl der Messwerte und $L$ der jeweilige Schallpegel ist. Für die einzelnen Tagesabschnitte gilt:

					- **Tag ($L_{day}$)**: Berechnet über den Zeitraum von 06:00 bis 18:00 Uhr
					- **Abend ($L_{evening}$)**: Berechnet über den Zeitraum von 18:00 bis 22:00 Uhr
					- **Nacht ($L_{night}$)**: Berechnet über den Zeitraum von 22:00 bis 06:00 Uhr

					Diese Formel basiert auf der **RICHTLINIE 2002/49/EG** des Europäischen Parlaments und des Rates vom 25. Juni 2002 zur Bewertung und Bekämpfung von Umgebungslärm. Zur Ermittlung der Mittelungspegel wird die in DIN 45641 beschriebene Methode angewandt, wie sie auch in der Wikipedia-Erklärung zum **Mittelungspegel** (Stand: 19.02.2025) dargestellt wird.

					# Disclaimer (English)

					The noise level data used in this analysis is based on the guidelines of **CNOSSOS-EU** and the German standard **RLS-16**. Both the night-time noise level ($L_{night}$) and the calculated overall level $\\mathbf{L_{DEN}}$ — derived from $L_{day}$, $L_{evening}$, and $L_{night}$ — are presented.

					$\\mathbf{L_{DEN}}$ is computed using the following formula:

					$$
					L_{DEN} = 10 \\cdot \\log_{10}\\left(\\frac{12 \\cdot 10^{L_{day}/10} + 4 \\cdot 10^{(L_{evening}+5)/10} + 8 \\cdot 10^{(L_{night}+10)/10}}{24}\\right)
					$$

					The noise levels $L_{day}$, $L_{evening}$, and $L_{night}$ are averaged using the formula

					$$
					L_{avg} = 10 \\cdot \\log_{10}\\left(\\frac{1}{n} \\sum 10^{L/10}\\right)
					$$

					where $n$ represents the number of measurements and $L$ is the individual noise level. The averaging is performed for different time periods as follows:

					- **Day ($L_{day}$)**: Calculated over the period from 06:00 to 18:00
					- **Evening ($L_{evening}$)**: Calculated over the period from 18:00 to 22:00
					- **Night ($L_{night}$)**: Calculated over the period from 22:00 to 06:00

					This formula is based on the **Directive 2002/49/EC** of the European Parliament and the Council of 25 June 2002, which governs the assessment and management of environmental noise. In addition, the averaging of noise levels is performed according to the method described in DIN 45641, as also referenced in the Wikipedia entry on **noise level averaging** (dated 19.02.2025).
					""";
			});


	}

}
