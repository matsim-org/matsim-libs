package besttimeresponseintegration;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;

/**
 * 
 * @author Gunnar Flötteröd, based on MATSim example code
 *
 */
class BestTimeResponseStrategyFactory implements Provider<PlanStrategy> {
	
	private final Scenario scenario;

	@Inject
	BestTimeResponseStrategyFactory(final Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public PlanStrategy get() {

		final BestTimeResponsePlanSelector planSelector = new BestTimeResponsePlanSelector();
		PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(planSelector);

		final BestTimeResponseStrategyModule module = new BestTimeResponseStrategyModule(
				this.scenario, null, null);
		builder.addStrategyModule(module);

		return builder.build();
	}
}
