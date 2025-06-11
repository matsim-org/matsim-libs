package org.matsim.contribs.discrete_mode_choice.modules;

import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.utils.ModeChoiceInTheLoopChecker;
import org.matsim.contribs.discrete_mode_choice.replanning.DiscreteModeChoiceStrategyProvider;
import org.matsim.contribs.discrete_mode_choice.replanning.NonSelectedPlanSelector;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Inject;

/**
 * Main module of the Discrete Mode Choice extension. Should be added as an
 * overriding module before the MATSim controller is started.
 *
 * @author sebhoerl
 */
public class DiscreteModeChoiceModule extends AbstractModule {
	public static final String STRATEGY_NAME = "DiscreteModeChoice";

	@Inject
	private DiscreteModeChoiceConfigGroup dmcConfig;

	@Override
	public void install() {
		addPlanStrategyBinding(STRATEGY_NAME).toProvider(DiscreteModeChoiceStrategyProvider.class);
		addControlerListenerBinding().to(UtilitiesWriterHandler.class);

		if (getConfig().replanning().getPlanSelectorForRemoval().equals(NonSelectedPlanSelector.NAME)) {
			bindPlanSelectorForRemoval().to(NonSelectedPlanSelector.class);
		}

		if (dmcConfig.getEnforceSinglePlan()) {
			addControlerListenerBinding().to(ModeChoiceInTheLoopChecker.class);
		}

		install(new ModelModule());
	}
}
