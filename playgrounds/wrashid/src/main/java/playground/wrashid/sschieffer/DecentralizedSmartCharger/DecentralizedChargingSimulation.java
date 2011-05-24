package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;

import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.V2G.StochasticLoadCollector;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.scenarios.DetermisticLoadPricingCollector;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.scenarios.HubInfo;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.scenarios.MappingClass;
 

/**
 * convenience class which hides complexity to ease the use to run decentralized smart charging simulations
 * 
 * it handles the in and outputs with the decentralized smart charger 
 * @author Stella
 *
 */
public class DecentralizedChargingSimulation {

	public static Controler controler;
	
	public static ParkingTimesPlugin parkingTimesPlugin;
	
	public static EnergyConsumptionInit energyInit;
	
	public static String outputPath;
	
	public static double bufferBatteryCharge, standardChargingLength;
	
	public static MappingClass myMappingClass;
	
	public static DecentralizedSmartCharger mySmartCharger;
	
	public static DetermisticLoadPricingCollector loadPricingCollector;
	
	public static StochasticLoadCollector slc;
	
	public static double  compensationPerKWHRegulationUp;
	public static double compensationPerKWHRegulationDown;
	public static double compensationPERKWHFeedInVehicle;
	public static double xPercentNone;
	public static double xPercentDown;
	public static double xPercentDownUp;
	public static double electrification;
	public static boolean LPoutput;
	
	/**
	 * Setup for only decentralized smart charging optimization
	 * @param configPath
	 * @param outputPath
	 * @param phev
	 * @param ev
	 * @param combustion
	 * @param bufferBatteryCharge
	 * @param standardChargingLength
	 * @param myMappingClass
	 * @param loadPricingCollector
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 * @throws FunctionEvaluationException 
	 * @throws ConvergenceException 
	 */
	public DecentralizedChargingSimulation(
			String configPath, 
			String outputPath,  
			double electrification, 
			double ev,
			double bufferBatteryCharge,
			double standardChargingLength,
			MappingClass myMappingClass,
			ArrayList<HubInfo> myHubInfo,
			boolean LPoutput
			) throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException, IOException{
		
		this.outputPath=outputPath;
		this.bufferBatteryCharge=bufferBatteryCharge;
		this.standardChargingLength=standardChargingLength;
		this.myMappingClass=myMappingClass;
		this.LPoutput=LPoutput;
		
		loadPricingCollector= new DetermisticLoadPricingCollector(myHubInfo);
		
		controler=new Controler(configPath);
		parkingTimesPlugin= new ParkingTimesPlugin(controler);
		
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		eventHandlerAtStartupAdder.addEventHandler(parkingTimesPlugin);
		controler.addControlerListener(eventHandlerAtStartupAdder);
		this.electrification=electrification;
		energyInit= new EnergyConsumptionInit(
				electrification, ev);
		
		controler.addControlerListener(energyInit);
		controler.setOverwriteFiles(true);
		}
	
	
	/**
	 * adds ControlerListenerDecentralizedCharging
	 */
	public void addControlerListenerDecentralizedCharging(){
		controler.addControlerListener(new DecentralizedChargerAfterIterationListener());
		
	}
	
	/**
	 * Setup for Decentralized Smart Charger and V2G
	 * set variables relevant for V2G and adds an events listener to the controler that will
	 * run the decentralized smart charger and the V2G procedure after each iteration
	 * 
	 * @param xPercentNone
	 * @param xPercentDown
	 * @param xPercentDownUp
	 * @param slc
	 * @param compensationPerKWHRegulationUp
	 * @param compensationPerKWHRegulationDown
	 */
	public void setUpV2G(
			double xPercentDown,
			double xPercentDownUp,
			StochasticLoadCollector slc,
			double compensationPerKWHRegulationUp,
			double compensationPerKWHRegulationDown,
			double compensationPERKWHFeedInVehicle){
	
		this.xPercentNone=xPercentNone;
		this.xPercentDown=xPercentDown;
		this.xPercentDownUp=xPercentDownUp;
		this.slc=slc;
		this.compensationPerKWHRegulationUp=compensationPerKWHRegulationUp;
		this.compensationPerKWHRegulationDown=compensationPerKWHRegulationDown;
		this.compensationPERKWHFeedInVehicle= compensationPERKWHFeedInVehicle;
		
		controler.addControlerListener(new DecentralizedChargerAndV2GAfterIterationListener());		
	}
	
	
	/**
	 * sets the public DecentralizedSmartCharger variable to the defined object
	 * method is called from within DecentralizedChargerAfterIterationListener
	 * @param mySmartCharger
	 */
	public static void setDecentralizedSmartCharger(DecentralizedSmartCharger mySmarty){
		mySmartCharger=mySmarty;
	}
	
