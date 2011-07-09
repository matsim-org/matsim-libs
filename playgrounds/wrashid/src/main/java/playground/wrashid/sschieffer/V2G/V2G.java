package playground.wrashid.sschieffer.V2G;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import lpsolve.LpSolveException;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.OptimizationException;
import org.matsim.api.core.v01.Id;

import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.sschieffer.DSC.DecentralizedSmartCharger;
import playground.wrashid.sschieffer.DSC.LP.LPEV;
import playground.wrashid.sschieffer.DSC.LP.LPPHEV;
import playground.wrashid.sschieffer.SetUp.ElectricitySourceDefinition.GeneralSource;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.ChargingInterval;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.DrivingInterval;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.LoadDistributionInterval;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.ParkingInterval;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.Schedule;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.TimeInterval;


/**
 * this class handles regulation up and down. For every agent, it calculates, if rescheduling or keeping its current schedule is more profitable.
 * If rescheduling has a higher utility for the agent, he reschedules the rest of his day and decreases the stochastic hub load 
 * 
 * <ul>
 * <li> reschedule according to V2G loads if possible
 * <li> save revenues from V2G from every agent in LinkedList agentV2GRevenue
 * </ul>
 * 
 * @author Stella
 *
 */
public class V2G {
	
	private DecentralizedSmartCharger mySmartCharger;
	//private LinkedListValueHashMap<Id, Double> agentV2GRevenue = new LinkedListValueHashMap<Id, Double>(); 
	private HashMap<Id, V2GAgentStats> agentV2GStatistic; 
	private HashMap<Id, V2GAgentStats> hubSourceStatistic;
	
	private ArrayList<V2GDecisionMonitor> V2GdecisionsList;
	
	public Schedule answerScheduleAfterElectricSourceInterval;
	private double averageV2GRevenueEV ;
	private double averageV2GRevenuePHEV ;
	private double averageV2GRevenueAllAgents ;
	
	private double averageExtraChargingCostEV ;
	private double averageExtraChargingCostPHEV ;
	private double averageExtraChargingCostAllAgents;
	
	private double averageJoulesSavedByLocalProductionEV ;
	private double averageJoulesSavedByLocalProductionPHEV ;
	private double averageJoulesSavedByLocalProductionAllAgents;
	
	private double averageJoulesTakenFromBatteryForExtraConsumptionEV ;
	private double averageJoulesTakenFromBatteryForExtraConsumptionPHEV ;
	private double averageJoulesTakenFromBatteryForExtraConsumptionAllAgents;
	
	private double totalRegulationUp ;
	private double totalRegulationUpEV ;
	private double totalRegulationUpPHEV ;
	
	private double totalRegulationDown ;
	private double totalRegulationDownEV ;
	private double totalRegulationDownPHEV ;
	
	private double totalCompensationEV ;
	private double totalCompensationPHEV ;
	
	private double totalMoneySavedReschedulingEV ;
	private double totalMoneySavedReschedulingPHEV ;
	
	private double averageRevenueFeedInEV ;
	private double averageRevenueFeedInPHEV ;
	private double averageRevenueFeedInAllAgents ;
	
	private double totalJoulesFeedInEV ;
	private double totalJoulesFeedInPHEV ;
	private double totalJoulesFeedInAllAgents ;
	
	private double totalJoulesFeedInHubSources;
	private double averageRevenueFeedInHubSources ;
	private double averageExtraChargingHubSources ;
	
	public V2G(DecentralizedSmartCharger mySmartCharger){
		this.mySmartCharger=mySmartCharger;
		 
		V2GdecisionsList= new ArrayList<V2GDecisionMonitor>();
		
		averageV2GRevenueEV=0;
		 averageV2GRevenuePHEV=0;
		 averageV2GRevenueAllAgents=0;
		
		 averageExtraChargingCostEV=0;
		 averageExtraChargingCostPHEV=0;
		 averageExtraChargingCostAllAgents=0;
		 
		averageJoulesSavedByLocalProductionEV=0 ;
		averageJoulesSavedByLocalProductionPHEV =0;
		averageJoulesSavedByLocalProductionAllAgents=0;
			
		averageJoulesTakenFromBatteryForExtraConsumptionEV =0;
		averageJoulesTakenFromBatteryForExtraConsumptionPHEV =0;
		averageJoulesTakenFromBatteryForExtraConsumptionAllAgents=0;
		
		totalCompensationEV=0;
		totalCompensationPHEV=0;
		
		totalMoneySavedReschedulingEV=0;
		totalMoneySavedReschedulingPHEV=0;
		 totalRegulationUp=0;
		 totalRegulationUpEV=0;
		 totalRegulationUpPHEV=0;
		
		 totalRegulationDown=0;
		 totalRegulationDownEV=0;
		 totalRegulationDownPHEV=0;
		
		 averageRevenueFeedInEV=0;
		 averageRevenueFeedInPHEV=0;
		 averageRevenueFeedInAllAgents=0;
		
		 totalJoulesFeedInEV=0;
		 totalJoulesFeedInPHEV=0;
		 totalJoulesFeedInAllAgents=0;
		
		 totalJoulesFeedInHubSources=0;
		 averageRevenueFeedInHubSources=0;
		 averageExtraChargingHubSources=0;	
	}
	
	
	
	public void initializeAgentStats(){
		agentV2GStatistic = new HashMap<Id, V2GAgentStats>(); 
		hubSourceStatistic = new HashMap<Id, V2GAgentStats>(); 
		for (Id id: mySmartCharger.vehicles.getKeySet()){
			agentV2GStatistic.put(id, new V2GAgentStats());
		}
		HashMap<Id, GeneralSource> m= mySmartCharger.myHubLoadReader.locationSourceMapping;
		if (mySmartCharger.myHubLoadReader.locationSourceMapping!= null){
			for (Id id: mySmartCharger.myHubLoadReader.locationSourceMapping.keySet()){
				hubSourceStatistic.put(id, new V2GAgentStats()); //linkId, Stats
			}
		}
	}
	
	
	public HashMap<Id, V2GAgentStats> getHubSourceStatistic(){
		return hubSourceStatistic;
	}
	
	
	public double getAgentV2GRevenues(Id id){
		return agentV2GStatistic.get(id).getRevenueV2G();
	}
	
	public double getAgentTotalJouleV2GUp(Id id){
		return agentV2GStatistic.get(id).getTotalJoulesUp();
	}
	
	public double getAgentTotalJouleV2GDown(Id id){
		return agentV2GStatistic.get(id).getTotalJoulesDown();
	}
	
	
	public double getAgentTotalJouleFeedIn(Id id){
		return agentV2GStatistic.get(id).getTotalJoulesFeedIn();
	}
	
	
	public double getAgentRevenueFeedIn(Id id){
		return agentV2GStatistic.get(id).getRevenueFeedIn();
	}
	
	
	public double getAgentJoulesSavedByLocalProduction(Id id){
		return agentV2GStatistic.get(id).getJoulesSavedWithLocalProduction();
	}
	
	public double getAgentJoulesTakenFromBatteryForExtraConsumption(Id id){
		return agentV2GStatistic.get(id).getJoulesTakenFromBatteryForExtraConsumption();
	}
	
	public double getAgentExtraChargingCost(Id id){
		return agentV2GStatistic.get(id).getExtraChargingCosts();
	}
	
	
	
	public double getAverageV2GRevenueAgent(){
		return averageV2GRevenueAllAgents;
	}
	
	public double getAverageV2GRevenueEV(){
		return averageV2GRevenueEV;
	}
	
	public double getAverageV2GRevenuePHEV(){
		return averageV2GRevenuePHEV;
	}
	
	public double getAverageExtraChargingAllVehicles(){
		return averageExtraChargingCostAllAgents;
	}
	
	public double getAverageExtraChargingAllEVs(){
		return averageExtraChargingCostEV;
	}
	
