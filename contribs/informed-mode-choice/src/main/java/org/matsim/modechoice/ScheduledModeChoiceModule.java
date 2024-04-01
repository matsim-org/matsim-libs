package org.matsim.modechoice;

import com.google.inject.TypeLiteral;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.replanning.choosers.StrategyChooser;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.modechoice.replanning.WorstNotSelctedPlanSelector;
import org.matsim.modechoice.replanning.scheduled.AllBestPlansStrategyProvider;
import org.matsim.modechoice.replanning.scheduled.ReRouteSelectedStrategyProvider;
import org.matsim.modechoice.replanning.scheduled.ScheduledStrategyChooser;
import org.matsim.modechoice.replanning.scheduled.TimeMutateSelectedStrategyProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * Module for to enable scheduled mode choice.
 */
public class ScheduledModeChoiceModule extends AbstractModule {

	private static final Logger log = LogManager.getLogger(ScheduledModeChoiceModule.class);

	public static String ALL_BEST_K_PLAN_MODES_STRATEGY = "AllBestKPlanModes";
	/**
	 * Other mode choice strategies.
	 */
	private final static Set<String> EXCLUDED = Set.of(
		DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice,
		DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode,
		DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode,
		DefaultPlanStrategiesModule.DefaultStrategy.ReRoute,
		DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator,
		DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator_ReRoute,
		InformedModeChoiceModule.SELECT_SUBTOUR_MODE_STRATEGY,
		InformedModeChoiceModule.SELECT_BEST_K_PLAN_MODES_STRATEGY,
		InformedModeChoiceModule.SELECT_SINGLE_TRIP_MODE_STRATEGY,
		InformedModeChoiceModule.RANDOM_SUBTOUR_MODE_STRATEGY,
		ALL_BEST_K_PLAN_MODES_STRATEGY
	);
	public static String REROUTE_SELECTED = "ReRouteSelected";
	public static String TIME_MUTATE_SELECTED = "TimeAllocationMutatorSelected";


	private final Builder builder;

	private ScheduledModeChoiceModule(Builder builder) {
		this.builder = builder;
	}

	/**
	 * Create new builder to initialize the module.
	 */
	public static Builder newBuilder() {
		return new Builder();
	}

	@Override
	public void install() {

		addPlanStrategyBinding(ALL_BEST_K_PLAN_MODES_STRATEGY).toProvider(AllBestPlansStrategyProvider.class);
		addPlanStrategyBinding(REROUTE_SELECTED).toProvider(ReRouteSelectedStrategyProvider.class);
		addPlanStrategyBinding(TIME_MUTATE_SELECTED).toProvider(TimeMutateSelectedStrategyProvider.class);

		bind(new TypeLiteral<StrategyChooser<Plan, Person>>() {}).to(ScheduledStrategyChooser.class);

		ScheduledModeChoiceConfigGroup config = ConfigUtils.addOrGetModule(getConfig(), ScheduledModeChoiceConfigGroup.class);

		Collection<ReplanningConfigGroup.StrategySettings> strategies = new ArrayList<>(getConfig().replanning().getStrategySettings());
		getConfig().replanning().clearStrategySettings();

		for (String subpopulation : config.getSubpopulations()) {

			OptionalDouble reroute = strategies.stream().filter(s -> s.getStrategyName().equals(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute) &&
					s.getSubpopulation().equals(subpopulation))
				.mapToDouble(ReplanningConfigGroup.StrategySettings::getWeight)
				.findFirst();

			OptionalDouble timeMutate = strategies.stream().filter(s ->
					(s.getStrategyName().equals(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator) ||
						s.getStrategyName().equals(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator_ReRoute)) &&
						s.getSubpopulation().equals(subpopulation))
				.mapToDouble(ReplanningConfigGroup.StrategySettings::getWeight)
				.findFirst();

			strategies.removeIf(s -> s.getSubpopulation().equals(subpopulation) && EXCLUDED.contains(s.getStrategyName()));

			ReplanningConfigGroup.StrategySettings strategy = new ReplanningConfigGroup.StrategySettings();
			strategy.setStrategyName(ALL_BEST_K_PLAN_MODES_STRATEGY);
			strategy.setSubpopulation(subpopulation);
			strategy.setWeight(1.0);
			strategies.add(strategy);

			if (reroute.isPresent()) {
				ReplanningConfigGroup.StrategySettings s = new ReplanningConfigGroup.StrategySettings();
				s.setStrategyName(REROUTE_SELECTED);
				s.setSubpopulation(subpopulation);
				s.setWeight(reroute.getAsDouble());
				strategies.add(s);
			}

			if (timeMutate.isPresent()) {
				ReplanningConfigGroup.StrategySettings s = new ReplanningConfigGroup.StrategySettings();
				s.setStrategyName(TIME_MUTATE_SELECTED);
				s.setSubpopulation(subpopulation);
				s.setWeight(timeMutate.getAsDouble());
				strategies.add(s);
			}
		}

		strategies.forEach(s -> getConfig().replanning().addStrategySettings(s));

		if (config.isAdjustTargetIterations()) {

			int iters = config.getWarumUpIterations() + config.getScheduleIterations() * (1 + config.getBetweenIterations());

			int target = (int) Math.ceil(iters / getConfig().replanning().getFractionOfIterationsToDisableInnovation());

			log.info("Adjusting number of iterations from {} to {}.", getConfig().controller().getLastIteration(), target);
			getConfig().controller().setLastIteration(target);
		}

		bindPlanSelectorForRemoval().to(WorstNotSelctedPlanSelector.class);

	}

	/**
	 * Builder to configure the module.
	 */
	public static final class Builder {
		public ScheduledModeChoiceModule build() {
			return new ScheduledModeChoiceModule(this);
		}
	}

}
