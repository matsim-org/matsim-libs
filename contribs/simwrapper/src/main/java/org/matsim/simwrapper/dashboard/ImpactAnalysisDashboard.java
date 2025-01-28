package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.impact.ImpactAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Table;
import org.matsim.simwrapper.viz.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Dashboard with general overview.
 */
public class ImpactAnalysisDashboard implements Dashboard {

	private final Collection<String> modes;

	/**
	 * Constructor.
	 *
	 * @param modes The modes to display.
	 */
	public ImpactAnalysisDashboard(Collection<String> modes) {
		this.modes = modes;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Impact Analysis";
		header.description = "Impact overview of the MATSim run.";

		List<String> modeArgs = new ArrayList<>(List.of("--modes", String.join(",", modes)));

		String[] modeArgsArray = modeArgs.toArray(new String[0]);

		modes.forEach(mode -> {
			layout.row(mode)
				.el(Table.class, (viz, data) -> {
					viz.title = "Central Traffic / Physical Effects (" + mode.substring(0, 1).toUpperCase() + mode.substring(1) + ")";
					viz.style = "topsheet";
					viz.dataset = data.computeWithPlaceholder(ImpactAnalysis.class, "general_%s.csv", mode, modeArgsArray);
					viz.enableFilter = false;
					viz.showAllRows = true;
					viz.width = 1d;
					viz.height = 5d;
					viz.alignment = new String[]{"right", "right", "left"};
				})

				.el(Table.class, (viz, data) -> {
					viz.title = "Change In Exhaust Emissions (" + mode.substring(0, 1).toUpperCase() + mode.substring(1) + ")";
					viz.style = "topsheet";
					viz.dataset = data.computeWithPlaceholder(ImpactAnalysis.class, "emissions_%s.csv", mode, modeArgsArray);
					viz.enableFilter = false;
					viz.showAllRows = true;
					viz.width = 1d;
					viz.height = 5d;
					viz.alignment = new String[]{"right", "right", "left"};
				});
		});

		layout.row("disclaimer")
			.el(TextBlock.class, (viz, data) -> {
				viz.backgroundColor = "white";
				viz.content = """
					# Disclaimer

					Die in dieser Analyse verwendeten Verkehrsdaten basieren auf einer Hochrechnung von Verkehrszahlen eines einzelnen Tages auf ein gesamtes Jahr. Die Grundlage dieser Hochrechnung stammt aus dem Bericht: \s

					**Grundsätzliche Überprüfung und Weiterentwicklung der Nutzen-Kosten-Analyse im Bewertungsverfahren der Bundesverkehrswegeplanung** \s
					FE-PROJEKTNR.: 960974/2011 \s
					Endbericht für das Bundesministerium für Verkehr und digitale Infrastruktur \s
					Essen, Berlin, München, 24. März 2015 \s

					Die spezifischen Hochrechnungsfaktoren wurden wie folgt angewendet: \s
					- Für PKW und alle anderen Verkehrsmittel: **Faktor 334** \s
					- Für LKW: **Faktor 302** \s

					Diese Faktoren basieren auf den Angaben des genannten Berichts (Seite 172) und wurden ebenfalls im Rahmen des Bundesverkehrswegeplans 2030 verwendet. \s

					# Disclaimer (English)

					The traffic data used in this analysis is based on an extrapolation of single-day traffic figures to an entire year. The basis for this extrapolation is derived from the report: \s

					**Fundamental Review and Further Development of the Cost-Benefit Analysis in the Assessment Procedure of Federal Transport Infrastructure Planning** \s
					FE-PROJECT NO.: 960974/2011 \s
					Final Report for the Federal Ministry of Transport and Digital Infrastructure \s
					Essen, Berlin, Munich, March 24, 2015 \s

					The specific extrapolation factors applied are as follows: \s
					- For passenger cars (PKW) and all other modes of transport: **Factor 334** \s
					- For trucks (LKW): **Factor 302** \s

					These factors are based on the data provided in the mentioned report (page 172) and were also used in the Federal Transport Infrastructure Plan 2030. \s
					""";
			});
	}
}
