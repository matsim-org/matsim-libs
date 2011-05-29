package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.OptimizationException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;

import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.sschieffer.DSC.AgentContractCollector;
import playground.wrashid.sschieffer.DSC.ContractTypeAgent;
import playground.wrashid.sschieffer.DSC.DecentralizedSmartCharger;
import playground.wrashid.sschieffer.DSC.DrivingInterval;
import playground.wrashid.sschieffer.DSC.EnergyConsumptionInit;
import playground.wrashid.sschieffer.DSC.LoadDistributionInterval;
import playground.wrashid.sschieffer.DSC.ParkingInterval;
import playground.wrashid.sschieffer.DSC.Schedule;
import playground.wrashid.sschieffer.DSC.VehicleTypeCollector;

public class TestSimulationSetUp {

	
	final Controler controler;
	final ParkingTimesPlugin parkingTimesPlugin;
	final EnergyConsumptionInit energyConsumptionInit;
	
	final VehicleTypeCollector myVehicleTypes;
	
	final double optimalPrice=0.25*1/1000*1/3600*3500;//0.25 CHF per kWh		
	final double suboptimalPrice=optimalPrice*3; // cost/second  
	
	HashMap<Integer, Schedule> deterministicHubLoadDistribution;
	HashMap<Integer, Schedule> pricingHubDistribution;
	HashMap<Integer, Schedule> stochastic;
	HashMap<Id, Schedule> agentSource;
	HashMap<Id, ContractTypeAgent> agentContracts;
	/**
	 * 
	 * @param configPath
	 * @param phev
	 * @param ev
	 * @param combustion
	 * @throws IOException 
	 */
	public TestSimulationSetUp(String configPath, 
			double electrification, 
			double ev
			) throws IOException{
		
		controler=new Controler(configPath);
		
		
		parkingTimesPlugin = new ParkingTimesPlugin(controler);
		
		energyConsumptionInit= new EnergyConsumptionInit(
				electrification, ev);
		
		deterministicHubLoadDistribution= readHubsTest();
		pricingHubDistribution=readHubsPricingTest(optimalPrice, suboptimalPrice);
		
		TestVehicleCollectorSetUp vehicleSetup= new TestVehicleCollectorSetUp();		
		myVehicleTypes = vehicleSetup.setUp();
		
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		
		eventHandlerAtStartupAdder.addEventHandler(parkingTimesPlugin);
		
		controler.addControlerListener(energyConsumptionInit);
				
		controler.addControlerListener(eventHandlerAtStartupAdder);
		
		controler.setOverwriteFiles(true);
		
	}
	
	
	
	/**
	 * sets up standard Decentralized Smart CHarger so that it can be run in the next step; 
	 * Debug option is true
	 * @param outputPath
	 * @param bufferBatteryCharge
	 * @param standardChargingSlotLength
	 * @return
	 * @throws OptimizationException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public DecentralizedSmartCharger setUpSmartCharger(
			String outputPath,
			double bufferBatteryCharge,
			double standardChargingSlotLength) throws OptimizationException, IOException, InterruptedException{
		DecentralizedSmartCharger myDecentralizedSmartCharger = new DecentralizedSmartCharger(
				controler, 
				getParkingTimesPlugIn(),
				getEnergyConsumptionInit().getEnergyConsumptionPlugin(),
				outputPath,
				getVehicleTypeCollector()
				);
		
		myDecentralizedSmartCharger.initializeLP(bufferBatteryCharge, false);
		
		myDecentralizedSmartCharger.initializeChargingSlotDistributor(standardChargingSlotLength);
		
		myDecentralizedSmartCharger.setLinkedListValueHashMapVehicles(
				getEnergyConsumptionInit().getElectricVehicles());
		
		myDecentralizedSmartCharger.initializeHubLoadDistributionReader(
				mapHubsTest(), 
				getDeterministicLoadSchedule(),							
				getDeterministicPricing(),
				optimalPrice,
				suboptimalPrice
				);
		myDecentralizedSmartCharger.setDebug(true);
		
		return myDecentralizedSmartCharger;
	}
	
	
	
	
	public void setUpStochasticLoadDistributions() throws IOException{
			
		stochastic= readStochasticLoad(1);
		agentSource= makeAgentVehicleSourceNegativeAndPositive();
	}
	
	
	/**
	 * sets up AgentContractCollector and return LinkedListValueHashMap<Id, ContractTypeAgent> agentContract 
	 * according to percentages of contracts 
	 * 
	 * @param myDecentralizedSmartCharger
	 * @param compensationPerKWHRegulationUp
	 * @param compensationPerKWHRegulationDown
	 * @param xPercentNone
	 * @param xPercentDown
	 * @param xPercentDownUp
	 * @return
	 */
	public void setUpAgentSchedules(
			DecentralizedSmartCharger myDecentralizedSmartCharger,
			double compensationPerKWHRegulationUp,
			double compensationPerKWHRegulationDown,
			double compensationPERKWHFeedInVehicle,
			double xPercentDown,
			double xPercentDownUp){
		
		AgentContractCollector myAgentContractsCollector= new AgentContractCollector (
				myDecentralizedSmartCharger,
				 compensationPerKWHRegulationUp,
				 compensationPerKWHRegulationDown,
				 compensationPERKWHFeedInVehicle);
		
		agentContracts= myAgentContractsCollector.makeAgentContracts(
					controler,
					xPercentDown,
					xPercentDownUp);
		
	}
	
