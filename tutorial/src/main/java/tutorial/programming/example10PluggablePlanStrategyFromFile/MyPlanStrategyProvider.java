package tutorial.programming.example10PluggablePlanStrategyFromFile;


import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;

import javax.inject.Inject;
import javax.inject.Provider;

class MyPlanStrategyProvider implements Provider<PlanStrategy> {

	@Inject
	Network network;

	@Inject
	Population population;

	@Inject
	EventsManager eventsManager;

	@Override
	public PlanStrategy get() {
		// A PlanStrategy is something that can be applied to a Person (not a Plan).
		// Define how to select between existing plans:
		PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new MyPlanSelector());

		// if you just want to select plans, you can stop here (except for the builder.build() below).


		// Otherwise, to do something with that plan, one needs to add modules into the strategy.  If there is at least
		// one module added here, then the plan is copied and then modified.
		MyPlanStrategyModule mod = new MyPlanStrategyModule(network, population);
		builder.addStrategyModule(mod);

		// these modules may, at the same time, be events listeners (so that they can collect information):
		eventsManager.addHandler(mod);

		return builder.build();
	}

}
