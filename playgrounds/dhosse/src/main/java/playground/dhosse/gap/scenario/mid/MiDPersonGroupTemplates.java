package playground.dhosse.gap.scenario.mid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.LegImpl;
import org.matsim.core.utils.misc.Time;

public class MiDPersonGroupTemplates {

	Map<String, Map<String, List<MiDTravelChain>>> personGroupId2TravelPattern2TravelChains = new HashMap<>();
	Map<String, Double> personGroupId2TotalWeight = new HashMap<>();
	
	public void setWeights(){
		
		for(Entry<String, Map<String, List<MiDTravelChain>>> entry : this.personGroupId2TravelPattern2TravelChains.entrySet()){
			
			double accumulatedWeight = 0.;
			
			for(Entry<String, List<MiDTravelChain>> entryL2 : entry.getValue().entrySet()){
				
				accumulatedWeight += entryL2.getValue().size();
				
			}
			
			this.personGroupId2TotalWeight.put(entry.getKey(), new Double(accumulatedWeight));
			
		}
		
	}
	
	public void addTravelChainToPattern(String pHash, String pattern, MiDTravelChain tChain){
		
		if(!this.personGroupId2TravelPattern2TravelChains.containsKey(pHash)){
			
			this.personGroupId2TravelPattern2TravelChains.put(pHash, new HashMap<String, List<MiDTravelChain>>());
			this.personGroupId2TravelPattern2TravelChains.get(pHash).put(pattern, new ArrayList<MiDTravelChain>());
			
		} else {
			
			if(!this.personGroupId2TravelPattern2TravelChains.get(pHash).containsKey(pattern)){
				
				this.personGroupId2TravelPattern2TravelChains.get(pHash).put(pattern, new ArrayList<MiDTravelChain>());
				
			}
			
		}
		
		this.personGroupId2TravelPattern2TravelChains.get(pHash).get(pattern).add(tChain);
		
	}
	
	public Map<String,List<MiDTravelChain>> getTravelPatterns(String pHash){
		
		return this.personGroupId2TravelPattern2TravelChains.get(pHash);
		
	}
	
	public Double getWeightForPersonGroupHash(String pHash){
		
		return this.personGroupId2TotalWeight.get(pHash);
		
	}
	
	Map<String, List<MiDSurveyPerson>> personGroups = new HashMap<String, List<MiDSurveyPerson>>();
	
	public void handlePerson(MiDSurveyPerson p){
		
		for(PlanElement pe : p.getPlan().getPlanElements()){
			
			if(pe instanceof Activity){
				
				Activity act = (Activity)pe;
				
				if(act.getType().equals("NULL")){
					return;
				}
				
				if(act.getEndTime() == Time.UNDEFINED_TIME && act.getStartTime() == Time.UNDEFINED_TIME && act.getMaximumDuration() == Time.UNDEFINED_TIME){
					return;
				}
				
			} else{
				
				LegImpl leg = (LegImpl)pe;
				if(Double.isNaN(leg.getRoute().getDistance())){
					
					return;
					
				}
				
			}
			
		}
		
		String hash = p.generateHash();
		
		if(!this.personGroups.containsKey(hash)){
			
			this.personGroups.put(hash, new ArrayList<MiDSurveyPerson>());
			
		}
		
		this.personGroups.get(hash).add(p);
		
	}
	
	public Map<String, List<MiDSurveyPerson>> getPersonGroups(){
		
		return this.personGroups;
		
	}
	
}
