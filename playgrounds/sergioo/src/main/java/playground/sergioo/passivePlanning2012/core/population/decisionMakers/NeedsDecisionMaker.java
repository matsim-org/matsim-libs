package playground.sergioo.passivePlanning2012.core.population.decisionMakers;

import java.util.HashMap;
import java.util.Map;

import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.TypeOfActivityDecisionMaker;


public class NeedsDecisionMaker implements TypeOfActivityDecisionMaker {

	//Attributes
	private final Map<NeedType, Double> needs = new HashMap<NeedType, Double>();
	
	//Methods
	public void putNeed(NeedType needType, Double value) {
		needs.put(needType, value);
	}
	public Map<NeedType, Double> getNeeds() {
		return needs;
	}
	@Override
	public String decideTypeOfActivity(double time) {
		return null;
	}
	
}