	public double getAverageExtraChargingAllPHEVs(){
		return averageExtraChargingCostPHEV;
	}
	
	
	public double getAverageJoulesSavedByLocalProductionEV(){
		return averageJoulesSavedByLocalProductionEV;
	}
	
	public double getAverageJoulesSavedByLocalProductionPHEV(){
		return averageJoulesSavedByLocalProductionPHEV;
	}
	
	public double getAverageJoulesSavedByLocalProductionAllAgents(){
		return averageJoulesSavedByLocalProductionAllAgents;
	}
	
	public double getAverageJoulesTakenFromBatteryForExtraConsumptionEV(){
		return averageJoulesTakenFromBatteryForExtraConsumptionEV;
	}
	
	public double getAverageJoulesTakenFromBatteryForExtraConsumptionPHEV(){
		return averageJoulesTakenFromBatteryForExtraConsumptionPHEV;
	}
	
	public double getAverageJoulesTakenFromBatteryForExtraConsumptionAllAgents(){
		return averageJoulesTakenFromBatteryForExtraConsumptionAllAgents;
	}
		
	
	
	public double getAverageFeedInRevenueHubSources(){
		return averageRevenueFeedInHubSources;
	}
	
	
	public double getAverageExtraChargingHubSources(){
		return averageExtraChargingHubSources;
	}
	
	public double getTotalJoulesFeedInHubSources(){
		return totalJoulesFeedInHubSources;
	}
	
	
	public double getAverageFeedInRevenueEV(){
		return averageRevenueFeedInEV;
	}
	
	
	public double getAverageFeedInRevenuePHEV(){
		return averageRevenueFeedInPHEV;
	}
	
	public double getAverageFeedInRevenueAllAgents(){
		return averageRevenueFeedInAllAgents;
	}
	
	
	public double getTotalRegulationFeedIn(){
		return totalJoulesFeedInAllAgents;
	}
	
	public double getTotalRegulationFeedInEV(){
		return totalJoulesFeedInEV;
	}
	
	public double getTotalRegulationFeedInPHEV(){
		return totalJoulesFeedInPHEV;
	}
	
	public double getTotalRegulationUp(){
		return totalRegulationUp;
	}
	
	public double getTotalRegulationUpEV(){
		return totalRegulationUpEV;
	}
	
	public double getTotalRegulationUpPHEV(){
		return totalRegulationUpPHEV;
	}
	
	public double getTotalRegulationDown(){
		return totalRegulationDown;
	}
	
	public double getTotalRegulationDownEV(){
		return totalRegulationDownEV;
	}
	
	public double getTotalRegulationDownPHEV(){
		return totalRegulationDownPHEV;
	}
	
	
	public double getTotalDirectCompensationEV(){
		return totalCompensationEV;
	}
	
	public double getTotalDirectCompensationPHEV(){
		return totalCompensationPHEV;
	}
	
	public double getIndirectSavingReschedulingEV(){
		return totalMoneySavedReschedulingEV;
	}
	
	public double getIndirectSavingReschedulingPHEV(){
		return totalMoneySavedReschedulingPHEV;
	}
	
	
	private void writeV2GPerAgentTxt(){
		try{
		    // Create file 
			String title=(mySmartCharger.outputPath + "V2GPerAgent_summary.txt");
		    FileWriter fstream = new FileWriter(title);
		    BufferedWriter out = new BufferedWriter(fstream);
		    
		    out.write("Agent Id \t");
		    out.write("EV? \t");
		    out.write("V2G: Total up \t");
		    out.write("V2G: Total down \t");
		    out.write("V2G: Total revenue \t");
		    out.write("StochasticVehicle: Feed in revenue \t");
		    out.write("StochasticVehicle: Joules Feed in \t");
		    out.write("StochasticVehicle: JoulesSaved by local Production \t");
		    out.write("StochasticVehicle: Joules Taken from battery for extra consumption \t");		    
		    out.write("StochasticVehicle:Extra charging costs \n");
		    
		    //*********************
		    int totalEV=mySmartCharger.getAllAgentsWithEV().size();
			for(int i=0; i<totalEV; i++){
				Id agentId= mySmartCharger.getAllAgentsWithEV().get(i);
				out.write(agentId.toString() +"\t");
			    out.write(mySmartCharger.hasAgentEV(agentId)+"\t");
			    out.write(getAgentTotalJouleV2GUp(agentId)+"\t");
			    out.write(getAgentTotalJouleV2GDown(agentId)+"\t");
			    out.write(getAgentV2GRevenues(agentId)+"\t");
			    out.write(getAgentRevenueFeedIn(agentId)+"\t");
			    out.write(getAgentTotalJouleFeedIn(agentId)+"\t");
			    out.write(getAgentJoulesSavedByLocalProduction(agentId)+"\t");
			    out.write(getAgentJoulesTakenFromBatteryForExtraConsumption(agentId)+"\t");			   
			    out.write(getAgentExtraChargingCost(agentId)+"\n");
			}
			
			int totalPHEV=mySmartCharger.getAllAgentsWithPHEV().size();
			for(int i=0; i<totalPHEV; i++){
				Id agentId= mySmartCharger.getAllAgentsWithPHEV().get(i);
				out.write(agentId.toString() +"\t");
			    out.write(mySmartCharger.hasAgentEV(agentId)+"\t");
			    out.write(getAgentTotalJouleV2GUp(agentId)+"\t");
			    out.write(getAgentTotalJouleV2GDown(agentId)+"\t");
			    out.write(getAgentV2GRevenues(agentId)+"\t");
			    out.write(getAgentRevenueFeedIn(agentId)+"\t");
			    out.write(getAgentTotalJouleFeedIn(agentId)+"\t");
			    out.write(getAgentJoulesSavedByLocalProduction(agentId)+"\t");
			    out.write(getAgentJoulesTakenFromBatteryForExtraConsumption(agentId)+"\t");			   
			    out.write(getAgentExtraChargingCost(agentId)+"\n");
			}
			
		    		   
		    //Close the output stream
		    out.close();
		    }catch (Exception e){
		    	//Catch exception if any
		    }
	}
	
	private void writeV2GDecisionMonitor(){
		try{
		    // Create file 
			String title=(mySmartCharger.outputPath + "V2GDecisions_summary.txt");
		    FileWriter fstream = new FileWriter(title);
		    BufferedWriter out = new BufferedWriter(fstream);
		    
		    out.write("Agent Id \t");
		    out.write("EV? \t");
		    out.write("From: \t");
		    out.write("To: \t");
		    out.write("costKeeping: \t");
		    out.write("costReschedule: \t");
		    out.write("compensation:  \t");
		    out.write("reschedule?: \t");
		    out.write("additional Revenue?: \n");
		    //*********************
		    int totalDecisions=V2GdecisionsList.size();
			for(int i=0; i<totalDecisions; i++){
				//System.out.println("item"+ i);
				V2GDecisionMonitor v= V2GdecisionsList.get(i);
				out.write(v.writeString());
			}
		    		   
		    //Close the output stream
		    out.close();
		    }catch (Exception e){
		    	e.printStackTrace();
		    	}
	}
	
	
	
