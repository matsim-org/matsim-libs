package org.matsim.simwrapper.dashboard;

import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.viz.MapPlot;
import org.matsim.simwrapper.viz.SelectLinkAnalysis;

public class SelectLinkAnalysisDashboard implements Dashboard {

		@Override
		public void configure(Header header, Layout layout, SimWrapperConfigGroup configGroup) {

			header.tab = "SLA";
			header.title = "Select Link Analysis";
			header.description = "Click a link on the network to see the OD flows of agents who traversed this link in the selected hour.";

				layout.row("row")
					.el(SelectLinkAnalysis.class, (viz, data) -> {
						viz.title = "Select Link Analysis Viewer";
						viz.description = "Click on a link to view the select analysis viz.";
						// attempting to simplify - network.avro is just referenced in yml as network: "nameOfFile"
						viz.network = viz.setNetwork(data.compute(CreateAvroNetwork.class, "network.avro", "--with-properties"));
//						viz.setShape(data.compute(CreateAvroNetwork.class, "network.avro", "--with-properties"), "linkId");
						viz.height = 12d;
			});
	}

}
