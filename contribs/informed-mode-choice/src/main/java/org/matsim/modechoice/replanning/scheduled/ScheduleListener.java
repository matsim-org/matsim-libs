package org.matsim.modechoice.replanning.scheduled;

import jakarta.inject.Inject;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.selectors.WorstPlanForRemovalSelector;
import org.matsim.modechoice.ScheduledModeChoiceConfigGroup;

/**
 * Helper listener.
 */
public class ScheduleListener implements IterationEndsListener {

	private final StrategyManager strategyManager;
	private final ScheduledModeChoiceConfigGroup config;

	private boolean iterOver = false;

	@Inject
	public ScheduleListener(StrategyManager strategyManager, Config config) {
		this.strategyManager = strategyManager;
		this.config = ConfigUtils.addOrGetModule(config, ScheduledModeChoiceConfigGroup.class);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		int applyIdx = ScheduledStrategyChooser.isScheduledIteration(event.getIteration(), config);

		if (applyIdx == ScheduledStrategyChooser.ITER_OVER && !iterOver) {
			// restore the default removal selector for final phase
			strategyManager.setPlanSelectorForRemoval(new WorstPlanForRemovalSelector());

			// Reset plan types at the end so plan can be removed freely
			for (Person person : event.getServices().getScenario().getPopulation().getPersons().values()) {
				for (Plan plan : person.getPlans()) {
					plan.setType(null);
				}
			}

			iterOver = true;
		}
	}
}
