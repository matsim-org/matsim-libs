
package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.io.IOException;

import junit.framework.TestCase;
import lpsolve.LpSolveException;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.OptimizationException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

/**
 * tests methods:
 * <li> attributeSuperfluousVehicleLoadsToGridIfPossible
 * <li> reduceAgentVehicleLoadsByGivenLoadInterval
 * @author Stella
 *
 */
public class V2GTestOnePlan extends TestCase{

	/**
	 * @param args
	 */
	final String outputPath="D:\\ETH\\MasterThesis\\TestOutput\\";
	String configPath="test/input/playground/wrashid/sschieffer/config_plans1.xml";
	final Controler controler=new Controler(configPath);
	
	
	public Id agentOne=null;
	
	public static V2G myV2G;
	
	public static DecentralizedSmartCharger myDecentralizedSmartCharger;
	
	
	public static void testMain(String[] args) throws MaxIterationsExceededException, OptimizationException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException {
				
	}
	
	
	/**
	*  
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws LpSolveException
	 * @throws OptimizationException
	 * @throws IOException
	 */
	public void testV2G() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, OptimizationException, IOException{
		
		
		
		final ParkingTimesPlugin parkingTimesPlugin;
		final EnergyConsumptionPlugin energyConsumptionPlugin;
		
		double gasPricePerLiter= 0.25; 
		double gasJoulesPerLiter = 43.0*1000000.0;// Benzin 42,7–44,2 MJ/kg
		double emissionPerLiter = 23.2/10; // 23,2kg/10l= xx/mass   1kg=1l
		//--> gas Price per second for PHEV 4.6511627906976747E-4
		
		
		double optimalPrice=0.25*1/1000*1/3600*3500;//0.25 CHF per kWh		
		double suboptimalPrice=optimalPrice*3; // cost/second  
		//0.24*10^-4 EUR per second
			
		final LinkedListValueHashMap<Integer, Schedule> deterministicHubLoadDistribution= readHubsTest();
		final LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution=readHubsPricingTest(optimalPrice, suboptimalPrice);
		
		
		final HubLinkMapping hubLinkMapping=new HubLinkMapping(deterministicHubLoadDistribution.size());//= new HubLinkMapping(0);
		
		
		final double phev=1.0;
		final double ev=0.0;
		final double combustion=0.0;
		
		
		final double bufferBatteryCharge=0.0;
		
		
		GasType normalGas=new GasType("normal gas", 
				gasJoulesPerLiter, 
				gasPricePerLiter, 
				emissionPerLiter);
		
		
		/*
		 * Battery characteristics:
		 * - full capacity [J]
		 * e.g. common size is 24kWh = 24kWh*3600s/h*1000W/kW = 24*3600*1000Ws= 24*3600*1000J
		 * - minimum level of state of charge, avoid going below this SOC= batteryMin
		 * (0.1=10%)
		 * - maximum level of state of charge, avoid going above = batteryMax
		 * (0.9=90%)
		 * 
		 * Create desired Battery Types
		 */
		double batterySizeEV= 24*3600*1000; 
		double batterySizePHEV= 24*3600*1000; 
		double batteryMinEV= 0.1; 
		double batteryMinPHEV= 0.1; 
		double batteryMaxEV= 0.9; 
		double batteryMaxPHEV= 0.9; 		
		
		Battery EVBattery = new Battery(batterySizeEV, batteryMinEV, batteryMaxEV);
		Battery PHEVBattery = new Battery(batterySizePHEV, batteryMinPHEV, batteryMaxPHEV);
		
		
		VehicleType EVTypeStandard= new VehicleType("standard EV", 
				EVBattery, 
				null, 
				new ElectricVehicle(null, new IdImpl(1)),
				80000);// Nissan leaf 80kW Engine
		
		VehicleType PHEVTypeStandard= new VehicleType("standard PHEV", 
				PHEVBattery, 
				normalGas, 
				new PlugInHybridElectricVehicle(new IdImpl(1)),
				80000);
		
		final VehicleTypeCollector myVehicleTypes= new VehicleTypeCollector();
		myVehicleTypes.addVehicleType(EVTypeStandard);
		myVehicleTypes.addVehicleType(PHEVTypeStandard);
		
		
		final double MINCHARGINGLENGTH=5*60;//5 minutes
		
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		
		parkingTimesPlugin = new ParkingTimesPlugin(controler);
		
		eventHandlerAtStartupAdder.addEventHandler(parkingTimesPlugin);
		
		final EnergyConsumptionInit e= new EnergyConsumptionInit(
				phev, ev, combustion);
		
		controler.addControlerListener(e);
				
		controler.addControlerListener(eventHandlerAtStartupAdder);
		
		controler.setOverwriteFiles(true);
		
		controler.addControlerListener(new IterationEndsListener() {
			
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				
				try {
					
					
					mapHubsTest(controler,hubLinkMapping);
					
					DecentralizedSmartCharger myDecentralizedSmartCharger = new DecentralizedSmartCharger(
							event.getControler(), 
							parkingTimesPlugin,
							e.getEnergyConsumptionPlugin(),
							outputPath,
							myVehicleTypes
							);
					
					
					myDecentralizedSmartCharger.initializeLP(bufferBatteryCharge);
					
					myDecentralizedSmartCharger.initializeChargingSlotDistributor(MINCHARGINGLENGTH);
					
					myDecentralizedSmartCharger.setLinkedListValueHashMapVehicles(
							e.getVehicles());
					
					myDecentralizedSmartCharger.initializeHubLoadDistributionReader(
							hubLinkMapping, 
							deterministicHubLoadDistribution,							
							pricingHubDistribution
							);
					
					myDecentralizedSmartCharger.run();
					
					/***********************************
					 * V2G
					 * *********************************
					 */
					
					
					LinkedListValueHashMap<Integer, Schedule> stochasticLoad = readStochasticLoad(1);
						
					LinkedListValueHashMap<Id, Schedule> agentVehicleSourceMapping =
						makeAgentVehicleSourceNegativeAndPositive(controler);
					
					linkedListIntegerPrinter(stochasticLoad, "Before stochastic general");
					
					linkedListIdPrinter(agentVehicleSourceMapping,  "Before agent");
					
					myDecentralizedSmartCharger.setStochasticSources(
							stochasticLoad, 
							null, 
							agentVehicleSourceMapping);
					
					// setting agent Contracts
					
					double compensationPerKWHRegulationUp=0.15;
					double compensationPerKWHRegulationDown=0.15;
					 
					AgentContractCollector myAgentContractsCollector= new AgentContractCollector (
							myDecentralizedSmartCharger,
							 compensationPerKWHRegulationUp,
							 compensationPerKWHRegulationDown);
					
					LinkedListValueHashMap<Id, ContractTypeAgent> agentContracts= 
						myAgentContractsCollector.makeAgentContracts(
								controler,
								0,
								0,
								1);
					
					myDecentralizedSmartCharger.setAgentContracts(agentContracts);
					
					// instead of initalize and run go through steps
					myV2G= new V2G(myDecentralizedSmartCharger);
					myDecentralizedSmartCharger.setV2G(myV2G);
					
					System.out.println("START CHECKING VEHICLE SOURCES");
					
					
					/*
					 * CHECK reduceAgentVehicleLoadsByGivenLoadInterval
					 */
					
					for(Id id: controler.getPopulation().getPersons().keySet()){
						System.out.println("AGENT VEHICLE SOURCE BEFORE V2G of -3500 between 0-300");
						
						agentVehicleSourceMapping.getValue(id).printSchedule();
						//0-2000 at -3500
						// -3500+3500=0
						myV2G.reduceAgentVehicleLoadsByGivenLoadInterval(
								id, 
								new LoadDistributionInterval(0,
										300, 
										new PolynomialFunction(new double[]{-3500}),
										false));
						
						System.out.println("AGENT VEHICLE SOURCE AFTER V2G of -3500 between 0-300");
						agentVehicleSourceMapping.getValue(id).printSchedule();
						
						LoadDistributionInterval lFirst= 
							(LoadDistributionInterval) agentVehicleSourceMapping.getValue(id).timesInSchedule.get(0);
						
						LoadDistributionInterval lSecond= 
							(LoadDistributionInterval) agentVehicleSourceMapping.getValue(id).timesInSchedule.get(1);
						
						assertEquals(agentVehicleSourceMapping.getValue(id).getNumberOfEntries(), 3);
						
						assertEquals(lFirst.getEndTime(),
								300.0);
						assertEquals(lFirst.getPolynomialFunction().getCoefficients()[0],
								0.0);
						assertEquals(lSecond.getPolynomialFunction().getCoefficients()[0],
								-3500.0);
						
						
						//20000.0, 20300.0  at 3500
						// 3500-(3500)=0
						myV2G.reduceAgentVehicleLoadsByGivenLoadInterval(
								id, 
								new LoadDistributionInterval(20000.0,
										20300, 
										new PolynomialFunction(new double[]{3500}),
										true));
						
						
						System.out.println("AGENT VEHICLE SOURCE AFTER V2G of 3500 between 20000-20300");
						agentVehicleSourceMapping.getValue(id).printSchedule();
						//writing out
						lFirst= 
							(LoadDistributionInterval) agentVehicleSourceMapping.getValue(id).timesInSchedule.get(0);
						
						lSecond= 
							(LoadDistributionInterval) agentVehicleSourceMapping.getValue(id).timesInSchedule.get(1);
						
						LoadDistributionInterval lThird= 
							(LoadDistributionInterval) agentVehicleSourceMapping.getValue(id).timesInSchedule.get(2);
						
						
						assertEquals(agentVehicleSourceMapping.getValue(id).getNumberOfEntries(), 3);
						
						assertEquals(lFirst.getEndTime(),
								300.0);
						assertEquals(lFirst.getPolynomialFunction().getCoefficients()[0],
								0.0);
						assertEquals(lSecond.getPolynomialFunction().getCoefficients()[0],
								-3500.0);
						
						assertEquals(lThird.getPolynomialFunction().getCoefficients()[0],
								0.0);
						assertEquals(lThird.getEndTime(),
								20300.0);
						
						
						
						/*
						 * CHECK
						 * 
						 * attributeSuperfluousVehicleLoadsToGridIfPossible(Id agentId, 
			Schedule agentParkingDrivingSchedule, 
			LoadDistributionInterval electricSourceInterval)
						 */
						
						System.out.println("check: attributeSuperfluousVehicleLoadsToGridIfPossible");
						
						System.out.println("hubLoad at beginning");
						Schedule hubSchedule=myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadDistribution.getValue(1);
						hubSchedule.printSchedule();
						
						
						System.out.println("agent schedule");
						Schedule agentParkingDrivingSchedule= 
							myDecentralizedSmartCharger.getAllAgentParkingAndDrivingSchedules().getValue(id);
						agentParkingDrivingSchedule.printSchedule();
						//attribute in Parking time - should be successful
						myV2G.attributeSuperfluousVehicleLoadsToGridIfPossible(id, 
								agentParkingDrivingSchedule, 
								new LoadDistributionInterval(0, 
										300, 
										new PolynomialFunction(new double[]{-2000}), 
										false));
						
						System.out.println("hub schedule after attributing -2000 to grid between 0-300");
						hubSchedule=myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadDistribution.getValue(1);
						hubSchedule.printSchedule();
						
						//attribute in Driving time - cannot be successful
						myV2G.attributeSuperfluousVehicleLoadsToGridIfPossible(id, 
								agentParkingDrivingSchedule, 
								new LoadDistributionInterval(21600.0, 
										22000, 
										new PolynomialFunction(new double[]{-2000}), 
										false));
						System.out.println("hub schedule after attributing -2000  to grid between 21600-22000 driving time");
						
						hubSchedule=myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadDistribution.getValue(1);
						hubSchedule.printSchedule();
						
						lFirst= 
							(LoadDistributionInterval)hubSchedule.timesInSchedule.get(0);
						
						
						assertEquals(hubSchedule.getNumberOfEntries(), 2);
						
						assertEquals(1500.0, lFirst.getPolynomialFunction().getCoefficients()[0]);
						
						
						/*
						 * CHECK
						 * reduceHubLoadByGivenLoadInterval
						 */
						System.out.println("CHECK: reduceHubLoadByGivenLoadInterval");
						
						myV2G.reduceHubLoadByGivenLoadInterval(1, 
								new LoadDistributionInterval(21600.0, 
										22000, 
										new PolynomialFunction(new double[]{-2000}), 
										false));
						System.out.println("hub schedule after reducing it by 2000 between 21600-22000 driving time");
						
						hubSchedule=myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadDistribution.getValue(1);
						hubSchedule.printSchedule();
						//bullshit
						//3500+2000=5500
						assertEquals(hubSchedule.getNumberOfEntries(), 4);
						lFirst= 
							(LoadDistributionInterval)hubSchedule.timesInSchedule.get(0);
						lSecond= 
							(LoadDistributionInterval)hubSchedule.timesInSchedule.get(1);
						lThird= 
							(LoadDistributionInterval)hubSchedule.timesInSchedule.get(2);
						assertEquals(1500.0, lFirst.getPolynomialFunction().getCoefficients()[0]);
						assertEquals(3500.0, lSecond.getPolynomialFunction().getCoefficients()[0]);
						assertEquals(5500.0, lThird.getPolynomialFunction().getCoefficients()[0]);
						
					}
					
										
					/*
					 * reassign stochastic load and check results after  checkVehicleLoads()
					 * and checkHubStochasticLoads();
					 */
					
					//System.out.println("START CHECKING STOCHASTIC HUB LOADS");
					//myDecentralizedSmartCharger.checkHubStochasticLoads();
					
					
					/*
					linkedListIntegerPrinter(stochasticLoad, "After stochastic general");
					linkedListIdPrinter(agentVehicleSourceMapping,  "After agent");
					*/
					
				} catch (Exception e1) {
					
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
							
			}
		});
		controler.run();
		
		
		//*****************************************
		//*****************************************
		
		
		
		
	}
	
	
	public void linkedListIdPrinter(LinkedListValueHashMap<Id, Schedule> list, String info){
		System.out.println("Print LinkedList "+ info);
		for(Id id: list.getKeySet()){
			list.getValue(id).printSchedule();
		}
		
	}
	
