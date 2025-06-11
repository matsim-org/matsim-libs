package org.matsim.simwrapper;

import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.simwrapper.dashboard.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Default dashboards suited for every run.
 */
public class DefaultDashboardProvider implements DashboardProvider {

	@Override
	public List<Dashboard> getDashboards(Config config, SimWrapper simWrapper) {
		List<Dashboard> result = new ArrayList<>(List.of(
			new OverviewDashboard(),
			new TripDashboard(),
			new TrafficDashboard(Set.copyOf(config.qsim().getMainModes()))
		));

		if (config.transit().isUseTransit()) {
			result.add(new PublicTransitDashboard());
		}

		if (config.counts().getCountsFileName() != null) {
			result.add(new TrafficCountsDashboard());
		}

		if (ConfigUtils.hasModule(config, EmissionsConfigGroup.class)) {
			result.add(new EmissionsDashboard(config.global().getCoordinateSystem()));
			result.add(new ImpactAnalysisDashboard(config.qsim().getMainModes()));
		}

		if (ConfigUtils.hasModule(config, NoiseConfigGroup.class)) {
			result.add(new NoiseDashboard(config.global().getCoordinateSystem()));
		}

		result.add(new StuckAgentDashboard());

		return result;
	}


	@Override
	public double priority() {
		return -1;
	}
}
