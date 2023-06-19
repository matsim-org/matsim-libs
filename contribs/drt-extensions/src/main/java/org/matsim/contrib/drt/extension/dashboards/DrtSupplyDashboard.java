package org.matsim.contrib.drt.extension.dashboards;

import org.matsim.application.analysis.LogFileAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Hexagons;
import org.matsim.simwrapper.viz.Line;
import org.matsim.simwrapper.viz.Plotly;
import org.matsim.simwrapper.viz.Table;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.traces.BarTrace;

import java.util.List;

/**
 * @Autor:xinxin
 */
public class DrtSupplyDashboard implements Dashboard {
	@Override
	public void configure(Header header, Layout layout) {
		header.tab = "Supply";
		header.title = "General KPIs";
		header.description = "Only conventional KEXI vehicles are considered";
		layout.row("first")
				.el(Table.class, (viz, data) -> {
					viz.title = "Supply Numbers";
					viz.description = "only human-driven fleet";
					viz.showAllRows = true;
					viz.dataset = data.resource("topsheet-KEXI-base-supply.yaml");
				})
				.el(Hexagons.class, (viz, data) -> {
					viz.title = "Stops";
					viz.description = "All KEXI stops in Kehlheim";
					viz.height = 10d;
					viz.width = 2d;

					viz.center = data.context().getCenter();
					viz.zoom = data.context().mapZoomLevel;


					// TODO: create stops csv from xml

					viz.file = "../../input/kelheim-v2.0-drt-stops-locations.csv";

					 //viz.addAggregation("Locations","Stops","X","Y",)







				});

		// TODO
	}
}
