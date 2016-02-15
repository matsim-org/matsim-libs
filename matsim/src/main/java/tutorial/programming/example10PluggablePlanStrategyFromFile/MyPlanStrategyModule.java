package tutorial.programming.example10PluggablePlanStrategyFromFile;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.ReplanningContext;

class MyPlanStrategyModule implements PlanStrategyModule, ActivityEndEventHandler {
  	private static final Logger log = Logger.getLogger(MyPlanStrategyModule.class);

	Network network;
	Population population;

	MyPlanStrategyModule(Network network, Population population) {
		this.network = network;
		this.population = population;
	}

	@Override
	public void finishReplanning() {
	}

	@Override
	public void handlePlan(Plan plan) {
		log.error("calling handlePlan");
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		log.error("calling handleEvent for an ActivityEndEvent");
	}

	@Override
	public void reset(int iteration) {
		log.error("calling reset");
	}

}
