package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;

import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
 

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
	
	public static double bufferBatteryCharge, minChargingLength;
	
	public static MappingClass myMappingClass;
	
	public static DecentralizedSmartCharger mySmartCharger;
	
	public static DetermisticLoadPricingCollector loadPricingCollector;
	
	public static StochasticLoadCollector slc;
	
	public static double  compensationPerKWHRegulationUp;
	public static double compensationPerKWHRegulationDown;
	
	public static double xPercentNone;
	public static double xPercentDown;
	public static double xPercentDownUp;
	
	/**
	 * Setup for only decentralized smart charging optimization
	 * @param configPath
	 * @param outputPath
	 * @param phev
	 * @param ev
	 * @param combustion
	 * @param bufferBatteryCharge
	 * @param minChargingLength
	 * @param myMappingClass
	 * @param loadPricingCollector
	 */
	public DecentralizedChargingSimulation(String configPath, 
			String outputPath,  
			double phev, double ev, double combustion,
			double bufferBatteryCharge,
			double minChargingLength,
			MappingClass myMappingClass,
			DetermisticLoadPricingCollector loadPricingCollector){
		
		this.outputPath=outputPath;
		this.bufferBatteryCharge=bufferBatteryCharge;
		this.minChargingLength=minChargingLength;
		this.myMappingClass=myMappingClass;
		this.loadPricingCollector=loadPricingCollector;
		
		controler=new Controler(configPath);
		parkingTimesPlugin= new ParkingTimesPlugin(controler);
		
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		eventHandlerAtStartupAdder.addEventHandler(parkingTimesPlugin);
		controler.addControlerListener(eventHandlerAtStartupAdder);
		
		energyInit= new EnergyConsumptionInit(
				phev, ev, combustion);
		
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
			double xPercentNone,
			double xPercentDown,
			double xPercentDownUp,
			StochasticLoadCollector slc,
			double compensationPerKWHRegulationUp,
			double compensationPerKWHRegulationDown){
	
		this.xPercentNone=xPercentNone;
		this.xPercentDown=xPercentDown;
		this.xPercentDownUp=xPercentDownUp;
		this.slc=slc;
		this.compensationPerKWHRegulationUp=compensationPerKWHRegulationUp;
		this.compensationPerKWHRegulationDown=compensationPerKWHRegulationDown;
		
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
	public LinkedListValueHashMap<Id, Schedule> getAllAgentParkingAndDrivingSchedules(){
		return mySmartCharger.getAllAgentParkingAndDrivingSchedules();
	}
		
	/**
	 * return linkeList with charging costs for all agents for the entire day
	 * @return
	 */
	public LinkedListValueHashMap<Id, Double> getChargingCostsForAgents(){
		return mySmartCharger.getChargingCostsForAgents();
	}
	
	/**
	 * return linkedList with charging schedules for all agents over the entire day
	 * @return
	 */
	public LinkedListValueHashMap<Id, Schedule> getAllAgentChargingSchedules(){
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
	
	/**
	 * GET ALL IDs OF AGENTS Combustion engine car
	 * @return
	 */
	public LinkedList<Id> getListOfAllCombustionAgents(){
		return mySmartCharger.getAllAgentsWithCombustionVehicle();
	}
	
	
	public double getTotalDrivingConsumptionOfAgentFromBattery(Id agentId){
		return mySmartCharger.getTotalDrivingConsumptionOfAgentFromBattery(agentId);
	}
	
	public double getTotalDrivingConsumptionOfAgentFromOtherSources(Id agentId){
		return mySmartCharger.getTotalDrivingConsumptionOfAgentFromOtherSources(agentId);
	}
	
	/**
	 * returns total emissions in kg from PHEVs and Combustion engine vehicles
	 * @return
	 */
	public double getTotalEmissions(){
		return mySmartCharger.getTotalEmissions();
	}	
	
	
	public HashMap<Id, Double> getAgentV2GRevenues(){
		return mySmartCharger.getAgentV2GRevenues();
	}
	
	
}
