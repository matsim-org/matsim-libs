package playground.anhorni.surprice;

import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

public class DecisionModels {
	
	private TreeMap<Id, DecisionModel> decisionModels = new TreeMap<Id, DecisionModel>();
	
	public DecisionModel getDecisionModelForAgent(Id agentId) {
		return this.decisionModels.get(agentId);
	}
	
	public void addDecsionModelForAgent(DecisionModel decisionModel, Id agentId) {
		this.decisionModels.put(agentId, decisionModel);
	}
}
