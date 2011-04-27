
package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.LinkedList;

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
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import junit.framework.TestCase;
import lpsolve.LpSolveException;

/**
 * tests AgentTimeIntervalReader
 * @author Stella
 *
 */
public class AgentTimeIntervalReaderTest extends TestCase{

	/**
	 * @param args
	 */
	final String outputPath="D:\\ETH\\MasterThesis\\TestOutput\\";
	String configPath="test/input/playground/wrashid/sschieffer/config.xml";
	final Controler controler=new Controler(configPath);
	
	
	public Id agentOne=null;
	
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
	public void testDecentralizedCharger() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, OptimizationException, IOException{
		
		
		final ParkingTimesPlugin parkingTimesPlugin;
		final EnergyConsumptionPlugin energyConsumptionPlugin;
		
		final double optimalPrice=0.1;
		final double suboptimalPrice=optimalPrice*3;
		final double gasPrice=optimalPrice*2;
			
		final LinkedListValueHashMap<Integer, Schedule> deterministicHubLoadDistribution= readHubsTest();
		final LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution=readHubsPricingTest(optimalPrice, suboptimalPrice);
		
		
		final HubLinkMapping hubLinkMapping=new HubLinkMapping(deterministicHubLoadDistribution.size());//= new HubLinkMapping(0);
		
		
		final double phev=1.0;
		final double ev=0.0;
		final double combustion=0.0;
		
		
		final double bufferBatteryCharge=0.0;
		
		double gasPricePerLiter= 0.25; 
		double gasJoulesPerLiter = 43.0*1000000.0;// Benzin 42,7–44,2 MJ/kg
		double emissionPerLiter = 23.2/10; // 23,2kg/10l= xx/mass   1kg=1l
		
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
					
					LinkedListValueHashMap<Integer, Schedule> locationSourceMapping= new LinkedListValueHashMap<Integer, Schedule>();
					//hub/LoadDIstributionSchedule
					
					LinkedListValueHashMap<Id, Schedule> agentVehicleSourceMapping= new LinkedListValueHashMap<Id, Schedule>();
					
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
					
					/***********************************
					 * AGENTTIMEINTERVALREADER
					 * *********************************
					 */
					
					
					for(Id id : controler.getPopulation().getPersons().keySet()){
						
						System.out.println(id.toString());
						if (id.toString().equals("1")){
							agentOne=id;
							
							Schedule parkingTimes= 
								myDecentralizedSmartCharger.myAgentTimeReader.readParkingTimes(id, new Schedule());
							System.out.println("Parking Times:");
							parkingTimes.printSchedule();
							
							assertEquals(parkingTimes.getNumberOfEntries(), 3);
							assertEquals(parkingTimes.timesInSchedule.get(0).getEndTime(), 21609.0);
							assertEquals(((ParkingInterval)parkingTimes.timesInSchedule.get(0)).getRequiredChargingDuration(), -1.0);
							assertEquals(parkingTimes.timesInSchedule.get(0).isParking(), true);
							/*
							 * Parking Times:
							 *************************
							Starting SOC: 0.0
							Parking Interval 	 start: 0.0	  end: 21609.0	  ChargingTime:  -1.0	  Optimal:  false	  Joules per Interval:  0.0	  ChargingSchedule:  0
							Parking Interval 	 start: 22846.0	  end: 36046.0	  ChargingTime:  -1.0	  Optimal:  false	  Joules per Interval:  0.0	  ChargingSchedule:  0
							Parking Interval 	 start: 38386.0	  end: 86400.0	  ChargingTime:  -1.0	  Optimal:  false	  Joules per Interval:  0.0	  ChargingSchedule:  0
							 *************************
							 */
							
							
							Schedule parkingAndDriving= myDecentralizedSmartCharger.myAgentTimeReader.addDrivingTimes(agentOne, parkingTimes);
							//Schedule s= myDecentralizedSmartCharger.getAllAgentParkingAndDrivingSchedules().getValue(id);
							System.out.println("Parking and Driving Times:");
							parkingAndDriving.printSchedule();
							//check first parking interval
							assertEquals(parkingAndDriving.getNumberOfEntries(), 5);
							
							assertEquals(parkingAndDriving.timesInSchedule.get(0).getEndTime(), 21609.0);
							assertEquals(((ParkingInterval)parkingAndDriving.timesInSchedule.get(0)).getRequiredChargingDuration(), -1.0);
							assertEquals(parkingAndDriving.timesInSchedule.get(0).isParking(), true);
							
							//check driving interval
							assertEquals(parkingAndDriving.timesInSchedule.get(1).getEndTime(), 22846.0);							
							assertEquals(((DrivingInterval)parkingAndDriving.timesInSchedule.get(1)).getConsumption(),1.6388045816871705E7);
							assertEquals(parkingAndDriving.timesInSchedule.get(1).isDriving(), true);
							
							/*
							Parking and Driving Times:
*************************
Starting SOC: 0.0
Parking Interval 	 start: 0.0	  end: 21609.0	  ChargingTime:  -1.0	  Optimal:  false	  Joules per Interval:  0.0	  ChargingSchedule:  0
Driving Interval 	  start: 21609.0	  end: 22846.0	  consumption: 1.6388045816871705E7
Parking Interval 	 start: 22846.0	  end: 36046.0	  ChargingTime:  -1.0	  Optimal:  false	  Joules per Interval:  0.0	  ChargingSchedule:  0
Driving Interval 	  start: 36046.0	  end: 38386.0	  consumption: 4.879471387207076E7
Parking Interval 	 start: 38386.0	  end: 86400.0	  ChargingTime:  -1.0	  Optimal:  false	  Joules per Interval:  0.0	  ChargingSchedule:  0
*************************

							 */
							
							
							// checkIntervalsForSubandOptimal
							parkingAndDriving= myDecentralizedSmartCharger.myAgentTimeReader.
												checkTimesWithHubSubAndOptimalTimes(parkingAndDriving, agentOne);
							
							System.out.println("Parking and Driving Times Checked Times:");
							//parkingAndDriving.printSchedule();
							
							/*//getJoulesForEach
							parkingAndDriving= myDecentralizedSmartCharger.myAgentTimeReader.
									getJoulesForEachParkingInterval(agentOne, parkingTimes);
							System.out.println("Parking and Driving Times Checked Times And Joules:");
							parkingAndDriving.printSchedule();*/
						}
						
					}
					
					
					
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
		
		double[] bullshitCoeffs = new double[]{100, 5789, 56};// 
		double[] bullshitCoeffs2 = new double[]{-22, 5.6, -2.5};
		
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
		bullShitSchedule.printSchedule();
		
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
	bullShitSchedule.printSchedule();
	
	return bullShitSchedule;
}

	
	
	

	
}


