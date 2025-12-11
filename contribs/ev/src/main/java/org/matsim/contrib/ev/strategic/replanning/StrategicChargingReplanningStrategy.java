package org.matsim.contrib.ev.strategic.replanning;

import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import com.google.inject.Provider;

/**
 * This class registers the replanning strategy for strategic charging.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class StrategicChargingReplanningStrategy implements Provider<PlanStrategy> {
	static public final String STRATEGY = "StrategicCharging";

	private final GlobalConfigGroup globalConfigGroup;
	private final Provider<StrategicChargingReplanningAlgorithm> algorithmProvider;

	public StrategicChargingReplanningStrategy(GlobalConfigGroup globalConfigGroup,
			Provider<StrategicChargingReplanningAlgorithm> algorithmProvider) {
		this.globalConfigGroup = globalConfigGroup;
		this.algorithmProvider = algorithmProvider;
	}

	@Override
	public PlanStrategy get() {
		PlanStrategyImpl.Builder builder = new Builder(new RandomPlanSelector<>());
		builder.addStrategyModule(new StrategicChargingReplanningModule(globalConfigGroup, algorithmProvider));
		return builder.build();
	}
}