	public void calcV2GVehicleStats(){
		writeV2GDecisionMonitor();
		writeV2GPerAgentTxt();
		//EV
		int totalEV=mySmartCharger.getAllAgentsWithEV().size();
		for(int i=0; i<totalEV; i++){
			
			Id agentId= mySmartCharger.getAllAgentsWithEV().get(i);
			
			averageV2GRevenueEV+=getAgentV2GRevenues(agentId);
			averageV2GRevenueAllAgents+=getAgentV2GRevenues(agentId);
			
			totalRegulationDown+=getAgentTotalJouleV2GDown(agentId);
			totalRegulationDownEV+=getAgentTotalJouleV2GDown(agentId);
			
			totalRegulationUp+=getAgentTotalJouleV2GUp(agentId);
			totalRegulationUpEV+=getAgentTotalJouleV2GUp(agentId);
			
			averageRevenueFeedInEV+= getAgentRevenueFeedIn(agentId);
			totalJoulesFeedInEV+= getAgentTotalJouleFeedIn(agentId);
			
			averageExtraChargingCostEV+= getAgentExtraChargingCost(agentId);
			averageExtraChargingCostAllAgents+= getAgentExtraChargingCost(agentId);
			
			averageJoulesSavedByLocalProductionAllAgents+=getAgentJoulesSavedByLocalProduction(agentId);
			averageJoulesSavedByLocalProductionEV+=getAgentJoulesSavedByLocalProduction(agentId);
			
			averageJoulesTakenFromBatteryForExtraConsumptionEV+=getAgentJoulesTakenFromBatteryForExtraConsumption(agentId);
			averageJoulesTakenFromBatteryForExtraConsumptionAllAgents+=getAgentJoulesTakenFromBatteryForExtraConsumption(agentId);
			
		}
		if (totalEV==0){
			averageV2GRevenueEV=0;
			averageRevenueFeedInEV=0;
			averageExtraChargingCostEV=0;
			averageJoulesSavedByLocalProductionEV=0;
			averageJoulesTakenFromBatteryForExtraConsumptionEV=0;
		}else{
			averageV2GRevenueEV=averageV2GRevenueEV/totalEV;
			averageRevenueFeedInEV=averageRevenueFeedInEV/totalEV;
			averageExtraChargingCostEV=averageExtraChargingCostEV/totalEV;
			averageJoulesSavedByLocalProductionEV=averageJoulesSavedByLocalProductionEV/totalEV;
			averageJoulesTakenFromBatteryForExtraConsumptionEV=averageJoulesTakenFromBatteryForExtraConsumptionEV/totalEV;
		}
	
		
		//PHEV
		int totalPHEV=mySmartCharger.getAllAgentsWithPHEV().size();
		for(int i=0; i<totalPHEV; i++){
			Id agentId= mySmartCharger.getAllAgentsWithPHEV().get(i);
			averageV2GRevenuePHEV+=getAgentV2GRevenues(agentId);
			averageV2GRevenueAllAgents+=getAgentV2GRevenues(agentId);
			
			totalRegulationDown+=getAgentTotalJouleV2GDown(agentId);
			totalRegulationDownPHEV+=getAgentTotalJouleV2GDown(agentId);
			
			totalRegulationUp+=getAgentTotalJouleV2GUp(agentId);
			totalRegulationUpPHEV+=getAgentTotalJouleV2GUp(agentId);
			
			averageRevenueFeedInPHEV+= getAgentRevenueFeedIn(agentId);
			totalJoulesFeedInPHEV+= getAgentTotalJouleFeedIn(agentId);
			
			averageExtraChargingCostPHEV+= getAgentExtraChargingCost(agentId);
			averageExtraChargingCostAllAgents+= getAgentExtraChargingCost(agentId);
			
			averageJoulesSavedByLocalProductionAllAgents+=getAgentJoulesSavedByLocalProduction(agentId);
			averageJoulesSavedByLocalProductionPHEV+=getAgentJoulesSavedByLocalProduction(agentId);
			
			averageJoulesTakenFromBatteryForExtraConsumptionPHEV+=getAgentJoulesTakenFromBatteryForExtraConsumption(agentId);
			averageJoulesTakenFromBatteryForExtraConsumptionAllAgents+=getAgentJoulesTakenFromBatteryForExtraConsumption(agentId);
			
		}
	
		if(totalPHEV==0){
			averageV2GRevenuePHEV=0.0;
			averageRevenueFeedInPHEV=0.0;
			averageExtraChargingCostPHEV=0.0;
			averageJoulesSavedByLocalProductionPHEV=0;
			averageJoulesTakenFromBatteryForExtraConsumptionPHEV=0;
		}else{
			averageV2GRevenuePHEV=averageV2GRevenuePHEV/totalPHEV;
			averageRevenueFeedInPHEV=averageRevenueFeedInPHEV/totalPHEV;
			averageExtraChargingCostPHEV=averageExtraChargingCostPHEV/totalPHEV;
			averageJoulesSavedByLocalProductionPHEV=averageJoulesSavedByLocalProductionPHEV/totalPHEV;
			averageJoulesTakenFromBatteryForExtraConsumptionPHEV=averageJoulesTakenFromBatteryForExtraConsumptionPHEV/totalPHEV;
		}
		//TOTAL
		averageV2GRevenueAllAgents=averageV2GRevenueAllAgents/(totalPHEV+totalEV);
		averageRevenueFeedInAllAgents=averageRevenueFeedInPHEV/(totalPHEV+totalEV);
		averageExtraChargingCostAllAgents=averageExtraChargingCostAllAgents/(totalPHEV+totalEV);
		
		averageJoulesTakenFromBatteryForExtraConsumptionAllAgents=
			averageJoulesTakenFromBatteryForExtraConsumptionAllAgents/(totalPHEV+totalEV);
		averageJoulesSavedByLocalProductionAllAgents=averageJoulesSavedByLocalProductionAllAgents/(totalPHEV+totalEV);
	}
	
	
	
	private void writePerHubSourceTxt(){
		try{
		    // Create file 
			String title=(mySmartCharger.outputPath + "HubSource_summary.txt");
		    FileWriter fstream = new FileWriter(title);
		    BufferedWriter out = new BufferedWriter(fstream);
		    
		    out.write("Link Id \t");
		    out.write("Name \t");
		    out.write("Total Joules Feed in \t");
		    out.write("Total Feed in revenue \t");
		    out.write("Total Extra charging cost\n");
		    
		   
		    //*********************
			int numLinks= mySmartCharger.myHubLoadReader.locationSourceMapping.keySet().size();
			for(Id linkId: mySmartCharger.myHubLoadReader.locationSourceMapping.keySet()){
			
				out.write(linkId.toString() +"\t");
			    out.write(mySmartCharger.myHubLoadReader.locationSourceMapping.get(linkId).getName()+"\t");
			    out.write(hubSourceStatistic.get(linkId).getTotalJoulesFeedIn()+"\t");
			    out.write(hubSourceStatistic.get(linkId).getRevenueFeedIn()+"\t");			   
			    out.write(hubSourceStatistic.get(linkId).getExtraChargingCosts()+"\n");
			}
			
		    		   
		    //Close the output stream
		    out.close();
		    }catch (Exception e){
		    	//Catch exception if any
		    }
	}
	
	
	
	public void calcHubSourceStats(){
		writePerHubSourceTxt();
		if(mySmartCharger.myHubLoadReader.locationSourceMapping!=null){
			int numLinks= mySmartCharger.myHubLoadReader.locationSourceMapping.keySet().size();
			for(Id linkId: mySmartCharger.myHubLoadReader.locationSourceMapping.keySet()){
				averageRevenueFeedInHubSources+=hubSourceStatistic.get(linkId).getRevenueFeedIn();
				totalJoulesFeedInHubSources+=hubSourceStatistic.get(linkId).getTotalJoulesFeedIn();
				averageExtraChargingHubSources+=hubSourceStatistic.get(linkId).getExtraChargingCosts();
			}
			averageRevenueFeedInHubSources=averageRevenueFeedInHubSources/numLinks;
			averageExtraChargingHubSources=averageExtraChargingHubSources/numLinks;
		}		
	}
	
	
	
	
	public void addRevenueToAgentFromFeedIn(double revenue, Id agentId){		
		agentV2GStatistic.get(agentId).addRevenueFeedIn(revenue);		
	}
	
	
	
	public void addJouleFeedInToAgentStats(double joulesUpDown, Id agentId){		
		
		if (joulesUpDown>0){
			agentV2GStatistic.get(agentId).addJoulesFeedIn(joulesUpDown);
		}	
	}
	
	
	
