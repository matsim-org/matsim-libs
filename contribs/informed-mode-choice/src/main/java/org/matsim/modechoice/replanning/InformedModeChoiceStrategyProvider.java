package org.matsim.modechoice.replanning;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.modechoice.search.TopKChoicesGenerator;

import javax.inject.Inject;

/**
 * Provider for {@link InformedModeChoicePlanStrategy}.
 */
public class InformedModeChoiceStrategyProvider implements Provider<PlanStrategy> {

	@Inject
	private Config config;
	@Inject
	private Scenario scenario;
	@Inject
	private Provider<TopKChoicesGenerator> generator;
	@Inject
	private Provider<TripRouter> tripRouter;
	@Inject
	private OutputDirectoryHierarchy controlerIO;
	@Inject
	private ActivityFacilities facilities;
	@Inject
	private TimeInterpretation timeInterpretation;

	@Override
	public PlanStrategy get() {
		return new InformedModeChoicePlanStrategy(config, scenario, controlerIO, generator, tripRouter, facilities, timeInterpretation);
	}
}
