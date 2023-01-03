package org.matsim.contribs.discrete_mode_choice.replanning;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;

/**
 * This replanning module is a simple check that will fail if at least one leg
 * in an agent's plan has no route assigned.
 * 
 * @author sebhoerl
 */
public class CheckConsistentRoutingReplanningModule extends AbstractMultithreadedModule {
	public CheckConsistentRoutingReplanningModule(GlobalConfigGroup globalConfigGroup) {
		super(globalConfigGroup);
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new PlanAlgorithm() {
			@Override
			public void run(Plan plan) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Leg) {
						Leg leg = (Leg) element;

						if (leg.getRoute() == null) {
							throw new IllegalStateException(
									String.format("%s.%s is turned off, but route is missing in plan for agent %s",
											DiscreteModeChoiceConfigGroup.GROUP_NAME,
											DiscreteModeChoiceConfigGroup.PERFORM_REROUTE,
											plan.getPerson().getId().toString()));
						}
					}
				}
			}
		};
	}
}
