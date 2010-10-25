package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.mrieser.core.mobsim.api.AgentSource;
import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.impl.DefaultPlanAgent;

public class FreightAgentTracker implements AgentSource, ActivityEndEventHandler {
	
	private Collection<CarrierImpl> carriers;

	private Collection<CarrierAgent> carrierAgents = new ArrayList<CarrierAgent>();
	
	double weight = 1;

	private PlanAlgorithm router;

	private EventsManager eventsManager;
	
	public FreightAgentTracker(Collection<CarrierImpl> carriers, PlanAlgorithm router, EventsManager eventsManager) {
		this.carriers = carriers;
		this.router = router;
		this.eventsManager = eventsManager;
		createCarrierAgents();
	}

	@Override
	public List<PlanAgent> getAgents() {
		List<PlanAgent> agents = new ArrayList<PlanAgent>();
		for (CarrierAgent carrierAgent : carrierAgents) {
			List<Plan> plans = carrierAgent.createFreightDriverPlans();
			for (Plan plan : plans) {
				PlanAgent planAgent = new DefaultPlanAgent(plan, weight);
				agents.add(planAgent);
			}
		}
		return agents;
	}

	private void createCarrierAgents() {
		for (CarrierImpl carrier : carriers) {
			CarrierAgent carrierAgent = new CarrierAgent(carrier, router);
			carrierAgents.add(carrierAgent);
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		Id personId = event.getPersonId();
		String activityType = event.getActType();
		for (CarrierAgent carrierAgent : carrierAgents) {
			if (carrierAgent.getDriverIds().contains(personId)) {
				carrierAgent.activityEnds(personId, activityType);
			}
		}
	}

	@Override
	public void reset(int iteration) {
		eventsManager.removeHandler(this);
	}
	
	
	
}
