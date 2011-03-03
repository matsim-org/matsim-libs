package playground.fhuelsmann.emission;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;

public class ColdstartAnalyseModul implements AnalysisModuleCold{

	
	
		//private Map<Id, double[]> coldEmissionsLink = new TreeMap<Id,double[]>();
	public Map<Id, Map<String,Double>> coldEmissionsPerson = new TreeMap<Id, Map<String,Double>>();
	
	
	public void calculateColdEmissionsPerLink(Id personId, double actDuration,double distance, HbefaColdTable hbefaColdTable) {
						

			int dis=-1;
			
			if ((distance/1000) <1.0  ) {
				dis =0;
			}
			else {
				dis=1;
			}
			
			int time =(int)(actDuration/3600);
			if (time>12) time =12; 
			
			for(Entry<String,Map<Integer,Map<Integer,HbefaColdObject>>> component : hbefaColdTable.getHbefaColdTable().entrySet()){
			
				double coldEf = hbefaColdTable.getHbefaColdTable().get(component.getKey()).get(dis).get(time).getColdEF();	
				
				if(this.coldEmissionsPerson.get(personId) == null){
					
					 Map<String,Double> tempMap = new TreeMap<String,Double>();
					 tempMap.put(component.getKey(), coldEf);
					 this.coldEmissionsPerson.put(personId, tempMap);
					 
				}else {
						this.coldEmissionsPerson.get(personId).put(component.getKey(), coldEf);

				}
			}
		}
	
	public void printColdEmissions(){
		
		
		for(Entry<Id,Map<String,Double>> personId : this.coldEmissionsPerson.entrySet()){
			for(Entry<String,Double> component : this.coldEmissionsPerson.get(personId.getKey()).entrySet()){
			System.out.println( personId.getKey() + "\t  " + component.getKey() + " \t " +component.getValue() + "\n" );
			}
		}	
	}

}