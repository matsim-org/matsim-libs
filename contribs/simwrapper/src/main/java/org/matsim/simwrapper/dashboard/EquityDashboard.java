package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.LogFileAnalysis;
import org.matsim.application.analysis.traffic.TrafficAnalysis;
import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.*;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.traces.BarTrace;

import java.util.List;

/**
 * Equity Dashboard
 */
public class EquityDashboard implements Dashboard {
	@Override
	public void configure(Header header, Layout layout) {

		header.title = "(In)Equity";
		header.description = "Transport Metrics differentiated by demographic attributes";

		layout.row("first").el(Table.class, (viz, data) -> {
			viz.title = "Score x Gender";
			viz.showAllRows = true;
			viz.dataset = data.compute(LogFileAnalysis.class, "run_info.csv");
			viz.width = 1d;
		});
	}


}
