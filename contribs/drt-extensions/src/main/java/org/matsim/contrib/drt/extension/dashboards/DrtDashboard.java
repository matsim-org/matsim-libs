package org.matsim.contrib.drt.extension.dashboards;


import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Data;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Table;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * General DRT dashboard for one specific service.
 */
public class DrtDashboard implements Dashboard {

	private final String mode;
	private final URL transitStopFile;
	private final URL serviceAreaShapeFile;

	public DrtDashboard(String mode, URL transitStopFile, URL serviceAreaShapeFile) {
		this.mode = mode;
		this.transitStopFile = transitStopFile;
		this.serviceAreaShapeFile = serviceAreaShapeFile;
	}

	/**
	 * Auto generate title.
	 */
	public String getTitle() {
		String name = context();
		return name.isBlank() ? "DRT" : "DRT - " + name;
	}

	@Override
	public String context() {
		return mode.replace("drt", "").replace("Drt", "");
	}

	private String postProcess(Data data, String file) {
		List<String> args = new ArrayList<>(List.of("--drt-mode", mode));
		if (transitStopFile != null) {
			args.addAll(List.of("--stop-file", transitStopFile.toString()));
		}

		return data.compute(DrtAnalysisPostProcessing.class, file, args.toArray(new String[0]));
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = getTitle();

		layout.row("overview")
			.el(Table.class, (viz, data) -> {
				viz.dataset = postProcess(data, "overview.csv");
				viz.showAllRows = true;
			});


	}
}
