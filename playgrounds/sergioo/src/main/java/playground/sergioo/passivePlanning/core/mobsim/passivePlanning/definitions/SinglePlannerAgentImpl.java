package playground.sergioo.passivePlanning.core.mobsim.passivePlanning.definitions;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.utils.collections.Tuple;

import playground.sergioo.passivePlanning.api.population.EmptyActivity;
import playground.sergioo.passivePlanning.core.population.EmptyActivityImpl;
import playground.sergioo.passivePlanning.core.population.PlanImplV2;
import playground.sergioo.passivePlanning.core.population.decisionMakers.types.DecisionMaker;

public abstract class SinglePlannerAgentImpl implements SinglePlannerAgent {

	//Attributes
	protected final DecisionMaker[] decisionMakers;
	protected int currentElementIndex;
	protected final Plan plan;

	//Constructors
	public SinglePlannerAgentImpl(DecisionMaker[] decisionMakers, Plan plan) {
		this.decisionMakers = decisionMakers;
		this.plan = plan;
	}

	//Methods
	@Override
	public Plan getPlan() {
		return plan;
	}
	@Override
	public void setTime(double time) {
		for(DecisionMaker decisionMaker:decisionMakers)
			decisionMaker.setTime(time);
		currentElementIndex = ((PlanImplV2)plan).getPlanElementIndex(time);
	}
	@Override
	public boolean isPlanned() {
		return !(plan.getPlanElements().get(currentElementIndex) instanceof EmptyActivity);
	}
	@Override
	public boolean planLegActivity() {
		Tuple<Leg, Activity> legAct = getLegActivity();
		if(legAct == null)
			return false;
		else {
			double time = decisionMakers[0].getTime();
			EmptyActivity old = (EmptyActivity) plan.getPlanElements().remove(currentElementIndex);
			if(legAct.getFirst().getDepartureTime()>time+1) {
				Activity empty = new EmptyActivityImpl();
				empty.setStartTime(time+1);
				empty.setEndTime(legAct.getFirst().getDepartureTime()-1);
				plan.getPlanElements().add(currentElementIndex, empty);
				currentElementIndex++;
			}
			plan.getPlanElements().add(currentElementIndex, legAct.getFirst());
			currentElementIndex++;
			plan.getPlanElements().add(currentElementIndex, legAct.getSecond());
			currentElementIndex++;
			if(legAct.getSecond().getEndTime()<old.getEndTime()) {
				Activity empty = new EmptyActivityImpl();
				empty.setStartTime(legAct.getSecond().getEndTime()+1);
				empty.setEndTime(old.getEndTime());
				plan.getPlanElements().add(currentElementIndex, empty);
				currentElementIndex++;
			}
			return true;
		}
	}
	protected abstract Tuple<Leg, Activity> getLegActivity();
	
}
