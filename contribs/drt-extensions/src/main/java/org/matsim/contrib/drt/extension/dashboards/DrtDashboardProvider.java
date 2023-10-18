package org.matsim.contrib.drt.extension.dashboards;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.DashboardProvider;
import org.matsim.simwrapper.SimWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates one dashboard per drt service.
 */
public class DrtDashboardProvider implements DashboardProvider {
	@Override
	public List<Dashboard> getDashboards(Config config, SimWrapper simWrapper) {

		List<Dashboard> result = new ArrayList<>();

		if (ConfigUtils.hasModule(config, MultiModeDrtConfigGroup.class)) {
			MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);

			for (DrtConfigGroup drtConfig : multiModeDrtConfigGroup.getModalElements()) {

				result.add(new DrtDashboard(drtConfig, config.getContext(), config.global().getCoordinateSystem(), config.controller().getLastIteration()));
			}
		}

		return result;
	}
}
