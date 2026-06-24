package org.matsim.simwrapper.dashboard;

import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.viz.MapPlot;

public class SelectLinkAnalysisDashboard implements Dashboard {

		@Override
		public void configure(Header header, Layout layout, SimWrapperConfigGroup configGroup) {

			header.title = "Select Link Analysis";
			header.description = "Click a link on the network to see the OD flows of agents who traversed this link in the selected hour.";

				layout.row("one")
					.el(new MapPlot("selectLink").getClass(), (viz, data) -> {
						viz.title = "SLA";
						viz.setShape(data.compute(CreateAvroNetwork.class, "network.avro", "--with-properties"), "linkId");
						viz.height = 12d;
			});

//			layout.row("disclaimer")
//				.el(TextBlock.class, (viz, data) -> {
//					viz.backgroundColor = "white";
//					viz.content = """
//					# Disclaimer
//
//					Die in dieser Analyse verwendeten Verkehrsdaten basieren auf einer Hochrechnung von Verkehrszahlen eines einzelnen Tages auf ein gesamtes Jahr. Die Grundlage dieser Hochrechnung stammt aus dem Bericht: \s
//
//					**Grundsätzliche Überprüfung und Weiterentwicklung der Nutzen-Kosten-Analyse im Bewertungsverfahren der Bundesverkehrswegeplanung** \s
//					FE-PROJEKTNR.: 960974/2011 \s
//					Endbericht für das Bundesministerium für Verkehr und digitale Infrastruktur \s
//					Essen, Berlin, München, 24. März 2015 \s
//
//					Die spezifischen Hochrechnungsfaktoren wurden wie folgt angewendet: \s
//					- Für PKW und alle anderen Verkehrsmittel: **Faktor 334** \s
//					- Für LKW: **Faktor 302** \s
//
//					Diese Faktoren basieren auf den Angaben des genannten Berichts (Seite 172) und wurden ebenfalls im Rahmen des Bundesverkehrswegeplans 2030 verwendet. \s
//
//					# Disclaimer (English)
//
//					The traffic data used in this analysis is based on an extrapolation of single-day traffic figures to an entire year. The basis for this extrapolation is derived from the report: \s
//
//					**Fundamental Review and Further Development of the Cost-Benefit Analysis in the Assessment Procedure of Federal Transport Infrastructure Planning** \s
//					FE-PROJECT NO.: 960974/2011 \s
//					Final Report for the Federal Ministry of Transport and Digital Infrastructure \s
//					Essen, Berlin, Munich, March 24, 2015 \s
//
//					The specific extrapolation factors applied are as follows: \s
//					- For passenger cars (PKW) and all other modes of transport: **Factor 334** \s
//					- For trucks (LKW): **Factor 302** \s
//
//					These factors are based on the data provided in the mentioned report (page 172) and were also used in the Federal Transport Infrastructure Plan 2030. \s
//					""";
//				});
//		}
	}

}
