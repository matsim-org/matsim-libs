package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;

import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class AgentContractCollector {

	private static DecentralizedSmartCharger dsc;
	
	 private static ContractTypeAgent contractRegUpRegDown;
	 
	 private static ContractTypeAgent contractRegDown;
	 
	 private static ContractTypeAgent contractNoRegulation;
	 
	 public AgentContractCollector (DecentralizedSmartCharger dsc,
			 double  compensationPerKWHRegulationUp,
			 double compensationPerKWHRegulationDown){
		 this.dsc=dsc;
		 
		 contractRegUpRegDown= new ContractTypeAgent(
					true, // up
					true, //down
					compensationPerKWHRegulationUp,
					compensationPerKWHRegulationDown);
		 
		contractRegDown= new ContractTypeAgent(
					false,
					true, 
					0,
					compensationPerKWHRegulationDown);
		 
		  contractNoRegulation= new ContractTypeAgent(
					false, // up
					false, //down
					0,
					0);
			
	 }
	
	
	
	
	/*
	 * RETURNS LinkedListValueHashMap<Id, ContractTypeAgent> with given percentage of population with
	 * no regulation, only regulation down or regulation up and down
	 */
	public HashMap<Id, ContractTypeAgent>  makeAgentContracts(
			Controler controler,
			double xPercentNone,
			double xPercentDown,
			double xPercentDownUp			
			){
		
		int popSize= dsc.vehicles.getKeySet().size();
		int count = 0;
		
		HashMap<Id, ContractTypeAgent> list = new HashMap<Id, ContractTypeAgent>();
		for(Id id : dsc.vehicles.getKeySet()){
			if (count< popSize*xPercentNone){
			
				list.put(id, contractNoRegulation);
			}else{
				if(count< popSize*(xPercentNone+xPercentDown)){
					list.put(id, contractRegDown);
				}else{
					list.put(id, contractRegUpRegDown);
				}
			}
		}
		return list;
	}
	
	
	
	/*
	 * RETURNS LinkedListValueHashMap<Id, ContractTypeAgent> with given percentage of population with
	 * no regulation, only regulation down or regulation up and down
	 */
	public static HashMap<Id, ContractTypeAgent>  makeAgentContractsEVPHEV(
			Controler controler,
			double xPercentNoneEV,
			double xPercentDownEV,
			double xPercentDownUpEV,
			double xPercentNonePHEV,
			double xPercentDownPHEV,
			double xPercentDownUpPHEV
			){
		
		int popSize= dsc.vehicles.getKeySet().size();
		int countEV = 0;
		int countPHEV = 0;
		HashMap<Id, ContractTypeAgent> list = new HashMap<Id, ContractTypeAgent>();
		for(Id id : dsc.vehicles.getKeySet()){
			
			if(dsc.hasAgentPHEV(id) ){
				if (countPHEV< popSize*xPercentNonePHEV){				
					list.put(id, contractNoRegulation);
				}else{
					if(countPHEV< popSize*(xPercentNonePHEV+xPercentDownPHEV)){
						list.put(id, contractRegDown);
					}else{
						list.put(id, contractRegUpRegDown);
					}
				}
				countPHEV++;
			}
			
			if(dsc.hasAgentEV(id)){
				if (countEV< popSize*xPercentNoneEV){
					list.put(id, contractNoRegulation);
				}else{
					if(countEV< popSize*(xPercentNoneEV+xPercentDownEV)){
						list.put(id, contractRegDown);
					}else{
						list.put(id, contractRegUpRegDown);
					}
				}
				countEV++;
			}
			
		}
		return list;
	}
	
}
