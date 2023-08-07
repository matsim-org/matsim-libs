package org.matsim.contrib.drt.extension.dashboards;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.DashboardProvider;
import org.matsim.simwrapper.SimWrapper;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DrtDashboardProvider implements DashboardProvider {
	@Override
	public List<Dashboard> getDashboards(Config config, SimWrapper simWrapper) {

		List<Dashboard> result = new ArrayList<>();

		if (ConfigUtils.hasModule(config, MultiModeDrtConfigGroup.class)) {
			MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);

			for (DrtConfigGroup drtConfig : multiModeDrtConfigGroup.getModalElements()) {

				URL transitStopFile = null;
				URL serviceAreaShapeFile = null;
				//it might be, that a serviceArea file is provided in the config, but the drt service is configured to be stopbased, nevertheless
				switch (drtConfig.operationalScheme) {
					case stopbased -> {
						transitStopFile = drtConfig.transitStopFile != null ? ConfigGroup.getInputFileURL(config.getContext(), drtConfig.transitStopFile) : null;
					}
					case door2door -> {
						//TODO potentially show the entire drt network (all drt links have stops)
					}
					case serviceAreaBased -> {
						 serviceAreaShapeFile = drtConfig.drtServiceAreaShapeFile != null ? ConfigGroup.getInputFileURL(config.getContext(), drtConfig.drtServiceAreaShapeFile) : null;
					}
				}

				result.add(new DrtDashboard(drtConfig.mode, config.global().getCoordinateSystem(), transitStopFile, serviceAreaShapeFile));
				result.add(new DrtDetailedDashboard(drtConfig.mode, transitStopFile));
//				result.add(new DrtSupplyDashboard());
//				result.add(new DrtDemandDashboard());
			}
		}

		return result;
	}
}
