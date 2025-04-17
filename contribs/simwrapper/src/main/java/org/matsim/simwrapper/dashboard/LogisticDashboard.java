package org.matsim.simwrapper.dashboard;

import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.LogisticViewer;

/**
 * Standard dashboard for the logistic viewer.
 */

public class LogisticDashboard implements Dashboard  {

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Logisitc Viewer";
		header.tab = "";

		layout.row("viewer").el(LogisticViewer.class, (viz, data) -> {
			viz.title = "Logisitic Viewer";
			viz.height = 12d;
			viz.description = "Visualize the logistical routes for carriers";

			// Include a network that has not been filtered
			viz.network = data.withContext("all").compute(CreateAvroNetwork.class, "network.avro",
				"--mode-filter", "", "--shp", "none");

			viz.carrier = data.output("(*.)?output_carriers.xml.gz");
			viz.lsps = data.output("(*.)?output_lsps.xml.gz");

		});
	}
}
