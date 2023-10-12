package org.matsim.contribs.discrete_mode_choice.modules;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.matsim.contribs.discrete_mode_choice.modules.ModelModule.ModelType;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.replanning.NonSelectedPlanSelector;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;

/**
 * Utility class that makes it possible to set up various was of using the
 * Discrete Mode Choice extension with MATSim.
 *
 * @author sebhoerl
 */
public final class DiscreteModeChoiceConfigurator {
	private DiscreteModeChoiceConfigurator() {

	}

	static public void configureAsSubtourModeChoiceReplacement(Config config) {
		for (StrategySettings strategy : config.replanning().getStrategySettings()) {
			if (strategy.getStrategyName().equals(DefaultStrategy.SubtourModeChoice)) {
				strategy.setStrategyName(DiscreteModeChoiceModule.STRATEGY_NAME);
			}
		}

		SubtourModeChoiceConfigGroup smcConfig = config.subtourModeChoice();
		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		if (dmcConfig == null) {
			dmcConfig = new DiscreteModeChoiceConfigGroup();
			config.addModule(dmcConfig);
		}

		List<String> tourConstraints = new LinkedList<>();
		tourConstraints.add(ConstraintModule.VEHICLE_CONTINUITY);
		tourConstraints.add(ConstraintModule.SUBTOUR_MODE);

		if (smcConfig.getProbaForRandomSingleTripMode() > 0.0) {
			dmcConfig.getSubtourConstraintConfig().setConstrainedModes(Arrays.asList(smcConfig.getChainBasedModes()));
		} else {
			dmcConfig.getSubtourConstraintConfig().setConstrainedModes(Arrays.asList(smcConfig.getModes()));
		}
		// ...setConstrainedModes used to ignore its arguments due to a typo.  This is now corrected, but results are no longer backwards compatible.  kai, jan'23

		dmcConfig.setCachedModes(Arrays.asList(smcConfig.getModes()));

		dmcConfig.setModelType(ModelType.Tour);
		dmcConfig.setSelector(SelectorModule.RANDOM);
		dmcConfig.setTourConstraints(tourConstraints);
		dmcConfig.setTourEstimator(EstimatorModule.UNIFORM);
		dmcConfig.setTourFinder(TourFinderModule.PLAN_BASED);

		dmcConfig.getVehicleTourConstraintConfig().setRestrictedModes(Arrays.asList(smcConfig.getChainBasedModes()));

		if (smcConfig.considerCarAvailability()) {
			dmcConfig.setModeAvailability(ModeAvailabilityModule.CAR);
			dmcConfig.getCarModeAvailabilityConfig().setAvailableModes(Arrays.asList(smcConfig.getModes()));
		} else {
			dmcConfig.setModeAvailability(ModeAvailabilityModule.DEFAULT);
			dmcConfig.getDefaultModeAvailabilityConfig().setAvailableModes(Arrays.asList(smcConfig.getModes()));
		}
	}

	static public void configureAsImportanceSampler(Config config) {
		configureAsSubtourModeChoiceReplacement(config);
		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		dmcConfig.setSelector(SelectorModule.MULTINOMIAL_LOGIT);
		dmcConfig.setTourEstimator(EstimatorModule.MATSIM_DAY_SCORING);
	}

	private final static double DEFAULT_REPLANNING_RATE = 0.20;

	static public void configureAsModeChoiceInTheLoop(Config config) {
		configureAsModeChoiceInTheLoop(config, DEFAULT_REPLANNING_RATE);
	}

	static public void configureAsModeChoiceInTheLoop(Config config, double replanningRate) {
		ReplanningConfigGroup replanningConfigGroup = config.replanning();
		replanningConfigGroup.clearStrategySettings();

		replanningConfigGroup.setMaxAgentPlanMemorySize(1);
		replanningConfigGroup.setFractionOfIterationsToDisableInnovation(Double.POSITIVE_INFINITY);
		replanningConfigGroup.setPlanSelectorForRemoval(NonSelectedPlanSelector.NAME);

		StrategySettings dmcStrategy = new StrategySettings();
		dmcStrategy.setStrategyName(DiscreteModeChoiceModule.STRATEGY_NAME);
		dmcStrategy.setWeight(replanningRate);
		replanningConfigGroup.addStrategySettings(dmcStrategy);

		StrategySettings selectorStrategy = new StrategySettings();
		selectorStrategy.setStrategyName(DefaultSelector.KeepLastSelected);
		selectorStrategy.setWeight(1.0 - replanningRate);
		replanningConfigGroup.addStrategySettings(selectorStrategy);

		checkModeChoiceInTheLoop(replanningConfigGroup);

		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		if (dmcConfig == null) {
			dmcConfig = new DiscreteModeChoiceConfigGroup();
			config.addModule(dmcConfig);
		}

		dmcConfig.setEnforceSinglePlan(true);
	}

	public static void checkModeChoiceInTheLoop(ReplanningConfigGroup replanningConfigGroup) {
		if (replanningConfigGroup.getMaxAgentPlanMemorySize() != 1) {
			throw new IllegalStateException(
					"Option strategy.maxAgentPlanMemorySize should be 1 if mode-choice-in-the-loop is enforced.");
		}

		Set<String> activeStrategies = new HashSet<>();

		for (StrategySettings strategySettings : replanningConfigGroup.getStrategySettings()) {
			if (strategySettings.getDisableAfter() != 0) {
				activeStrategies.add(strategySettings.getStrategyName());
			}
		}

		if (!activeStrategies.contains(DefaultSelector.KeepLastSelected)) {
			throw new IllegalStateException(
					"KeepLastSelected should be an active strategy if mode-choice-in-the-loop is enforced");
		}

		if (!activeStrategies.contains(DiscreteModeChoiceModule.STRATEGY_NAME)) {
			throw new IllegalStateException("Strategy " + DiscreteModeChoiceModule.STRATEGY_NAME
					+ " must be active if single plan mode is enforced");
		}

		activeStrategies.remove(DefaultSelector.KeepLastSelected);
		activeStrategies.remove(DiscreteModeChoiceModule.STRATEGY_NAME);
		activeStrategies.remove(DefaultStrategy.ReRoute);

		if (activeStrategies.size() > 0) {
			throw new IllegalStateException(
					"All these strategies should be disabled (disableAfter == 0) if mode-choice-in-the-loop is enforced: "
							+ activeStrategies);
		}

		if (!replanningConfigGroup.getPlanSelectorForRemoval().equals(NonSelectedPlanSelector.NAME)) {
			throw new IllegalStateException(
					"Removal selector should be NonSelectedPlanSelector if mode-choice-in-the-loop is enforced.");
		}
	}
}
