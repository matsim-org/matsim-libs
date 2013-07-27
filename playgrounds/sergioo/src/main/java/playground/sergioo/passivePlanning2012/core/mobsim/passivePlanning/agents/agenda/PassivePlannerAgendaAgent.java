package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.agenda;

import java.util.Collection;
import java.util.Set;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import playground.sergioo.passivePlanning2012.api.population.BasePerson;
import playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.agents.PassivePlannerDriverAgent;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager;

public class PassivePlannerAgendaAgent extends PassivePlannerDriverAgent  {

	//Constructors
	public PassivePlannerAgendaAgent(final BasePerson basePerson, final Netsim simulation, final PassivePlannerManager passivePlannerManager, Set<String> modes) {
		super(basePerson, simulation, passivePlannerManager);
		boolean carAvailability = false;
		Collection<String> mainModes = simulation.getScenario().getConfig().getQSimConfigGroup().getMainMode();
		for(PlanElement planElement:basePerson.getBasePlan().getPlanElements())
			if(planElement instanceof Leg)
				if(mainModes.contains(((Leg)planElement).getMode()))
					carAvailability = true;
		planner = new SinglePlannerAgendaAgent(simulation.getScenario(), carAvailability, modes, basePerson.getBasePlan(), this);
		planner.setPlanElementIndex(0);
	}
	
	//Methods
	@Override
	public void endActivityAndComputeNextState(double now) {
		Activity prevAct = (Activity)getCurrentPlanElement();
		((SinglePlannerAgendaAgent)planner).shareKnownPlace(prevAct.getFacilityId(), prevAct.getStartTime(), prevAct.getType());
		super.endActivityAndComputeNextState(now);
	}
	
}
