package org.matsim.simwrapper;

import org.matsim.core.config.Config;
import org.matsim.simwrapper.dashboard.TrafficCountsDashboard;
import org.matsim.simwrapper.dashboard.StuckAgentDashboard;
import org.matsim.simwrapper.dashboard.TripDashboard;

import java.util.ArrayList;
import java.util.List;

/**
 * Default dashboards suited for every run.
 */
public class DefaultDashboardProvider implements DashboardProvider {
	@Override
	public List<Dashboard> getDashboards(Config config, SimWrapper simWrapper) {
		List<Dashboard> result = new ArrayList<>(List.of(
			new TripDashboard(),
			new StuckAgentDashboard()
		));

		if (config.counts().getCountsFileName() != null) {
			result.add(new TrafficCountsDashboard());
		}

		return result;
	}


	@Override
	public double priority() {
		return -1;
	}
}
