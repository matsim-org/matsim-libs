package playground.wrashid.sschieffer.DecentralizedSmartCharger;

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
	public static LinkedListValueHashMap<Id, ContractTypeAgent>  makeAgentContracts(
			Controler controler,
			double xPercentNone,
			double xPercentDown,
			double xPercentDownUp			
			){
		LinkedListValueHashMap<Id, ContractTypeAgent> list = new LinkedListValueHashMap<Id, ContractTypeAgent>();
		for(Id id : controler.getPopulation().getPersons().keySet()){
			
			double rand= Math.random();
			if(rand<xPercentNone){
				list.put(id, contractNoRegulation);
			}else{
				if(rand<xPercentNone+xPercentDown){
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
	public static LinkedListValueHashMap<Id, ContractTypeAgent>  makeAgentContractsEVPHEV(
			Controler controler,
			double xPercentNoneEV,
			double xPercentDownEV,
			double xPercentDownUpEV,
			double xPercentNonePHEV,
			double xPercentDownPHEV,
			double xPercentDownUpPHEV
			){
		LinkedListValueHashMap<Id, ContractTypeAgent> list = new LinkedListValueHashMap<Id, ContractTypeAgent>();
		for(Id id : controler.getPopulation().getPersons().keySet()){
			
			if(dsc.hasAgentPHEV(id) ){
				double rand= Math.random();
				if(rand<xPercentNonePHEV){
					list.put(id, contractNoRegulation);
				}else{
					if(rand<xPercentNonePHEV+xPercentDownPHEV){
						list.put(id, contractRegDown);
					}else{
						list.put(id, contractRegUpRegDown);
					}
				}
				
			}
			if(dsc.hasAgentEV(id)){
				double rand= Math.random();
				if(rand<xPercentNoneEV){
					list.put(id, contractNoRegulation);
				}else{
					if(rand<xPercentNoneEV+xPercentDownEV){
						list.put(id, contractRegDown);
					}else{
						list.put(id, contractRegUpRegDown);
					}
				}
			}
			
		}
		return list;
	}
	
}
