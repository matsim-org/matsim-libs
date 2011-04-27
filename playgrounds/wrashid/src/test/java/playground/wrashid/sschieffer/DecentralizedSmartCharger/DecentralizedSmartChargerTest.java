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

public class DecentralizedSmartChargerTest extends TestCase{

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
	 * 1) CHECKS BASIC FUNCTIONALITY OF INTERVALS AND SCHEDULE
	 * 2) CHECKS RESULTS OF AGENTTIMEINTERVALREADER SPECIFIC TO AGENT 1 AND THIS SETUP
	 * 
	 *  
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws LpSolveException
	 * @throws OptimizationException
	 * @throws IOException
	 */
	public void testDecentralizedCharger() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, OptimizationException, IOException{
		
		
		
		//*****************************************
		
		final ParkingTimesPlugin parkingTimesPlugin;
		final EnergyConsumptionPlugin energyConsumptionPlugin;
		
		final double optimalPrice=0.1;
		final double suboptimalPrice=optimalPrice*3;
		
			
		final LinkedListValueHashMap<Integer, Schedule> deterministicHubLoadDistribution= readHubsTest();
		final LinkedListValueHashMap<Integer, Schedule> stochasticHubLoadDistribution=readStochasticLoad(deterministicHubLoadDistribution.size());
		final LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution=readPricingHubDistribution(optimalPrice, suboptimalPrice);
		final LinkedListValueHashMap<Integer, Schedule> connectivityHubDistribution;
		
		
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
					
					//*****************************************
					//*****************************************
					
					myDecentralizedSmartCharger.readAgentSchedules();
					
					for(Id id : myDecentralizedSmartCharger.getAllAgentParkingAndDrivingSchedules().getKeySet()){
						
						System.out.println(id.toString());
						if (id.toString().equals("1")){
							agentOne=id;
							Schedule s= myDecentralizedSmartCharger.myAgentTimeReader.readParkingAndDrivingTimes(id);
							//Schedule s= myDecentralizedSmartCharger.getAllAgentParkingAndDrivingSchedules().getValue(id);
							s.printSchedule();
							
							assertEquals(6, s.getNumberOfEntries());
							assertEquals(62490.0, s.timesInSchedule.get(4).getEndTime());
							ParkingInterval p= (ParkingInterval) s.timesInSchedule.get(0);
							
							
						}
						
					}
					
					//*****************************************
					//*****************************************
					
					myDecentralizedSmartCharger.findRequiredChargingTimes();
					
					// updated schedules with required charging times					
					Schedule s= myDecentralizedSmartCharger.getAllAgentParkingAndDrivingSchedules().getValue(agentOne);
					s.printSchedule();
					// basics should be the same
					assertEquals(6, s.getNumberOfEntries());
					assertEquals(62490.0, s.timesInSchedule.get(4).getEndTime());
					
					
					// THESE ARE FOR LP AS OF March 31st
					ParkingInterval pFirst= (ParkingInterval) s.timesInSchedule.get(0);
					assertEquals(1.897038219281625E14, pFirst.getJoulesInInterval());
					assertEquals(0.0, pFirst.getRequiredChargingDuration());
					assertEquals(true, pFirst.isInSystemOptimalChargingTime());
					
					ParkingInterval pLast= (ParkingInterval) s.timesInSchedule.get(s.getNumberOfEntries()-1);
					assertEquals(0.0, pLast.getRequiredChargingDuration());
					assertEquals(false, pLast.isInSystemOptimalChargingTime());
					
					ParkingInterval p5th= (ParkingInterval) s.timesInSchedule.get(4);
					assertEquals(13988.57142857143, p5th.getRequiredChargingDuration());
					assertEquals(true, p5th.isInSystemOptimalChargingTime());
					
					/*Parking Interval 	 start: 0.0	  end: 21609.0	  ChargingTime:  0.0	  Optimal:  true	  Joules per Interval:  1.897038219281625E14
					Driving Interval 	  start: 21609.0	  end: 22846.0	  consumption: 1.6388045816871705E7
					Parking Interval 	 start: 22846.0	  end: 36046.0	  ChargingTime:  4635.074196830796	  Optimal:  true	  Joules per Interval:  6.53919159828E14
					Driving Interval 	  start: 36046.0	  end: 38386.0	  consumption: 4.879471387207076E7
					Parking Interval 	 start: 38386.0	  end: 62490.0	  ChargingTime:  13988.57142857143	  Optimal:  true	  Joules per Interval:  3.5063335651397495E15
					Parking Interval 	 start: 62490.0	  end: 86400.0	  ChargingTime:  0.0	  Optimal:  false	  Joules per Interval:  -3.3411427243079994E14
					*/
					
					
					
					
					myDecentralizedSmartCharger.assignChargingTimes();
					
					PolynomialFunction func= DecentralizedSmartCharger.myHubLoadReader.getDeterministicLoadPolynomialFunctionAtLinkAndTime(agentOne,p5th.getLocation(),
							p5th);
					
					Schedule sAssigned = myDecentralizedSmartCharger.myChargingSlotDistributor.assignChargingScheduleForParkingInterval(
							func, 
							p5th.getJoulesInInterval(), 
							p5th.getStartTime(), 
							p5th.getEndTime(), 
							p5th.getRequiredChargingDuration() //13988.57142857143
							);
		
					 
					sAssigned.printSchedule();
					
					int numTimeIntervalSOLL= (int) Math.ceil(p5th.getRequiredChargingDuration()/MINCHARGINGLENGTH);
					
					assertEquals(numTimeIntervalSOLL, sAssigned.getNumberOfEntries());
					
					double totalChargingTime=0;
					
					for (int i=0; i<sAssigned.getNumberOfEntries();i++){
						ChargingInterval c= (ChargingInterval)sAssigned.timesInSchedule.get(i);
						totalChargingTime+=c.getIntervalLength();
					}
					
					/*System.out.println(totalChargingTime);
					System.out.println(p5th.getRequiredChargingDuration());
					13988.571428571428
					13988.57142857143*/
					DecimalFormat tenPlaces = new DecimalFormat("0.0000000000"); 
					assertEquals(tenPlaces.format(totalChargingTime), tenPlaces.format(p5th.getRequiredChargingDuration()));
					
					//*****************************************
					//*****************************************
					//100%
					LinkedList<Id> agentsWithPHEV = myDecentralizedSmartCharger.getAllAgentsWithPHEV();
					
					assertEquals(agentsWithPHEV.size(), 100);
					
					//*****************************************
					//*****************************************
					//TEST CalculateChargingCost For Agent Schedule
					
					
					
					//*****************************************
					//*****************************************
					//TEST LPPHEV
					
					
					
					
					LPPHEVTest testLP = new LPPHEVTest();
					testLP.testRunLPPHEV();
					
									
					
					myDecentralizedSmartCharger.clearResults();
					
					
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
	/*	final LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution;
		final LinkedListValueHashMap<Integer, Schedule> connectivityHubDistribution;*/
		
	}
	
		
	
	
	public static LinkedListValueHashMap<Integer, Schedule> readPricingHubDistribution(double optimal, double suboptimal) throws IOException{
		
		LinkedListValueHashMap<Integer, Schedule> pricing= readHubsTest();
		
		PolynomialFunction pOpt = new PolynomialFunction(new double[] {optimal});
		PolynomialFunction pSubopt = new PolynomialFunction(new double[] {suboptimal});
		
		for(Integer i: pricing.getKeySet()){
			for(int j=0; j<pricing.getValue(i).getNumberOfEntries(); j++){
				// for every time interval for every hub
				if(pricing.getValue(i).timesInSchedule.get(j).isParking()){
					LoadDistributionInterval l = (LoadDistributionInterval) pricing.getValue(i).timesInSchedule.get(j);
					
					if(l.isOptimal()){
						l= new LoadDistributionInterval(
								l.getStartTime(),
								l.getEndTime(), 
								pOpt, 
								true);
					}else{
						l= new LoadDistributionInterval(
								l.getStartTime(),
								l.getEndTime(), 
								pSubopt, 
								false);
						
					}
				}
			}
		}
		return pricing;
	
		
	}

	
	
	

	
}


