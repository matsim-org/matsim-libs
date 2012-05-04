package playground.sergioo.passivePlanning.core.population.decisionMakers;

import java.util.HashMap;
import java.util.Map;

import playground.sergioo.passivePlanning.core.population.decisionMakers.types.TypeOfActivityDecisionMaker;


public class NeedsDecisionMaker implements TypeOfActivityDecisionMaker {

	//Attributes
	private final Map<NeedType, Double> needs = new HashMap<NeedType, Double>();
	private double time;
	
	//Methods
	public void putNeed(NeedType needType, Double value) {
		needs.put(needType, value);
	}
	public String decideTypeOfActivity() {
		return null;
	}
	public Map<NeedType, Double> getNeeds() {
		return needs;
	}
	@Override
	public double getTime() {
		return time;
	}
	@Override
	public void setTime(double time) {
		this.time = time;
	}
	
}