	public void addRevenueToHubSourceFromFeedIn(double revenue, Id linkId){		
		hubSourceStatistic.get(linkId).addRevenueFeedIn(revenue);		
	}
	
	
	
	public void addJouleFeedInToHubSourceStats(double joulesUpDown, Id linkId){		
		
		if (joulesUpDown>0){
			hubSourceStatistic.get(linkId).addJoulesFeedIn(joulesUpDown);
		}	
	}
	
	
	
	public void addRevenueToAgentFromV2G(double revenue, Id agentId){		
		agentV2GStatistic.get(agentId).addRevenueV2G(revenue);		
	}
	
	
	
	
	public void addJoulesUpDownToAgentStats(double joulesUpDown, Id agentId){		
		
		if (joulesUpDown>0){
			//joules>0 reg down
			agentV2GStatistic.get(agentId).addJoulesDown(joulesUpDown);
		}else{//up
			agentV2GStatistic.get(agentId).addJoulesUp(joulesUpDown);
		}
	
	}
	
	
	
	public void regulationUpDownVehicleLoad(Id agentId, 
			LoadDistributionInterval electricSourceInterval, 			
			double compensation, // money
			double joules,		
			String type			
			) throws Exception{
		
		double currentSOCBefore= getSOCAtTime(agentId, electricSourceInterval.getEndTime());
		double currentSOC=currentSOCBefore+joules;
		
		double batterySize= mySmartCharger.getBatteryOfAgent(agentId).getBatterySize();
		double batteryMin=mySmartCharger.getBatteryOfAgent(agentId).getMinSOC();
		double batteryMax=mySmartCharger.getBatteryOfAgent(agentId).getMaxSOC(); 
		//*****************************
		if (currentSOC>batterySize*batteryMax && (batterySize*batteryMax>currentSOCBefore)){
			// IF too much V2G
			double newJoules=batterySize*batteryMax-currentSOCBefore;			
			if (joules<0){newJoules=newJoules*(-1);}
			compensation=newJoules*compensation/joules;
			// adjust electricSurceIntercal
			// W*s=J
			double wattInterval= newJoules/electricSourceInterval.getIntervalLength();
			LoadDistributionInterval adjusted= new LoadDistributionInterval(electricSourceInterval.getStartTime(), 
					electricSourceInterval.getEndTime(), wattInterval);
			costComparisonVehicle(agentId, 
					adjusted, 				
					compensation,				
					type,				
					batterySize*batteryMax, newJoules);
		}else {
			// IF too much V2G
			if(currentSOC<batterySize*batteryMin && (batterySize*batteryMin<currentSOCBefore)){
				double newJoules=currentSOCBefore-batterySize*batteryMin;
				if (joules<0){newJoules=newJoules*(-1);}
				compensation=newJoules*compensation/joules;
				// adjust electricSurceIntercal
				double wattInterval= newJoules/electricSourceInterval.getIntervalLength();
				LoadDistributionInterval adjusted= new LoadDistributionInterval(electricSourceInterval.getStartTime(), 
						electricSourceInterval.getEndTime(), wattInterval);
				costComparisonVehicle(agentId, 
						adjusted, 				
						compensation,				
						type,				
						batterySize*batteryMin, newJoules);
			}else{
				// IF GOOD
				
				costComparisonVehicle(agentId, 
						electricSourceInterval, 				
						compensation,				
						type,				
						currentSOC, joules);
			}
		}		
		
	}
	
	
	
	
	
	public void feedInHubSource(Id linkId,
			LoadDistributionInterval electricSourceInterval,											
			double compensation,
			double joulesFromSource												
			) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		
		// REDUCE HUBSOURCE LOAD
		reduceHubSourceLoadsByGivenLoadInterval(
				linkId, 
				electricSourceInterval);
		
		// ADD REVENUE  
		addRevenueToHubSourceFromFeedIn(compensation, linkId);
		
		// ADD joules
		addJouleFeedInToHubSourceStats(joulesFromSource, linkId);	
		
		// REDUCE 15min stochastic general load
		// increase hubLoadfromCharge
		// hubload +(- ((-posCurrent)  )  )		
		int hubId= mySmartCharger.myHubLoadReader.getHubForLinkId(linkId);
			
		
		LoadDistributionInterval negatedElectricSourceInterval= new LoadDistributionInterval(
				electricSourceInterval.getStartTime(), 
				electricSourceInterval.getEndTime(), 
				new PolynomialFunction(electricSourceInterval.getPolynomialFunction().getCoefficients().clone()).negate());
		
