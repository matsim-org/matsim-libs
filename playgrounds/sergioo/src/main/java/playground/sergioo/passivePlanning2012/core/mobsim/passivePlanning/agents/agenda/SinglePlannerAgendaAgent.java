package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.agenda;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.facilities.ActivityFacility;

import playground.sergioo.passivePlanning2012.api.population.AgendaBasePerson;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.PassivePlannerDriverAgent;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.SinglePlannerAgentImpl;
import playground.sergioo.passivePlanning2012.core.population.PlaceSharer;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.AgendaDecisionMaker;
import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.DecisionMaker;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager.CurrentTime;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager.MobsimStatus;

public class SinglePlannerAgendaAgent extends SinglePlannerAgentImpl {

	public SinglePlannerAgendaAgent(PassivePlannerDriverAgent agent) {
		super(new DecisionMaker[]{((AgendaBasePerson)agent.getBasePerson()).getAgendaDecisionMaker()}, agent);
	}
	
	@Override
	protected List<? extends PlanElement> getLegActivityLeg(double startTime, CurrentTime now,
			Id<ActivityFacility> startFacilityId, double endTime, Id<ActivityFacility> endFacilityId, final MobsimStatus mobsimStatus) {
		((AgendaDecisionMaker)decisionMakers[0]).prepareScheduling((Activity) agent.getCurrentPlan().getPlanElements().get(currentElementIndex.get()-1), agent.getCurrentPlan(), now);
		((AgendaDecisionMaker)decisionMakers[0]).setMobsimEnds(mobsimStatus);
		return ((AgendaDecisionMaker)decisionMakers[0]).decideRoute(startTime, startFacilityId, endFacilityId, null, tripRouter);
	}
	public void addKnownPerson(PlaceSharer sharer) {
		((PlaceSharer)decisionMakers[0]).addKnownPerson(sharer);
	}
	public void shareKnownPlace(Id<ActivityFacility> facilityId, double startTime, String type) {
		PlaceSharer sharer = ((PlaceSharer)decisionMakers[0]);
		if(!type.equals("home"))
			sharer.shareKnownPlace(facilityId, startTime, type);
	}
	public PlaceSharer getPlaceSharer() {
		return (PlaceSharer)decisionMakers[0];
	}

	public void shareKnownTravelTime(Id<ActivityFacility> prevFacilityId, Id<ActivityFacility> facilityId, String mode, double startTime, double travelTime) {
		((PlaceSharer)decisionMakers[0]).shareKnownTravelTime(prevFacilityId, facilityId, mode, startTime, travelTime);
	}

}
