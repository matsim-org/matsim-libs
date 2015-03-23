package playground.wrashid.parkingSearch.withindayFW.core;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.obj.TwoKeyHashMapWithDouble;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;


public class ParkingStrategy {


	private TwoKeyHashMapWithDouble<Id, Integer> score;
	
	private final DuringLegAgentSelector identifier;

	public ParkingStrategy(DuringLegAgentSelector identifier) {
		this.identifier = identifier;
		score=new TwoKeyHashMapWithDouble<Id, Integer>();
	}

	public void putScore(Id agentId, int legPlanElementIndex, double score){
		this.score.put(agentId, legPlanElementIndex, score);
	}
	
	public Double getScore(Id agentId, int legPlanElementIndex){
		return score.get(agentId, legPlanElementIndex);
	}
	
	public void removeScore(Id agentId, int legPlanElementIndex){
		this.score.get(agentId).remove(legPlanElementIndex);
	}

	public DuringLegAgentSelector getIdentifier() {
		return identifier;
	}
	
	
}