		reduceHubLoadByGivenLoadInterval(hubId, negatedElectricSourceInterval);
	}
	
	
	
	
	public void hubSourceChargeExtra(Id linkId,
			LoadDistributionInterval electricSourceInterval,
			double joulesFromSource	) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		
		// REDUCE HUBSOURCE LOAD
		// interval < 0   (-negVal) - (-negVal)=0
		reduceHubSourceLoadsByGivenLoadInterval(
				linkId, 
				electricSourceInterval);
		
		// approximation of costs of charging
		double extraCost=approximateChargingCostOfVariableLoad(
				linkId, electricSourceInterval.getStartTime(), joulesFromSource);
		
		hubSourceStatistic.get(linkId).addExtraChargingCosts(extraCost);
		
		// reduce hubLoadfromCharge
		// hubload +(- (-(negCurrent)  )  )
		
		int hubId= mySmartCharger.myHubLoadReader.getHubForLinkId(linkId);
		LoadDistributionInterval negatedElectricSourceInterval= new LoadDistributionInterval(
				electricSourceInterval.getStartTime(), 
				electricSourceInterval.getEndTime(), 
				new PolynomialFunction(electricSourceInterval.getPolynomialFunction().getCoefficients().clone()).negate());
		
		reduceHubLoadByGivenLoadInterval(hubId, negatedElectricSourceInterval);
		
	}
	
	
	
	
	
	/**
	 * determines during which times the agent is actually parking at the hub in the given load interval
	 * calculates costs for keeping or rescheduling
	 * and then makes a cost comparison
	 * 
	 * @param agentId
	 * @param currentStochasticLoadInterval
	 * @param agentParkingDrivingSchedule
	 * @param type
	 * @param hub
	 * @throws Exception 
	 */
	public void regulationUpDownHubLoad(Id agentId, 
			LoadDistributionInterval currentStochasticLoadInterval, 
			Schedule agentParkingDrivingSchedule, 			
			String type,			
			int hub) throws Exception{
		
		//*****************************
		//*****************************
		// CHECK WHICH PARTS AGENT IS AT HUB and parking
			Schedule relevantAgentParkingAndDrivingScheduleAtHub= 
				findAndReturnAgentScheduleWithinLoadIntervalWhichIsAtSpecificHub(agentId, hub,				
				currentStochasticLoadInterval);
			
			if(relevantAgentParkingAndDrivingScheduleAtHub.getNumberOfEntries()>0){
				
				for(int i=0; i<relevantAgentParkingAndDrivingScheduleAtHub.getNumberOfEntries();i++){
					
					ParkingInterval t= (ParkingInterval) relevantAgentParkingAndDrivingScheduleAtHub.timesInSchedule.get(i);
					// the overlap interval is:
					LoadDistributionInterval electricSourceInterval= 
						new LoadDistributionInterval(t.getStartTime(), 
								t.getEndTime(), 
								new PolynomialFunction(currentStochasticLoadInterval.getPolynomialFunction().getCoefficients().clone()), 
								currentStochasticLoadInterval.isOptimal());
					
					double joules= mySmartCharger.functionSimpsonIntegrator.integrate(electricSourceInterval.getPolynomialFunction(), 
							electricSourceInterval.getStartTime(),
							electricSourceInterval.getEndTime());
					//System.out.println("joules agent ="+ joules );
					
					/*
					 * IN CASE JOULES IS HIGHER THAN WHAT IS FEASIBLE AT THE CONNECTION
					 */
					if (Math.abs(joules)>t.getChargingSpeed()*electricSourceInterval.getIntervalLength()){
						if(joules<0){
							electricSourceInterval= 
								new LoadDistributionInterval(t.getStartTime(), 
										t.getEndTime(), 
										new PolynomialFunction(new double []{-1*t.getChargingSpeed()}), 
										currentStochasticLoadInterval.isOptimal());
							joules=-1*t.getChargingSpeed()*electricSourceInterval.getIntervalLength();
						}else{
							electricSourceInterval= 
								new LoadDistributionInterval(t.getStartTime(), 
										t.getEndTime(), 
										new PolynomialFunction(new double []{t.getChargingSpeed()}), 
										currentStochasticLoadInterval.isOptimal());
							joules=t.getChargingSpeed()*electricSourceInterval.getIntervalLength();
						}
					}
					//*****************************
					//LIMIT JOULES TO VALID SOC VALUES
					
					double currentSOCBefore=getSOCAtTime(agentId, electricSourceInterval.getEndTime() );
					/*
					 * if regulation up   joules<0    currentSOC+(-joules)
					 * if regulation down  joules>0   currentSOC+(+joules)
					 */
					
					double currentSOC=currentSOCBefore+joules;
					double batterySize= mySmartCharger.getBatteryOfAgent(agentId).getBatterySize();
					double batteryMin=mySmartCharger.getBatteryOfAgent(agentId).getMinSOC();
					double batteryMax=mySmartCharger.getBatteryOfAgent(agentId).getMaxSOC(); 
					//*****************************
					if (currentSOC>batterySize*batteryMax && (batterySize*batteryMax>currentSOCBefore)){
						// IF too much V2G
						double newJoules=batterySize*batteryMax-currentSOCBefore;
						if (joules<0){newJoules=newJoules*(-1);}
						double compensation=calculateCompensationUpDown(newJoules,agentId);
						// adjust electricSurceIntercal
						// W*s=J
						double wattInterval= newJoules/electricSourceInterval.getIntervalLength();
						LoadDistributionInterval adjusted= new LoadDistributionInterval(electricSourceInterval.getStartTime(), 
								electricSourceInterval.getEndTime(), wattInterval);
						costComparisonHubload(agentId, 
								adjusted, 							
								compensation,
								newJoules,							
								type,							
								hub,
								batterySize*batteryMax);
					}else {
						// IF too much V2G
						if(currentSOC<batterySize*batteryMin && (batterySize*batteryMin<currentSOCBefore)){
							double newJoules=currentSOCBefore-batterySize*batteryMin;
							if (joules<0){newJoules=newJoules*(-1);}
							double compensation=calculateCompensationUpDown(newJoules,agentId);
							// adjust electricSurceIntercal
							double wattInterval= newJoules/electricSourceInterval.getIntervalLength();
							LoadDistributionInterval adjusted= new LoadDistributionInterval(electricSourceInterval.getStartTime(), 
									electricSourceInterval.getEndTime(), wattInterval);
							costComparisonHubload(agentId, 
									adjusted, 							
									compensation,
									newJoules,							
									type,							
									hub,
									batterySize*batteryMin);
						}else{
							// IF GOOD
							
							double compensation=calculateCompensationUpDown(joules,agentId);
							costComparisonHubload(agentId, 
									electricSourceInterval, 							
									compensation,
									joules,							
									type,							
									hub,
									currentSOC);
						}
					}
					
					
				}
	
			}
	}



	/**
	 * calculates the costs for keeping or rescheduling the plan
	 * accordingly, the charging schedule is changed and the stochastic loads reduced
	 * @param agentId
	 * @param electricSourceInterval
	 * @param agentParkingDrivingSchedule
	 * @param compensation
	 * @param ev
	 * @param type
	 * @param lpev
	 * @param lpphev
	 * @param batterySize
	 * @param batteryMin
	 * @param batteryMax
	 * @param currentSOC
	 * @throws Exception 
	 */
	public void costComparisonVehicle(Id agentId, 
			LoadDistributionInterval electricSourceInterval,			
			double compensation, // money		
			String type,				
			double currentSOC,
			double joules) throws Exception{
		
		double batterySize= mySmartCharger.getBatteryOfAgent(agentId).getBatterySize();
		double batteryMin=mySmartCharger.getBatteryOfAgent(agentId).getMinSOC();
		double batteryMax=mySmartCharger.getBatteryOfAgent(agentId).getMaxSOC(); 
		//getSOCAtTime(agentId, electricSourceInterval.getEndTime())
		
		Schedule secondHalf= cutScheduleAtTimeSecondHalfAndReassignJoules(agentId, 
				mySmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentId), 
				electricSourceInterval.getEndTime());
					
		secondHalf.setStartingSOC(currentSOC);
		answerScheduleAfterElectricSourceInterval=null;
		
		double costKeeping=mySmartCharger.calculateChargingCostForAgentSchedule(agentId, secondHalf);
		
		double costReschedule;
		//if (currentSOC>=batterySize*batteryMin && currentSOC<=batterySize*batteryMax){
		costReschedule= calcCostForRescheduling(
					secondHalf, 
					agentId, 
					batterySize, batteryMin, batteryMax, 
					type,
					currentSOC,					
					compensation
					);
		//}else{
		//	costReschedule=Double.MAX_VALUE;
		//}
			
		// reschedule if equal - hopefully the load can be balanced elsewhere in the grid
		if(costKeeping>=costReschedule && answerScheduleAfterElectricSourceInterval!=null){
			
			//reschedule
			reschedule(agentId, 
					answerScheduleAfterElectricSourceInterval,					
					electricSourceInterval,
					costKeeping,
					costReschedule,
					joules);
			
			// joules>0 positive function which needs to be reduced
			reduceAgentVehicleLoadsByGivenLoadInterval(
					agentId, 
					electricSourceInterval);
			
			if(joules>0){
				agentV2GStatistic.get(agentId).addJoulesSavedWithLocalProduction(joules);
			}else{
				agentV2GStatistic.get(agentId).addJoulesTakenFromBatteryForExtraConsumption(joules);
			}
		
		}else{
			
			if(joules>0){
				// reg down not possible - i.e. battery full
				// thus attribute rest to upper level
				attributeSuperfluousVehicleLoadsToGridIfPossible(agentId, 					 
						electricSourceInterval
						);	
			}else{
				// reg up = local energy demand
				// increase charging need here
				chargeMoreToAccomodateExtraVehicleLoad(agentId, 					 
						electricSourceInterval);
			}	
		}
	}
	
	
	
	public void costComparisonHubload(Id agentId, 
			LoadDistributionInterval electricSourceInterval, 			
			double compensation,
			double joules,			
			String type,			
			int hub,
			double currentSOC) throws Exception{
		
		Schedule secondHalf= cutScheduleAtTimeSecondHalfAndReassignJoules(agentId, 
				mySmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentId), 
				electricSourceInterval.getEndTime());
		//secondHalf.printSchedule();
		secondHalf.setStartingSOC(currentSOC);
		answerScheduleAfterElectricSourceInterval=null;		
		
		double costKeeping=mySmartCharger.calculateChargingCostForAgentSchedule(agentId, secondHalf);
		
		double batterySize= mySmartCharger.getBatteryOfAgent(agentId).getBatterySize();
		double batteryMin=mySmartCharger.getBatteryOfAgent(agentId).getMinSOC();
		double batteryMax=mySmartCharger.getBatteryOfAgent(agentId).getMaxSOC(); 
		
		double costReschedule=0;
		
		
		costReschedule= calcCostForRescheduling(
					secondHalf, 
					agentId, 
					batterySize, batteryMin, batteryMax, 
					type,
					currentSOC,					
					compensation					
					);
				
		
		// if costs are equal = also reschedule, because its good for the system
		// if EV failure then costKeeping can be Infinity > Double.Max=costReschedule
		// with answerScheduleAfterElectricSourceInterval=null
		V2GDecisionMonitor v = new V2GDecisionMonitor(agentId, costKeeping, costReschedule, compensation, electricSourceInterval);
		v.writeOut();
		V2GdecisionsList.add(v);
		if(costKeeping>=costReschedule && answerScheduleAfterElectricSourceInterval!=null){
			
			addCompensationAndSavedMoneyByRescheduling(agentId, costKeeping, costReschedule, compensation);
			//UPDATE CHARGING COSTS 
			addRevenueToAgentFromV2G(costKeeping-costReschedule, agentId);
			
			// add joules to V2G up or down
			addJoulesUpDownToAgentStats(joules, agentId);
			
			//reschedule
			reschedule(agentId,
					answerScheduleAfterElectricSourceInterval,
					electricSourceInterval,
					costKeeping,
					costReschedule,
					joules);
			
			// e.g. stochastic load on net is -3500
			// thus car discharges 3500 to balance the net
			// thus -3500 will be reduced by (-3500)= 0
			reduceHubLoadByGivenLoadInterval(hub, electricSourceInterval);
		}
	}
	
	
	/**
	 * goes through the part of agents schedule which is during  currentStochasticLoadInterval,
	 * a schedule will be returned of all overlapping segments that are at the specified hub 
	 * 
	 * @param hub
	 * @param agentParkingDrivingSchedule
	 * @param currentStochasticLoadInterval
	 * @return
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 */
	public Schedule findAndReturnAgentScheduleWithinLoadIntervalWhichIsAtSpecificHub(
			Id agentId, 
			int hub,
			LoadDistributionInterval currentStochasticLoadInterval) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		Schedule relevantAgentParkingAndDrivingScheduleAtHub=new Schedule();
		
		
		Schedule relevantAgentParkingAndDrivingSchedule= 
			mySmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentId).cutScheduleAtTime(
					currentStochasticLoadInterval.getEndTime(), agentId);
	
		relevantAgentParkingAndDrivingSchedule= 
			relevantAgentParkingAndDrivingSchedule.cutScheduleAtTimeSecondHalf(
					currentStochasticLoadInterval.getStartTime(), 0.0, agentId);
		
		for(int i=0; i<relevantAgentParkingAndDrivingSchedule.getNumberOfEntries(); i++){
			
			TimeInterval t= relevantAgentParkingAndDrivingSchedule.timesInSchedule.get(i);
			if(t.isParking()
					&& 
					mySmartCharger.myHubLoadReader.getHubForLinkId( ((ParkingInterval) t ).getLocation())==hub){
				relevantAgentParkingAndDrivingScheduleAtHub.addTimeInterval(t);
			}
		}
		
		return relevantAgentParkingAndDrivingScheduleAtHub;
	}
	
	
	/**
	 * <ul>
	 * <li>updates revenue of agent in addRevenueToAgentFromV2G
	 * <li>reassembles updated agentParkingandDriving Schedule from original Schedule,
	 * the rescheduled part during the electric stochastic load
	 * and the newly scheduled part after the electric stochastic load;
	 * <li>saves the reassembled schedule back into AgentParkingAndDrivingSchedules
	 * <li> updates total charging costs in ChargingCostsForAgents
	 * </ul>
	 * 
	 * 
	 * @param agentId
	 * @param answerScheduleAfterElectricSourceInterval
	 * @param agentParkingDrivingSchedule
	 * @param electricSourceInterval
	 * @param costKeeping
	 * @param costReschedule
	 * @throws Exception 
	 */
	public void reschedule(Id agentId, 
			Schedule answerScheduleAfterElectricSourceInterval,
			LoadDistributionInterval electricSourceInterval,
			double costKeeping,
			double costReschedule,
			double joules) throws Exception{
		
		
		// change agents charging costs
		mySmartCharger.getChargingCostsForAgents().put(
				agentId, 
				(mySmartCharger.getChargingCostsForAgents().get(agentId)-costKeeping+costReschedule)
				);
		
				
		//UPDATE AGENTPARKINGADNDRIVINGSCHEDULE WITH NEW required charging times
		// also reducing or increasing charging times during electric load
		updateAgentDrivingParkingSchedule(agentId, 
				answerScheduleAfterElectricSourceInterval,				
				electricSourceInterval,
				joules);
		
		// distribute new found required charging times for agent
		updateChargingSchedule(agentId, 
				answerScheduleAfterElectricSourceInterval,
				electricSourceInterval,
				joules);
	}
	
	/**
	 * creates and saves new charging schedule 
	 * 
	 * @param agentId
	 * @param answerScheduleAfterElectricSourceInterval
	 * @param electricSourceInterval
	 * @throws Exception 
	 */
	public void  updateChargingSchedule(Id agentId, 
			Schedule answerScheduleAfterElectricSourceInterval,
			LoadDistributionInterval electricSourceInterval, 
			double joules	) throws Exception{
		
		//UPDATE CHARGING SCHEDULE
		
		Schedule newChargingSchedule= mySmartCharger.myChargingSlotDistributor.distribute(
				agentId, 
				mySmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentId));
		if(DecentralizedSmartCharger.debug){	
			System.out.println("new  charging schedule of agent "+ agentId.toString());
			 newChargingSchedule.printSchedule();
			 System.out.println("new  parking driving schedule of agent "+ agentId.toString());
			 mySmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentId).printSchedule();	
		}
		
		mySmartCharger.getAllAgentChargingSchedules().put(agentId, newChargingSchedule);
			
	}
	
	
	
	/**
	 * builds new agent parking and driving schedule from
	 * <li> first half of agent schedule up to loadInterval
	 * <li> schedule during loadInterval +/- additional energy = charging time
	 * <li> the answer schedule
	 * 
	 * 
	 * @param agentId
	 * @param answerScheduleAfterElectricSourceInterval
	 * @param agentParkingDrivingSchedule
	 * @param electricSourceInterval
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 */
	public void updateAgentDrivingParkingSchedule(Id agentId, 
			Schedule answerScheduleAfterElectricSourceInterval,			
			LoadDistributionInterval electricSourceInterval,
			double joules) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		if(DecentralizedSmartCharger.debug){		
			System.out.println("parking driving schedule of agent BEFORE"+ agentId.toString());
			mySmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentId).printSchedule();
		}
		
		
		//mySmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentId).printSchedule();
		
		Schedule rescheduledFirstHalf= mySmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentId).cutScheduleAtTime(
				electricSourceInterval.getEndTime(), agentId);
		
		//rescheduledFirstHalf.printSchedule();
		
		//inbetween part with new load
		Schedule rescheduledElectricLoadPart= rescheduledFirstHalf.cutScheduleAtTimeSecondHalf(
				electricSourceInterval.getStartTime(), 0.0, agentId);
		
		//rescheduledElectricLoadPart.printSchedule();
		
		// insert new bit
		// joules/charging speed = secs
		ParkingInterval thisParking=((ParkingInterval)rescheduledElectricLoadPart.timesInSchedule.get(0));
		double newChargingLength= joules/thisParking.getChargingSpeed();
		/*
		 * if joules>0  reg down=charging,--> then new CHargingLength>0	
		 * new ChargingLength= oldLength+newLength
		 * ifjoules<0 reg up --> charging length<0
		 * new Charging Length = oldLength+(-newLength)
		 */
		thisParking.setRequiredChargingDuration( thisParking.getRequiredChargingDuration()+newChargingLength);
		
		//first remaining part
		rescheduledFirstHalf= rescheduledFirstHalf.cutScheduleAtTime(electricSourceInterval.getStartTime(), agentId);
		
		try{
		
			rescheduledFirstHalf.mergeSchedules(rescheduledElectricLoadPart);
			rescheduledFirstHalf.mergeSchedules(answerScheduleAfterElectricSourceInterval);
					
			mySmartCharger.getAllAgentParkingAndDrivingSchedules().put(agentId, rescheduledFirstHalf);
			if(DecentralizedSmartCharger.debug){		
				System.out.println("parking driving schedule of agent After"+ agentId.toString());
				mySmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentId).printSchedule();
			}
		}
		catch(Exception e){
			System.out.println("Exception merging schedules" + agentId.toString());
			rescheduledFirstHalf.printSchedule();
			e.printStackTrace();
			
		}
	}
	
	
	/**
	 * adds the passed electricSourceInterval to the current stochastic agent vehicle load
	 * in myHubLoadReader.agentVehicleSourceMapping.getValue(agentId)	
	 * 
	 * @param agentId
	 * @param electricSourceInterval
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 */
	public void reduceAgentVehicleLoadsByGivenLoadInterval(
			Id agentId, 
			LoadDistributionInterval electricSourceInterval) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		// -3500-(-3500)=0		
		
		//Schedule agentVehicleSource= mySmartCharger.myHubLoadReader.agentVehicleSourceMappingAfterContinuous.get(agentId);
		
		PolynomialFunction negativePolynomialFunc= 
			new PolynomialFunction(electricSourceInterval.getPolynomialFunction().getCoefficients().clone());
		negativePolynomialFunc=negativePolynomialFunc.negate();		
		
		// aggregated 96 bins
		mySmartCharger.myHubLoadReader.agentVehicleSourceAfter15MinBins.get(agentId).increaseYEntryOf96EntryBinCollectorBetweenSecStartEndByFunction(
					 electricSourceInterval.getStartTime(), 
					 electricSourceInterval.getEndTime(), negativePolynomialFunc);
	}

	

	public void reduceHubSourceLoadsByGivenLoadInterval(
			Id linkId, 
			LoadDistributionInterval electricSourceInterval) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		PolynomialFunction negativePolynomialFunc= 
			new PolynomialFunction(electricSourceInterval.getPolynomialFunction().getCoefficients().clone());
		negativePolynomialFunc=negativePolynomialFunc.negate();
		
		
		// aggregated 96 bins
		mySmartCharger.myHubLoadReader.locationSourceMappingAfter15MinBins.get(linkId).increaseYEntryOf96EntryBinCollectorBetweenSecStartEndByFunction(
					 electricSourceInterval.getStartTime(), 
					 electricSourceInterval.getEndTime(), negativePolynomialFunc);
			
	}
	
	

	/**
	 * reduces the entry of myHubLoadReader.stochasticHubLoadDistribution.getValue(i)	 * 
	 * with updated HubLoadSchedule which is reduced by given electricSourceInterval
	 * 
	 * hubLoad + (-1*electricSourceInterval)
	 * @param i
	 * @param electricSourceInterval
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 */
	public void reduceHubLoadByGivenLoadInterval(
			Integer hubId, 
			LoadDistributionInterval electricSourceInterval) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		PolynomialFunction negativePolynomialFunc= 
			new PolynomialFunction(electricSourceInterval.getPolynomialFunction().getCoefficients().clone());
		negativePolynomialFunc=negativePolynomialFunc.negate();
		
		mySmartCharger.myHubLoadReader.stochasticHubLoadAfter15MinBins.get(hubId).
		increaseYEntryOf96EntryBinCollectorBetweenSecStartEndByFunction(
				electricSourceInterval.getStartTime(),
				electricSourceInterval.getEndTime(), 
				negativePolynomialFunc);
		
		
	}
	
	
	
	
	public void attributeSuperfluousVehicleLoadsToGridIfPossible(Id agentId, 
			LoadDistributionInterval electricSourceInterval
			) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		Schedule agentDuringLoad= 
			mySmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentId).cutScheduleAtTimeSecondHalf(
					electricSourceInterval.getStartTime(), 0.0, agentId);
		
		agentDuringLoad= agentDuringLoad.cutScheduleAtTime(electricSourceInterval.getEndTime(), agentId);	
		
		for(int i=0; i<agentDuringLoad.getNumberOfEntries();i++){
			TimeInterval agentIntervalInAgentDuringLoad= agentDuringLoad.timesInSchedule.get(i);
			if (agentIntervalInAgentDuringLoad.isParking()){
				
				// if overlap between agentInterval && electricSource
				LoadDistributionInterval overlapAgentAndElectricSource=
					agentIntervalInAgentDuringLoad.ifOverlapWithLoadDistributionIntervalReturnOverlap(electricSourceInterval);
				
				if(overlapAgentAndElectricSource!=null){
					//IF PARKING					
					// can be transmitted to HUb - thus reduce Vehicle Load 
					reduceAgentVehicleLoadsByGivenLoadInterval(
							agentId, 
							overlapAgentAndElectricSource);
					
					double joulesOfOverlap=DecentralizedSmartCharger.functionSimpsonIntegrator.integrate(
							overlapAgentAndElectricSource.getPolynomialFunction(), overlapAgentAndElectricSource.getStartTime(), overlapAgentAndElectricSource.getEndTime());
					
					
					double revenue=calculateCompensationFeedIn(joulesOfOverlap, agentId);//compensationPerFeedIn 
					addRevenueToAgentFromFeedIn(revenue, agentId);
					addJouleFeedInToAgentStats(joulesOfOverlap, agentId);
										
					
					/*
					 * REDUCE HUB LOAD
					 */
					int hubId=mySmartCharger.myHubLoadReader.getHubForLinkId(
							((ParkingInterval)agentIntervalInAgentDuringLoad).getLocation());
					//joules>0  attribute to hub level means to increase hub load - - =+
					overlapAgentAndElectricSource.negatePolynomialFunc();
					reduceHubLoadByGivenLoadInterval(
							hubId, 
							overlapAgentAndElectricSource);
					
					
				}
			}							
				
		}
	}
	
	
	public void chargeMoreToAccomodateExtraVehicleLoad(Id agentId, 
			LoadDistributionInterval electricSourceInterval
			) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		Schedule agentDuringLoad= 
			mySmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentId).cutScheduleAtTimeSecondHalf(
					electricSourceInterval.getStartTime(), 0.0, agentId);
		
		agentDuringLoad= agentDuringLoad.cutScheduleAtTime(electricSourceInterval.getEndTime(), agentId);	
		
		for(int i=0; i<agentDuringLoad.getNumberOfEntries();i++){
			TimeInterval agentIntervalInAgentDuringLoad= agentDuringLoad.timesInSchedule.get(i);
			if (agentIntervalInAgentDuringLoad.isParking()){
				
				// if overlap between agentInterval && electricSource
				LoadDistributionInterval overlapAgentAndElectricSource=
					agentIntervalInAgentDuringLoad.ifOverlapWithLoadDistributionIntervalReturnOverlap(electricSourceInterval);
				
				if(overlapAgentAndElectricSource!=null){
					//IF PARKING					
					// can be transmitted to HUb - thus reduce Vehicle Load 
					reduceAgentVehicleLoadsByGivenLoadInterval(
							agentId, 
							overlapAgentAndElectricSource);
					
					double joulesOfOverlap=DecentralizedSmartCharger.functionSimpsonIntegrator.integrate(
							overlapAgentAndElectricSource.getPolynomialFunction(), overlapAgentAndElectricSource.getStartTime(), overlapAgentAndElectricSource.getEndTime());
					
					
					// increase charging costs
					// approximation of costs of charging
					double extraCost=approximateChargingCostOfVariableLoad(
							((ParkingInterval)agentIntervalInAgentDuringLoad).getLocation(),
							((ParkingInterval)agentIntervalInAgentDuringLoad).getStartTime(),
							joulesOfOverlap);
								
				
					agentV2GStatistic.get(agentId).addExtraChargingCosts(extraCost);
					
					/*
					 * REDUCE HUB LOAD
					 * loadInterval is negative so negate it first
					 */
					int hubId=mySmartCharger.myHubLoadReader.getHubForLinkId(
							((ParkingInterval)agentIntervalInAgentDuringLoad).getLocation());
					overlapAgentAndElectricSource.negatePolynomialFunc();
					reduceHubLoadByGivenLoadInterval(
							hubId, 
							overlapAgentAndElectricSource);
					
				}
			}		
				
		}
	}
	
	
	
	/**
	 * approximates the charging costs for the interval with variable load by
	 * <li> determining the price for charging per second at the beginning of the interval
	 * <li> assuming that this price is similar over the entire interval
	 * <li> assuming standard connection of 3500W
	 * 
	 * @param linkId
	 * @param time
	 * @param joulesOfOverlap
	 * @return
	 */
	public double approximateChargingCostOfVariableLoad(Id linkId, double time,
			double joulesOfOverlap){
		
				
		double pricingValueOfTime= mySmartCharger.myHubLoadReader.getValueOfPricingPolynomialFunctionAtLinkAndTime(
				linkId, 
				time);
		
		//pricingValueOfTime *1s = CHF/s*1s = 3500W
		
		return Math.abs(pricingValueOfTime*joulesOfOverlap/mySmartCharger.STANDARDCONNECTIONSWATT);
	}
	
	
	
	
	public void addCompensationAndSavedMoneyByRescheduling(Id id, double costKeeping, double costReschedule, double compensation){
		if(mySmartCharger.hasAgentEV(id)){
			totalCompensationEV += compensation;
			if(costKeeping-(costReschedule+compensation)>0){
				totalMoneySavedReschedulingEV += costKeeping-(costReschedule+compensation);// costReschedule already subtracted compensation
			}
			}else{
			totalCompensationPHEV += compensation;		
			if(costKeeping-(costReschedule+compensation)>0){
				totalMoneySavedReschedulingPHEV += costKeeping-(costReschedule+compensation);// costReschedule already subtracted compensation
			}
		}
		
	}
	
	
	public double calcCostForRescheduling(
			Schedule secondHalf, 
			Id agentId, 
			double batterySize, double batteryMin, double batteryMax, 
			String type,
			double currentSOC,
			double compensation		
			) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException{
		
		double costReschedule=0;
		//***********************************
		//EV
		//***********************************
		if(mySmartCharger.hasAgentEV(agentId)){
			answerScheduleAfterElectricSourceInterval= mySmartCharger.getLPEV().solveLPReschedule(secondHalf, agentId, batterySize, batteryMin, batteryMax, type,currentSOC);
			
			if(answerScheduleAfterElectricSourceInterval==null){
				//System.out.println("Reschedule was not possible!");
				costReschedule= Double.MAX_VALUE;
			}else{
				costReschedule=mySmartCharger.calculateChargingCostForAgentSchedule(agentId, answerScheduleAfterElectricSourceInterval)
								;
				if(costReschedule<0){
					costReschedule= Double.MAX_VALUE;
				}else{
					costReschedule=costReschedule-compensation;
				}
			}
			
			
		}else{
			//***********************************
			//PHEV
			//***********************************
			
			answerScheduleAfterElectricSourceInterval= mySmartCharger.getLPEV().solveLPReschedule(secondHalf, 
					agentId, batterySize, batteryMin, batteryMax, 
					type,currentSOC);
			
			if(answerScheduleAfterElectricSourceInterval==null){
				answerScheduleAfterElectricSourceInterval = mySmartCharger.getLPPHEV().solveLPReschedule(
						secondHalf, agentId, batterySize, batteryMin, batteryMax, type, currentSOC);
				//if still no answer possible
				if(answerScheduleAfterElectricSourceInterval==null){
					if(DecentralizedSmartCharger.debug){
						System.out.println("Reschedule was not possible!");
					}
					costReschedule= Double.MAX_VALUE;
				}else{
					costReschedule=mySmartCharger.calculateChargingCostForAgentSchedule(agentId, answerScheduleAfterElectricSourceInterval);
					
					if(costReschedule<0){
						costReschedule= Double.MAX_VALUE;
					}else{
						costReschedule=costReschedule-compensation;
					}
				}
			}else{
				costReschedule=mySmartCharger.calculateChargingCostForAgentSchedule(agentId, answerScheduleAfterElectricSourceInterval);
				if(costReschedule<0){
					costReschedule= Double.MAX_VALUE;
				}else{
					costReschedule=costReschedule-compensation;
				}
			}
			
			
		}
		this.answerScheduleAfterElectricSourceInterval=answerScheduleAfterElectricSourceInterval;
		
		
		return costReschedule;
		
	}
	
	
	
	public double getSOCAtTime(Id agentId, double time) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
