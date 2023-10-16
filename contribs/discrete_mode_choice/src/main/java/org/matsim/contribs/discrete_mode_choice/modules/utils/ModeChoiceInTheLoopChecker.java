package org.matsim.contribs.discrete_mode_choice.modules.utils;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contribs.discrete_mode_choice.modules.DiscreteModeChoiceConfigurator;
import org.matsim.contribs.discrete_mode_choice.replanning.NonSelectedPlanSelector;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.selectors.PlanSelector;

import com.google.inject.Inject;

/**
 * Internal listener that is used to do some runtime checks when
 * mode-choice-in-the-loop should be enforced.
 *
 * @author sebhoerl
 *
 */
public class ModeChoiceInTheLoopChecker implements StartupListener {
	private final ReplanningConfigGroup strategyConfig;
	private final PlanSelector<Plan, Person> removalSelector;

	@Inject
	public ModeChoiceInTheLoopChecker(ReplanningConfigGroup strategyConfig, PlanSelector<Plan, Person> removalSelector) {
		this.strategyConfig = strategyConfig;
		this.removalSelector = removalSelector;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		DiscreteModeChoiceConfigurator.checkModeChoiceInTheLoop(strategyConfig);

		if (!(removalSelector instanceof NonSelectedPlanSelector)) {
			throw new IllegalStateException(
					"Removal strategy should be NonSelectedPlanSelector if mode-choice-in-the-loop is enforced.");
		}
	}
}
