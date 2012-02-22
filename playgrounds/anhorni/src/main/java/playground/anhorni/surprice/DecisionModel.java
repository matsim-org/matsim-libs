package playground.anhorni.surprice;

import java.util.List;
import java.util.TreeMap;

import org.matsim.api.core.v01.population.Plan;

import playground.anhorni.surprice.preprocess.ActFrequencyModel;

public class DecisionModel {
	
	private TreeMap<String, ActFrequencyModel> activityFrequencies = new TreeMap<String, ActFrequencyModel>();
	private AgentMemory memory;
		
	public void setMemory(AgentMemory memory) {
		this.memory = memory;
	}
		
	public boolean doesAct(String type, String day) {
		return true;
	}
	
	
	public Plan getPlan(List<Plan> plans, AgentMemory memory) {
		return plans.get(0);
	}
	
	public ModesBetas getModesBetas(AgentMemory memory) {
		return new ModesBetas();
	}
	
	public double getWeekFrequency(String type) {
		return this.activityFrequencies.get(type).getWeekFrequency();
	}
}