//		System.out.println("Schedule to compute SOC at time:"+ time);
		Schedule cutSchedule=mySmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentId).cutScheduleAtTime(time, agentId);
		
//		System.out.println("Schedule CUT compute SOC at time:");
//		cutSchedule.printSchedule();
		double SOCAtStart=cutSchedule.getStartingSOC();
		
		for(int i=0; i<cutSchedule.getNumberOfEntries(); i++){
			TimeInterval t= cutSchedule.timesInSchedule.get(i);
			if(t.isDriving()){
				SOCAtStart=SOCAtStart - ((DrivingInterval) t).getBatteryConsumption();
				
			}else{
				//Parking
				Schedule chargingSchedule= ((ParkingInterval)t).getChargingSchedule();
				if(chargingSchedule!=null){
					double chargingSpeed=((ParkingInterval)t).getChargingSpeed();
					
					SOCAtStart += chargingSpeed*chargingSchedule.getTotalTimeOfIntervalsInSchedule();
				}				
			}
		}
		
		return SOCAtStart;
		
	}






	public Schedule cutScheduleAtTimeFirstHalfAndReassignJoules (Id id, Schedule agentParkingDrivingSchedule, double time) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		Schedule half= agentParkingDrivingSchedule.cutScheduleAtTime (time, id);
		
		return half;
	}
	
	
	
	public Schedule cutScheduleAtTimeSecondHalfAndReassignJoules (Id id, Schedule agentParkingDrivingSchedule, double time) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		Schedule half= agentParkingDrivingSchedule.cutScheduleAtTimeSecondHalf ( time, 0.0, id);
		
		return half;
	}






	

	public double calculateCompensationUpDown(double contributionInJoulesAgent, Id agentId){
		/*
		 * if regulation up   joules<0
		 * if regulation down  joules>0
		 */
		if (contributionInJoulesAgent>0){//down
			double compensationPerkWh=mySmartCharger.getAgentContracts().get(agentId).compensationDown();
			double compensationPerJouleRegulationDown= compensationPerkWh*mySmartCharger.KWHPERJOULE;
			
			return  Math.abs(contributionInJoulesAgent*compensationPerJouleRegulationDown);
		}else{
			double compensationPerkWh=mySmartCharger.getAgentContracts().get(agentId).compensationUp();
			double compensationPerJouleRegulationUp= compensationPerkWh	*mySmartCharger.KWHPERJOULE;
				
			return  Math.abs(contributionInJoulesAgent*compensationPerJouleRegulationUp);
		}
	}
	
	
	public double calculateCompensationFeedIn(double contributionInJoulesAgent, Id agentId){
		/*
		 * if regulation up   joules<0
		 * if regulation down  joules>0
		 */
		if (contributionInJoulesAgent>0){//feedin
			double compensationPerJouleRegulationDown= 
				mySmartCharger.getAgentContracts().get(agentId).compensationUpFeedIn()*mySmartCharger.KWHPERJOULE;
				
			return  Math.abs(contributionInJoulesAgent*compensationPerJouleRegulationDown);
		}else{//charging
			
			return  0.0;
		}
		
	}
		
	
	
	
}
