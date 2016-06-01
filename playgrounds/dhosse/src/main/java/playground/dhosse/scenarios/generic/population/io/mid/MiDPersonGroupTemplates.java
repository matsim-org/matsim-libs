package playground.dhosse.scenarios.generic.population.io.mid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MiDPersonGroupTemplates {

	Map<String, Map<String, List<MiDTravelChain>>> personGroupId2TravelPattern2TravelChains = new HashMap<>();
	Map<String, Double> personGroupId2TotalWeight = new HashMap<>();
	Map<String, Double> pattern2TotalWeight = new HashMap<>();
	
	public void setWeights(){
		
		for(Entry<String, Map<String, List<MiDTravelChain>>> entry : this.personGroupId2TravelPattern2TravelChains.entrySet()){
			
			double accumulatedWeight = 0.;
			
			for(Entry<String, List<MiDTravelChain>> chains : entry.getValue().entrySet()){
				
				double w = 0.;
				
				for(MiDTravelChain chain : chains.getValue()){
				
					w+= chain.getWeight();
				
				}
				
				this.pattern2TotalWeight.put(chains.getKey(), w);
				accumulatedWeight += w;
				
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
	
	public double getWeightForTravelPattern(String pattern){
		return this.pattern2TotalWeight.get(pattern);
	}
	
	public Map<String,Map<String,List<MiDTravelChain>>> getPersonGroupId2TravelPatterns(){
		return this.personGroupId2TravelPattern2TravelChains;
	}
	
	public Map<String,List<MiDTravelChain>> getTravelPatterns(String pHash){
		
		return this.personGroupId2TravelPattern2TravelChains.get(pHash);
		
	}
	
	public Double getWeightForPersonGroupHash(String pHash){
		
		return this.personGroupId2TotalWeight.get(pHash);
		
	}
	
	Map<String, List<MiDPerson>> personGroups = new HashMap<String, List<MiDPerson>>();
	
//	public void handlePerson(MiDPerson p){
//		
//		for(PlanElement pe : p.getPlan().getPlanElements()){
//			
//			if(pe instanceof Activity){
//				
//				Activity act = (Activity)pe;
//				
//				if(act.getType().equals("NULL")){
//					return;
//				}
//				
//				if(act.getEndTime() == Time.UNDEFINED_TIME && act.getStartTime() == Time.UNDEFINED_TIME && act.getMaximumDuration() == Time.UNDEFINED_TIME){
//					return;
//				}
//				
//			} else{
//				
//				LegImpl leg = (LegImpl)pe;
//				if(Double.isNaN(leg.getRoute().getDistance())){
//					
//					return;
//					
//				}
//				
//			}
//			
//		}
//		
//		String hash = p.generateHash();
//		
//		if(!this.personGroups.containsKey(hash)){
//			
//			this.personGroups.put(hash, new ArrayList<MiDPerson>());
//			
//		}
//		
//		this.personGroups.get(hash).add(p);
//		
//	}
	
	public Map<String, List<MiDPerson>> getPersonGroups(){
		
		return this.personGroups;
		
	}
	
}
