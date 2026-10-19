package org.matsim.contrib.pseudosimulation.distributed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.contrib.pseudosimulation.replanning.DistributedPlanStrategyTranslationAndRegistration;
import org.matsim.contrib.pseudosimulation.util.CollectionUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ReplanningConfigGroup;

/** Applies the distributed master and slave replanning-weight policies to a MATSim config. */
final class ReplanningWeightUpdater {

	private static final String REPLACE_PLAN_FROM_SLAVE = "ReplacePlanFromSlave";

	int updateMaster(Config config, double mutationRate, double borrowingRate) {
		int disableAfterIteration = config.controller().getLastIteration();
		int maximumIterationForMutationDisabling = -1;
		if (borrowingRate + mutationRate >= 1) {
			borrowingRate = 0.9999 * borrowingRate / (mutationRate + borrowingRate);
			mutationRate = 0.9999 * mutationRate / (mutationRate + borrowingRate);
		}

		StrategyGroups groups = classify(config);
		for (int index : groups.mutators().keySet()) {
			maximumIterationForMutationDisabling = Math.max(
					groups.settings().get(index).getDisableAfter(), maximumIterationForMutationDisabling);
		}
		redistribute(config, groups, 1 - mutationRate - borrowingRate, mutationRate);

		int innovationEndsAtIteration = maximumIterationForMutationDisabling > 0
				? maximumIterationForMutationDisabling
				: disableAfterIteration;
		ReplanningConfigGroup.StrategySettings borrowingSetting = new ReplanningConfigGroup.StrategySettings();
		borrowingSetting.setWeight(borrowingRate);
		borrowingSetting.setStrategyName(REPLACE_PLAN_FROM_SLAVE);
		borrowingSetting.setDisableAfter(innovationEndsAtIteration);
		config.replanning().addStrategySettings(borrowingSetting);
		return innovationEndsAtIteration;
	}

	void updateSlave(Config config, double mutationRate) {
		if (mutationRate > 1) {
			mutationRate = 0.9999;
		}
		redistribute(config, classify(config), 1 - mutationRate, mutationRate);
	}

	private StrategyGroups classify(Config config) {
		List<ReplanningConfigGroup.StrategySettings> strategySettings = new ArrayList<>();
		strategySettings.addAll(config.replanning().getStrategySettings());
		Map<Integer, Double> selectors = new HashMap<>();
		Map<Integer, Double> mutators = new HashMap<>();
		for (int i = 0; i < strategySettings.size(); i++) {
			ReplanningConfigGroup.StrategySettings setting = strategySettings.get(i);
			if (DistributedPlanStrategyTranslationAndRegistration.SupportedSelectors.keySet()
					.contains(setting.getStrategyName())) {
				selectors.put(i, setting.getWeight());
			} else {
				mutators.put(i, setting.getWeight());
			}
		}
		return new StrategyGroups(strategySettings, selectors, mutators);
	}

	private void redistribute(Config config, StrategyGroups groups, double selectorWeight, double mutatorWeight) {
		double mutatorSum = CollectionUtils.sumElements(groups.mutators().values());
		double selectorSum = CollectionUtils.sumElements(groups.selectors().values());
		for (Map.Entry<Integer, Double> entry : groups.selectors().entrySet()) {
			groups.settings().get(entry.getKey()).setWeight(selectorWeight * entry.getValue() / selectorSum);
		}
		for (Map.Entry<Integer, Double> entry : groups.mutators().entrySet()) {
			groups.settings().get(entry.getKey()).setWeight(mutatorWeight * entry.getValue() / mutatorSum);
		}
		config.replanning().clearStrategySettings();
		for (ReplanningConfigGroup.StrategySettings strategySetting : groups.settings()) {
			config.replanning().addStrategySettings(strategySetting);
		}
	}

	private record StrategyGroups(
			List<ReplanningConfigGroup.StrategySettings> settings,
			Map<Integer, Double> selectors,
			Map<Integer, Double> mutators) {
	}
}