	public HashMap<Id, ContractTypeAgent> getAgentContracts(){
		return agentContracts;
	}
	
	
	public HashMap<Id, Schedule> getAgentStochasticLoadSources(){
		return agentSource;
	}
	
	public HashMap<Integer, Schedule> getStochasticLoadSchedule(){
		return stochastic;
	}
	
	public HashMap<Integer, Schedule> getDeterministicLoadSchedule(){
		return deterministicHubLoadDistribution;
	}
	
	public HashMap<Integer, Schedule> getDeterministicPricing(){
		return pricingHubDistribution;
	}
	
	public VehicleTypeCollector getVehicleTypeCollector(){
		return myVehicleTypes;
	}
	
	public EnergyConsumptionInit getEnergyConsumptionInit(){
		return energyConsumptionInit;
	}
	
	public ParkingTimesPlugin getParkingTimesPlugIn(){
		return parkingTimesPlugin;
	}
	
	public Controler getControler(){
		return controler;
	}
	
	
	/**
	 * 0.0 -- 62490.0    10
	   62490.0 -- Ende  -10	
	 * @return
	 * @throws IOException
	 */
	public static HashMap<Integer, Schedule> readHubsTest() throws IOException{
		HashMap<Integer, Schedule> hubLoadDistribution1= new  HashMap<Integer, Schedule>();
		hubLoadDistribution1.put(1, makeBullshitScheduleTest());
		/*hubLoadDistribution1.put(2, makeBullshitScheduleTest());
		hubLoadDistribution1.put(3, makeBullshitScheduleTest());
		hubLoadDistribution1.put(4, makeBullshitScheduleTest());*/
		return hubLoadDistribution1;
		
	}
	
	
	
	/**
	 * 0.0 -- 62490.0    optimal
	   62490.0 -- Ende   suboptimal
	 * @param optimal
	 * @param suboptimal
	 * @return
	 * @throws IOException
	 */
	public static HashMap<Integer, Schedule> readHubsPricingTest(double optimal, double suboptimal) throws IOException{
		HashMap<Integer, Schedule> hubLoadDistribution1= new  HashMap<Integer, Schedule>();
		hubLoadDistribution1.put(1, makeBullshitPricingScheduleTest(optimal, suboptimal));
	/*	hubLoadDistribution1.put(2, makeBullshitPricingScheduleTest(optimal, suboptimal));
		hubLoadDistribution1.put(3, makeBullshitPricingScheduleTest(optimal, suboptimal));
		hubLoadDistribution1.put(4, makeBullshitPricingScheduleTest(optimal, suboptimal));*/
		return hubLoadDistribution1;
		
	}
	
	
	
	/**
	 * 0.0 -- 62490.0    10
	   62490.0 -- Ende  -10		
	 * @return
	 * @throws IOException
	 */
	public static Schedule makeBullshitScheduleTest() throws IOException{
		
		Schedule bullShitSchedule= new Schedule();
		
		double[] bullshitCoeffs = new double[]{10};// 
		double[] bullshitCoeffs2 = new double[]{-10};
		
		PolynomialFunction bullShitFunc= new PolynomialFunction(bullshitCoeffs);
		PolynomialFunction bullShitFunc2= new PolynomialFunction(bullshitCoeffs2);
		LoadDistributionInterval l1= new LoadDistributionInterval(
				0.0,
				62490.0,
				bullShitFunc,//p
				true//boolean
		);
		
		bullShitSchedule.addTimeInterval(l1);
		
		
		LoadDistributionInterval l2= new LoadDistributionInterval(					
				62490.0,
				DecentralizedSmartCharger.SECONDSPERDAY,
				bullShitFunc2,//p
				false//boolean
		);
	
		bullShitSchedule.addTimeInterval(l2);
		//bullShitSchedule.printSchedule();
		
		return bullShitSchedule;
	}
	
	
	public HubLinkMapping mapHubsTest(){
		
		HubLinkMapping hubLinkMapping = new HubLinkMapping(1);
		for (Link link:controler.getNetwork().getLinks().values()){
			hubLinkMapping.addMapping(link.getId().toString(), 1);
			
		}
		return hubLinkMapping;
	}
	
	
	/**
	 * 4 hubs
	 * @return
	 */
	public HubLinkMapping mapHubsTest4(){
		
		HubLinkMapping hubLinkMapping = new HubLinkMapping(4);
		double maxX=5000;
		double minX=-20000;
		double diff= maxX-minX;
		
		for (Link link:controler.getNetwork().getLinks().values()){
			// x values of equil from -20000 up to 5000
			if (link.getCoord().getX()<(minX+diff)/4){
				
				hubLinkMapping.addMapping(link.getId().toString(), 1);
			}else{
				if (link.getCoord().getX()<(minX+diff)*2/4){
					hubLinkMapping.addMapping(link.getId().toString(), 2);
				}else{
					if (link.getCoord().getX()<(minX+diff)*3/4){
						hubLinkMapping.addMapping(link.getId().toString(), 3);
					}else{
						hubLinkMapping.addMapping(link.getId().toString(), 4);
					}
				}
			}
			
		}
		return hubLinkMapping;
	}
	
	
	
