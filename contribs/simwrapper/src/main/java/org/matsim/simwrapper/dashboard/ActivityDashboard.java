package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.activity.ActivityCountAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.MapPlot;

public class ActivityDashboard implements Dashboard {
	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Activity Analysis";
		header.description = "Displays the activities by type and location.";

		layout.row("activites")
			.el(MapPlot.class, (viz, data) -> {
				viz.title = "Activity Map";
				viz.description = "Activities per region";
				viz.height = 12.0;
				viz.center = data.context().getCenter();
				viz.zoom = data.context().mapZoomLevel;
				viz.display.fill.dataset = "activities_home";

				// --shp= + data.resource(fff.shp)

				viz.display.fill.columnName = "count";
				String shp = data.resources("kehlheim_test.shp", "kehlheim_test.shx", "kehlheim_test.dbf", "kehlheim_test.prj");
				viz.setShape(shp);


				viz.addDataset("activities_home", data.computeWithPlaceholder(ActivityCountAnalysis.class, "activities_%s_per_region.csv", "home", "--id-column=id", "--shp=" + shp));
				viz.addDataset("activities_other", data.computeWithPlaceholder(ActivityCountAnalysis.class, "activities_%s_per_region.csv", "other", "--id-column=id", "--shp=" + shp));
			});
	}
}
