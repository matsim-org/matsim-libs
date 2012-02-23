package playground.anhorni.surprice;

import java.util.List;
import java.util.TreeMap;
import org.matsim.api.core.v01.population.Plan;

public class DecisionModel {
	
	private TreeMap<String, Double> activityFrequenciesPerDayWeek = new TreeMap<String, Double>();
	private TreeMap<String, Double> activityFrequenciesSat = new TreeMap<String, Double>();
	private TreeMap<String, Double> activityFrequenciesSun = new TreeMap<String, Double>();
	private AgentMemory memory;
		
	public void setMemory(AgentMemory memory) {
		this.memory = memory;
	}
		
	public boolean doesAct(String type, String day) {
		int numberOfActsDone = this.memory.getNumberOfActivities(type, day);
		double numberOfActsPlanned = 0.0;
		
		if (day.equals("Sat")) {
			numberOfActsPlanned = this.activityFrequenciesSat.get(type);
		}
		else if (day.equals("Sun")) {
			numberOfActsPlanned = this.activityFrequenciesSun.get(type);
		}
		else {
			numberOfActsPlanned = this.activityFrequenciesPerDayWeek.get(type) * 5.0;
		}	
		if (numberOfActsDone >= numberOfActsPlanned) {
			return false;
		}
		else {
			return true;
		}
	}
	
	public void setFrequency(String type, String day, double frequency) {
		if (day.equals("Sat")) {
			this.activityFrequenciesSat.put(type, frequency);
		}
		else if (day.equals("Sun")) {
			this.activityFrequenciesSun.put(type, frequency);
		}
		else {
			this.activityFrequenciesPerDayWeek.put(type, frequency);
		}
	}
		
	public Plan getPlan(List<Plan> plans, AgentMemory memory) {
		return plans.get(0);
	}
}
