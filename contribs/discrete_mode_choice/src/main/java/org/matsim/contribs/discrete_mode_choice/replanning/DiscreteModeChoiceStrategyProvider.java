package org.matsim.contribs.discrete_mode_choice.replanning;

import javax.inject.Inject;

import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;

import com.google.inject.Provider;

/**
 * This defines the general choice strategy for the discrete mode choice
 * extension. It consists of three replanning modules:
 * 
 * <ul>
 * <li>TripsToLegs, which collapses all multi-stage trips into one leg</li>
 * <li>DiscreteModeChoiceReplanningModule, which creates a new discrete choice
 * model and performs the choices <(li>
 * <li>Optionally, ReRoute if is is configured in the config, otherwise a check
 * is added that all routes are present after the mode choice</li>
 * </ul>
 * 
 * @author sebhoerl
 */
public class DiscreteModeChoiceStrategyProvider implements Provider<PlanStrategy> {
	private final GlobalConfigGroup globalConfigGroup;
	private final Provider<TripRouter> tripRouterProvider;
	private final ActivityFacilities activityFacilities;
	private final Provider<DiscreteModeChoiceModel> modeChoiceModelProvider;
	private final Provider<TripListConverter> tripListConverterProvider;
	private final DiscreteModeChoiceConfigGroup dmcConfig;
	private final PopulationFactory populationFactory;
	private final TimeInterpretation timeInterpretation;

	@Inject
	DiscreteModeChoiceStrategyProvider(GlobalConfigGroup globalConfigGroup, ActivityFacilities activityFacilities,
			Provider<TripRouter> tripRouterProvider, Provider<DiscreteModeChoiceModel> modeChoiceModelProvider,
			DiscreteModeChoiceConfigGroup dmcConfig, Population population,
			Provider<TripListConverter> tripListConverterProvider, TimeInterpretation timeInterpretation) {
		this.globalConfigGroup = globalConfigGroup;
		this.activityFacilities = activityFacilities;
		this.tripRouterProvider = tripRouterProvider;
		this.modeChoiceModelProvider = modeChoiceModelProvider;
		this.tripListConverterProvider = tripListConverterProvider;
		this.dmcConfig = dmcConfig;
		this.populationFactory = population.getFactory();
		this.timeInterpretation = timeInterpretation;
	}

	@Override
	public PlanStrategy get() {
		PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<>());
		builder.addStrategyModule(new DiscreteModeChoiceReplanningModule(globalConfigGroup, modeChoiceModelProvider,
				tripListConverterProvider, populationFactory));

		if (dmcConfig.getPerformReroute()) {
			builder.addStrategyModule(new ReRoute(activityFacilities, tripRouterProvider, globalConfigGroup, timeInterpretation));
		} else {
			builder.addStrategyModule(new CheckConsistentRoutingReplanningModule(globalConfigGroup));
		}

		return builder.build();
	}

}
