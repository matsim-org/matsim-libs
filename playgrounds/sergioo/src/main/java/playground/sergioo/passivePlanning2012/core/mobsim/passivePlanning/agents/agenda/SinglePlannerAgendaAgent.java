package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.agenda;

import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.PassivePlannerDriverAgent;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.definitions.SinglePlannerAgentImpl;
import playground.sergioo.passivePlanning2012.core.population.agenda.Agenda;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.AgendaDecisionMaker;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.DecisionMaker;

public class SinglePlannerAgendaAgent extends SinglePlannerAgentImpl {

	public SinglePlannerAgendaAgent(Scenario scenario, boolean carAvailability, Set<String> modes, Plan plan, PassivePlannerDriverAgent agent, Agenda agenda) {
		super(new DecisionMaker[]{new AgendaDecisionMaker(scenario, carAvailability, modes, plan, agenda)}, plan, agent);
	}

	@Override
	protected List<? extends PlanElement> getLegActivityLeg(double startTime,
			Id startFacilityId, double endTime, Id endFacilityId) {
		((AgendaDecisionMaker)decisionMakers[0]).setLastActivity((Activity) plan.getPlanElements().get(currentElementIndex.get()-1));
		return ((AgendaDecisionMaker)decisionMakers[0]).decideRoute(Double.NaN, startFacilityId, endFacilityId, null, tripRouter);
	}
	public void shareKnownPlace(Id facilityId, double startTime, String type) {
		((AgendaDecisionMaker)decisionMakers[0]).shareKnownPlace(facilityId, startTime, type);
	}

}