		/**
		 * all day 3500W for 1,2 ... num hubs
		 * @param num
		 * @return
		 */
		public static HashMap<Integer, Schedule> readStochasticLoad(int num){
			
			HashMap<Integer, Schedule> stochastic= new HashMap<Integer, Schedule>();
			
			Schedule bullShitStochastic= new Schedule();
			PolynomialFunction p = new PolynomialFunction(new double[] {3500});
			
			bullShitStochastic.addTimeInterval(new LoadDistributionInterval(0, 24*3600, p, true));
			for (int i=0; i<num; i++){
				stochastic.put(i+1, bullShitStochastic);
			}
			return stochastic;
		
			
		}
		
			
		
	
	public static Schedule makeBullshitPricingScheduleTest(double optimal, double suboptimal) throws IOException{
		
		Schedule bullShitSchedule= new Schedule();
		
		PolynomialFunction pOpt = new PolynomialFunction(new double[] {optimal});	
		PolynomialFunction pSubopt = new PolynomialFunction(new double[] {suboptimal});
		
		
		LoadDistributionInterval l1= new LoadDistributionInterval(
				0.0,
				62490.0,
				pOpt,//p
				true//boolean
		);
		
		bullShitSchedule.addTimeInterval(l1);
		
		
		LoadDistributionInterval l2= new LoadDistributionInterval(					
				62490.0,
				DecentralizedSmartCharger.SECONDSPERDAY,
				pSubopt,//p
				false//boolean
		);
	
		bullShitSchedule.addTimeInterval(l2);
		//bullShitSchedule.printSchedule();
		
		return bullShitSchedule;
	}

	
	
	//-3500 between 0 and 2000
	// 3500 between 20000 and 20300
	public HashMap<Id, Schedule> makeAgentVehicleSourceNegativeAndPositive(){
		HashMap<Id, Schedule> agentSource= 
			new HashMap<Id, Schedule>();
		
		//Id
		for(Id id : energyConsumptionInit.getElectricVehicles().getKeySet()){
			
				Schedule bullShitMinus= new Schedule();
				PolynomialFunction pMinus = new PolynomialFunction(new double[] {-3500.0});
				bullShitMinus.addTimeInterval(new LoadDistributionInterval(0, 2000.0, pMinus, false));
				
				PolynomialFunction pPlus = new PolynomialFunction(new double[] {3500.0});
				bullShitMinus.addTimeInterval(new LoadDistributionInterval(20000.0, 20300.0, pPlus, true));
				
				agentSource.put(id, bullShitMinus);	
		}
		return agentSource;
	}

	
	/**
	 * makes and returns fake schedule
	 * </br>
	 * Parking 0  10  true  joules =100
		 * Parking 10  20 false joules =100
		 * Driving 20  30  consumption =1
		 * Parking 30  40  false joules =100
	 * 
	 * @return
	 */
	public Schedule makeFakeSchedule(){
		Id linkId=null;
		for (Link link:controler.getNetwork().getLinks().values())
		{
			linkId=link.getId();
			break;
		}
		/*
		 * Parking 0  10  true  joules =100
		 * Parking 10  20 false joules =100
		 * Driving 20  30  consumption =1
		 * Parking 30  40  false joules =100
		 */
		Schedule s= new Schedule();
		ParkingInterval p1= new ParkingInterval (0, 10, linkId);
		p1.setParkingOptimalBoolean(true);
		p1.setJoulesInPotentialChargingInterval(100);
		
		ParkingInterval p2= new ParkingInterval (10, 20, linkId);
		p2.setParkingOptimalBoolean(false);
		p2.setJoulesInPotentialChargingInterval(-100);
		
		DrivingInterval d3 = new DrivingInterval(20, 30, 1);
		
		ParkingInterval p4= new ParkingInterval (30, 40, linkId);
		p4.setParkingOptimalBoolean(false);
		p4.setJoulesInPotentialChargingInterval(-100);
		
		s.addTimeInterval(p1);
		s.addTimeInterval(p2);
		s.addTimeInterval(d3);
		s.addTimeInterval(p4);
		
		s.addJoulesToTotalSchedule(100);
		s.addJoulesToTotalSchedule(-200);
		
		return s;
	}
}
