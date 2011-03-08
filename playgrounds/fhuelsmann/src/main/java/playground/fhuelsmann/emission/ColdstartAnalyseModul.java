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
			
			int nightTime = 12;
			int initDis =1;
			
			for(Entry<String,Map<Integer,Map<Integer,HbefaColdObject>>> component : hbefaColdTable.getHbefaColdTable().entrySet()){
			
				double coldEfOtherAct = hbefaColdTable.getHbefaColdTable().get(component.getKey()).get(dis).get(time).getColdEF();	
				
				double coldEfNight = hbefaColdTable.getHbefaColdTable().get(component.getKey()).get(initDis).get(nightTime).getColdEF();
				
				if(this.coldEmissionsPerson.get(personId) == null){
					
					 Map<String,Double> tempMap = new TreeMap<String,Double>();
					 tempMap.put(component.getKey(), coldEfOtherAct+coldEfNight);
					 this.coldEmissionsPerson.put(personId, tempMap);
					 
				}else{
					
					if (this.coldEmissionsPerson.get(personId).containsKey(component.getKey())) {
						
						double oldValue = this.coldEmissionsPerson.get(personId).get(component.getKey());
						this.coldEmissionsPerson.get(personId).put(component.getKey(), oldValue+coldEfOtherAct);
						
//						String id = personId.toString();
//						if(id.contains("569253.3#11147"))	
//						System.out.println("component" + component.getKey()+"coldEfOtherAct " +coldEfOtherAct + " coldEfNight " + coldEfNight);
						
					} else this.coldEmissionsPerson.get(personId).put(component.getKey(), coldEfOtherAct);
				}
			}
	}
	
	
	
public void printColdEmissions(String outputFile){
		
	try{
		FileWriter fstream = new FileWriter(outputFile);			
		BufferedWriter out = new BufferedWriter(fstream);
		out.write("Id \t AirPollutant \t ColdStartEmissionen \n"); 
		
		for(Entry<Id,Map<String,Double>> personId : this.coldEmissionsPerson.entrySet()){
			for(Entry<String,Double> component : this.coldEmissionsPerson.get(personId.getKey()).entrySet()){
		//	System.out.println( personId.getKey() + "\t  " + component.getKey() + " \t " +component.getValue() + "\n" );
			out.append(personId.getKey() + "\t  " + component.getKey() + " \t " +component.getValue() + "\n" );
			}
		}	
	//Close the output stream
			out.close();
			System.out.println("Finished writing emission file to " + outputFile);
			}catch (Exception e){
					System.err.println("Error: " + e.getMessage());
			}
	}//printColdEmissions
}//Class
