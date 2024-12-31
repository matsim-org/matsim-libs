package org.matsim.contrib.ev.strategic.replanning;

import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;

import com.google.inject.Provider;

/**
 * This class registers the replanning strategy for strategic charging.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class StrategicChargingReplanningModule extends AbstractMultithreadedModule {
	private final Provider<StrategicChargingReplanningAlgorithm> algorithmProvider;

	StrategicChargingReplanningModule(GlobalConfigGroup globalConfigGroup,
			Provider<StrategicChargingReplanningAlgorithm> algorithmProvider) {
		super(globalConfigGroup);
		this.algorithmProvider = algorithmProvider;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return algorithmProvider.get();
	}
}
