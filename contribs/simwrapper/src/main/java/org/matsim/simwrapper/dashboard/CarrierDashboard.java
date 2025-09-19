package org.matsim.simwrapper.dashboard;

import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.CarrierViewer;
import org.matsim.simwrapper.viz.Table;

import java.util.List;

/**
 * Standard dashboard for the carrier viewer.
 */
public class CarrierDashboard implements Dashboard  {

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Carrier Viewer";
		header.tab = "";

		layout.row("viewer").el(CarrierViewer.class, (viz, data) -> {
			viz.title = "Carrier Viewer";
			viz.height = 12d;
			viz.description = "Visualize the carrier's routes";

			// Include a network that has not been filtered
			viz.network = data.withContext("all").compute(CreateAvroNetwork.class, "network.avro",
				"--mode-filter", "", "--shp", "none");

			viz.carriers = data.output("(*.)?output_carriers.xml.gz");

		});
	}
}
