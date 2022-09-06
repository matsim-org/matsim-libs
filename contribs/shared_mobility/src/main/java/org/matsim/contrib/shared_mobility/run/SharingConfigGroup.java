package org.matsim.contrib.shared_mobility.run;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.shared_mobility.run.SharingServiceConfigGroup.ServiceScheme;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.consistency.BeanValidationConfigConsistencyChecker;

public class SharingConfigGroup extends ReflectiveConfigGroup {
	private final static Logger logger = LogManager.getLogger(SharingConfigGroup.class);
	public static final String GROUP_NAME = "sharing";

	public SharingConfigGroup() {
		super(GROUP_NAME);
	}

	public void addService(SharingServiceConfigGroup modeConfig) {
		addParameterSet(modeConfig);
	}

	public Collection<SharingServiceConfigGroup> getServices() {
		List<SharingServiceConfigGroup> services = new LinkedList<>();

		for (ConfigGroup set : getParameterSets(SharingServiceConfigGroup.GROUP_NAME)) {
			services.add((SharingServiceConfigGroup) set);
		}

		return Collections.unmodifiableList(services);
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		new BeanValidationConfigConsistencyChecker().checkConsistency(config);

		Set<String> serviceIds = new HashSet<>();

		for (SharingServiceConfigGroup serviceConfig : getServices()) {
			// Avoid duplicate IDs

			if (serviceIds.contains(serviceConfig.getId())) {
				throw new IllegalStateException("Duplicate sharing service: " + serviceConfig.getId());
			}

			serviceIds.add(serviceConfig.getId());

			// Some warnings

			if (serviceConfig.getServiceScheme().equals(ServiceScheme.StationBased)
					&& serviceConfig.getServiceAreaShapeFile() != null) {
				logger.warn("Service " + serviceConfig.getId()
						+ " is station-based, so service area file will not be used!");
			}
		}
	}
}
