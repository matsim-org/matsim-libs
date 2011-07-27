package playground.fhuelsmann.emission;


import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;

import playground.fhuelsmann.emission.objects.HbefaColdObject;

public class ColdEmissionAnalysisModule implements AnalysisModuleCold{


	public Map<Id, Map<String,Double>> coldEmissionsPerson = new TreeMap<Id, Map<String,Double>>();

	public void calculateColdEmissionsPerLink(Id personId, double actDuration,double distance, HbefaColdEmissionTable hbefaColdTable) {

		int distance_km=-1;
		if ((distance/1000) <1.0  ) {
			distance_km =0;
		}
		else {
			distance_km=1;
		}

		int vehicleParkingTime_h =(int)(actDuration/3600);
		if (vehicleParkingTime_h>12) vehicleParkingTime_h =12; 

		int nightTime = 12;
		int initDis =1;
	
		for(Entry<String,Map<Integer,Map<Integer,HbefaColdObject>>> component : hbefaColdTable.getHbefaColdTable().entrySet()){

			double coldEfOtherAct = hbefaColdTable.getHbefaColdTable().get(component.getKey()).get(distance_km).get(vehicleParkingTime_h).getColdEF();	

			if(this.coldEmissionsPerson.get(personId) == null){
			
				Map<String,Double> tempMap = new TreeMap<String,Double>();
				this.coldEmissionsPerson.put(personId,tempMap);
			}
			
			if (!this.coldEmissionsPerson.get(personId).containsKey(component.getKey())) {
			
					double coldEfNight=0.0;					
					
					if(!personId.toString().contains("gv_")){ 
					coldEfNight = hbefaColdTable.getHbefaColdTable().get(component.getKey()).get(initDis).get(nightTime).getColdEF();}
					
				this.coldEmissionsPerson.get(personId).put(component.getKey(), coldEfNight + coldEfOtherAct );
	
			}else if(this.coldEmissionsPerson.get(personId).containsKey(component.getKey())){
					double oldValue = this.coldEmissionsPerson.get(personId).get(component.getKey());
					this.coldEmissionsPerson.get(personId).put(component.getKey(),oldValue+coldEfOtherAct );
			}			 			 
		}
	}
	
	public void calculatePerPersonPtBikeWalk(Id personId, Id linkId,HbefaColdEmissionTable hbefaColdTable){
		
		for(Entry<String,Map<Integer,Map<Integer,HbefaColdObject>>> component : hbefaColdTable.getHbefaColdTable().entrySet()){
	
			double coldEmissions =0.0;
			// just to be sure that there is an object which can be called  later
			if(this.coldEmissionsPerson.get(personId) == null){
				Map<String,Double> tempMap = new TreeMap<String,Double>();
				this.coldEmissionsPerson.put(personId,tempMap);}

			// subMap in map,should be modified
			Map<String,Double> oldMap = this.coldEmissionsPerson.get(personId);
			if (oldMap.get(component.getKey().toString()) == null){
				oldMap.put(component.getKey().toString(), coldEmissions);
				this.coldEmissionsPerson.put(personId,oldMap);}
			
			else {//do nothing
				}
			
//			//TODO: CO2 not directly available for cold emissions; thus it could be calculated through fc as follows:
//			get("FC")*0.865 - get("CO")*0.429 - get("HC")*0.866)/0.273;
			
		}
	}

	public Map<Id, Map<String, Double>> getColdEmissionsPerPerson() {
		return coldEmissionsPerson;
	}

	public void setColdEmissionsPerson(
			Map<Id, Map<String, Double>> coldEmissionsPerson) {
		this.coldEmissionsPerson = coldEmissionsPerson;
	}
}