	/**
	 * return chronological schedule of agent plans over the whole day,
	 * i.e. parking interval, driving interval, parking interval
	 * @return
	 */
	public HashMap<Id, Schedule> getAllAgentParkingAndDrivingSchedules(){
		return mySmartCharger.getAllAgentParkingAndDrivingSchedules();
	}
		
	
	/**
	 * visualizes the daily plans of all agents (parking, driving and charging times) and saves the files in the format
	 * outputPath+ "DecentralizedCharger\\agentPlans\\"+ id.toString()+"_dayPlan.png"
	 * @throws IOException
	 */
	public void visualizeAllAgentPlans() throws IOException{
		 mySmartCharger.visualizeDailyPlanForAllAgents();
	}
	
	
	/**
	 * visualizes the daily plans of one agent (parking, driving and charging times) and saves the file in the format
	 * outputPath+ "DecentralizedCharger\\agentPlans\\"+ id.toString()+"_dayPlan.png"
	 * @throws IOException
	 */
	public void visualizeAgentPlans(Id id) throws IOException{
		 mySmartCharger.visualizeDailyPlanForAgent(id);
	}
	
	/**
	 * return linkeList with charging costs for all agents for the entire day
	 * @return
	 */
	public HashMap<Id, Double> getChargingCostsForAgents(){
		return mySmartCharger.getChargingCostsForAgents();
	}
	
	/**
	 * return average charging cost for agents
	 * @return
	 */
	public double getAverageChargingCostPerAgentFromSmartCharging(){
		return mySmartCharger.getAverageChargingCostAgents();
	}
	
	/**
	 * return average charging cost for EV agents
	 * @return
	 */
	public double getAverageChargingCostPerEV_AgentFromSmartCharging(){
		return mySmartCharger.getAverageChargingCostEV();
	}
	
	/**
	 * return average charging cost for PHEV agents
	 * @return
	 */
	public double getAverageChargingCostPerPHEV_AgentFromSmartCharging(){
		return mySmartCharger.getAverageChargingCostPHEV();
	}
	
	/**
	 * return linkedList with charging schedules for all agents over the entire day
	 * @return
	 */
	public HashMap<Id, Schedule> getAllAgentChargingSchedules(){
		return mySmartCharger.getAllAgentChargingSchedules();
	}
	
	/**
	 * LIST OF AGENTS WITH EV WHERE LP FAILED, i.e. where battery swap would be necessary
	
	 * @return
	 */
	public LinkedList<Id> getListOfIdsOfEVAgentsWithFailedOptimization(){
		return mySmartCharger.getIdsOfEVAgentsWithFailedOptimization();
	}
	
	
	/**
	 * GET ALL IDs OF AGENTS WITH EV engine car
	 * @return
	 */
	public LinkedList<Id> getListOfAllEVAgents(){
		return mySmartCharger.getAllAgentsWithEV();
	}
	
	/**
	 * GET ALL IDs OF AGENTS WITH PHEV engine car
	 * @return
	 */
	public LinkedList<Id> getListOfAllPHEVAgents(){
		return mySmartCharger.getAllAgentsWithPHEV();
	}
	
	
	public double getTotalDrivingConsumptionOfAgentFromBattery(Id agentId){
		return mySmartCharger.getTotalDrivingConsumptionOfAgentFromBattery(agentId);
	}
	
	public double getTotalDrivingConsumptionOfAgentFromOtherSources(Id agentId){
		return mySmartCharger.getTotalDrivingConsumptionOfAgentFromOtherSources(agentId);
	}
	
	/**
	 * returns total emissions in kg from PHEVs vehicles
	 * @return
	 */
	public double getTotalEmissions(){
		return mySmartCharger.getTotalEmissions();
	}	
	
	
	
	public double getAverageRevenueV2GPerAgent(){
		return mySmartCharger.myV2G.getAverageV2GRevenueAgent();
	}	
	
	
	public double getAverageRevenueV2GPerEV(){
		return mySmartCharger.myV2G.getAverageV2GRevenueEV();
	}
	
	public double getAverageRevenueV2GPerPHEV(){
		return mySmartCharger.myV2G.getAverageV2GRevenuePHEV();
	}
	
	
	public double getTotalJoulesV2GRegulationUp(){
		return mySmartCharger.myV2G.getTotalRegulationUp();
	}
	
	public double getTotalJoulesV2GRegulationUpEV(){
		return mySmartCharger.myV2G.getTotalRegulationUpEV();
	}
	
	public double getTotalJoulesV2GRegulationUpPHEV(){
		return mySmartCharger.myV2G.getTotalRegulationUpPHEV();
	}
	
	public double getTotalJoulesV2GRegulationDown(){
		return mySmartCharger.myV2G.getTotalRegulationDown();
	}
	
	public double getTotalJoulesV2GRegulationDownEV(){
		return mySmartCharger.myV2G.getTotalRegulationDownEV();
	}
	
	public double getTotalJoulesV2GRegulationDownPHEV(){
		return mySmartCharger.myV2G.getTotalRegulationDownPHEV();
	}
	
}