	public void linkedListIntegerPrinter(LinkedListValueHashMap<Integer, Schedule> list, String info){
		System.out.println("Print LinkedList "+ info);
		for(Integer id: list.getKeySet()){
			list.getValue(id).printSchedule();
		}
		
	}
	
	
	
	
	public LinkedListValueHashMap<Id, Schedule> makeAgentVehicleSourceNegativeAndPositive(Controler controler){
		LinkedListValueHashMap<Id, Schedule> agentSource= 
			new LinkedListValueHashMap<Id, Schedule>();
		
		//Id
		for(Id id : controler.getPopulation().getPersons().keySet()){
			
				Schedule bullShitMinus= new Schedule();
				PolynomialFunction pMinus = new PolynomialFunction(new double[] {-3500.0});
				bullShitMinus.addTimeInterval(new LoadDistributionInterval(0, 2000.0, pMinus, false));
				
				PolynomialFunction pPlus = new PolynomialFunction(new double[] {3500.0});
				bullShitMinus.addTimeInterval(new LoadDistributionInterval(20000.0, 20300.0, pPlus, true));
				
				
				agentSource.put(id, bullShitMinus);	
			
			
		}
		return agentSource;
	}
	
	
	public static LinkedListValueHashMap<Integer, Schedule> readHubsTest() throws IOException{
		LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution1= new  LinkedListValueHashMap<Integer, Schedule>();
		hubLoadDistribution1.put(1, makeBullshitScheduleTest());
		hubLoadDistribution1.put(2, makeBullshitScheduleTest());
		hubLoadDistribution1.put(3, makeBullshitScheduleTest());
		hubLoadDistribution1.put(4, makeBullshitScheduleTest());
		return hubLoadDistribution1;
		
	}
	
	
	public static LinkedListValueHashMap<Integer, Schedule> readHubsPricingTest(double optimal, double suboptimal) throws IOException{
		LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution1= new  LinkedListValueHashMap<Integer, Schedule>();
		hubLoadDistribution1.put(1, makeBullshitPricingScheduleTest(optimal, suboptimal));
		hubLoadDistribution1.put(2, makeBullshitPricingScheduleTest(optimal, suboptimal));
		hubLoadDistribution1.put(3, makeBullshitPricingScheduleTest(optimal, suboptimal));
		hubLoadDistribution1.put(4, makeBullshitPricingScheduleTest(optimal, suboptimal));
		return hubLoadDistribution1;
		
	}
	
	
	
	
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
	
	
	public void mapHubsTest(Controler controler, HubLinkMapping hubLinkMapping){
		
	
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
	}
	
	
	
public static LinkedListValueHashMap<Integer, Schedule> readStochasticLoad(int num){
		
		LinkedListValueHashMap<Integer, Schedule> stochastic= new LinkedListValueHashMap<Integer, Schedule>();
		
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

	
	
	

	
}


