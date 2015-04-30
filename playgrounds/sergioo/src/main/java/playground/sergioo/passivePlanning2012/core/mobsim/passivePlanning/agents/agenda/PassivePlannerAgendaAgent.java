package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.agenda;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import playground.sergioo.passivePlanning2012.api.population.BasePerson;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.PassivePlannerDriverAgent;
import playground.sergioo.passivePlanning2012.core.population.PlacesSharer;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager;

public class PassivePlannerAgendaAgent extends PassivePlannerDriverAgent  {

	//Constructors
	public PassivePlannerAgendaAgent(final BasePerson basePerson, final Netsim simulation, final PassivePlannerManager passivePlannerManager) {
		super(basePerson, simulation, passivePlannerManager);
		planner = new SinglePlannerAgendaAgent(this);
	}
	
	//Methods
	@Override
	public void endActivityAndComputeNextState(double now) {
		Activity prevAct = (Activity)getCurrentPlanElement();
		((SinglePlannerAgendaAgent)planner).shareKnownPlace(prevAct.getFacilityId(), prevAct.getStartTime(), prevAct.getType());
		super.endActivityAndComputeNextState(now);
	}
	public void addKnownPerson(PlacesSharer sharer) {
		((SinglePlannerAgendaAgent)planner).addKnownPerson(sharer);
	}
	public PlacesSharer getPlaceSharer() {
		return ((SinglePlannerAgendaAgent)planner).getPlaceSharer();
	}
}
