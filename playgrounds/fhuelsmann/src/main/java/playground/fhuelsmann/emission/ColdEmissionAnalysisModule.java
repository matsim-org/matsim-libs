package playground.fhuelsmann.emission;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;

import playground.fhuelsmann.emission.objects.HbefaColdObject;

public class ColdEmissionAnalysisModule implements AnalysisModuleCold{


	public Map<Id, Map<String,Double>> coldEmissionsPerson = new TreeMap<Id, Map<String,Double>>();

	public void calculateColdEmissionsPerLink(Id personId, double actDuration,double distance, HbefaColdEmissionTable hbefaColdTable) {


		int dis=-1;

		if ((distance/1000) <1.0  ) {
			dis =0;
		}
		else {
			dis=1;
		}

		int time =(int)(actDuration/3600);
		if (time>12) time =12; 

		int nightTime = 12;
		int initDis =1;
	
		for(Entry<String,Map<Integer,Map<Integer,HbefaColdObject>>> component : hbefaColdTable.getHbefaColdTable().entrySet()){
			
			
			double coldEfOtherAct = hbefaColdTable.getHbefaColdTable().get(component.getKey()).get(dis).get(time).getColdEF();	
			
			if(this.coldEmissionsPerson.get(personId) == null){
			
				Map<String,Double> tempMap = new TreeMap<String,Double>();
				this.coldEmissionsPerson.put(personId,tempMap);}
		
			if (!this.coldEmissionsPerson.get(personId).containsKey(component.getKey())) {
			
					double coldEfNight=0.0;	
					
					if(personId.toString().contains("pv_pt_")){ 
						coldEfOtherAct =0.0;}
				
					
					if(!personId.toString().contains("gv_")&& !personId.toString().contains("pv_pt_")){ 
					coldEfNight = hbefaColdTable.getHbefaColdTable().get(component.getKey()).get(initDis).get(nightTime).getColdEF();}
					
				this.coldEmissionsPerson.get(personId).put(component.getKey(), coldEfNight + coldEfOtherAct );
			
			}else if(this.coldEmissionsPerson.get(personId).containsKey(component.getKey())){
			
				if(personId.toString().contains("pv_pt_")){
					coldEfOtherAct =0.0;}
			
					double oldValue = this.coldEmissionsPerson.get(personId).get(component.getKey());
					this.coldEmissionsPerson.get(personId).put(component.getKey(),oldValue+coldEfOtherAct );
					
	//				System.out.print("\n in the else : "+ personId + " component "+component.getKey()+ " Value "+ 
	//						this.coldEmissionsPerson.get(personId).get(component.getKey()) +" oldValue " + oldValue 
	//						+" coldEfOtherAct " + coldEfOtherAct + " new Value : " + this.coldEmissionsPerson.get(personId).get(component.getKey()) );
				}			 			 
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
