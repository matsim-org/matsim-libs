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
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import junit.framework.TestCase;
import lpsolve.LpSolveException;

public class testDecentralizedSmartCharger extends TestCase{

	/**
	 * @param args
	 */
	final String outputPath="C:\\Users\\stellas\\Output\\V1G\\";
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
		
		
		testSchedule t = new testSchedule();
		t.runTest();
		
		//*****************************************
		
		final ParkingTimesPlugin parkingTimesPlugin;
		final EnergyConsumptionPlugin energyConsumptionPlugin;
				
		final LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution= readHubsTest();
		final HubLinkMapping hubLinkMapping=new HubLinkMapping(hubLoadDistribution.size());//= new HubLinkMapping(0);
		
		
				
		final String outputPath="C:\\Users\\stellas\\Output\\V1G\\";
		
		final double phev=1.0;
		final double ev=0.0;
		final double combustion=0.0;
		
		final double gasJoulesPerLiter = 43.0*1000000.0;// Benzin 42,7–44,2 MJ/kg
		final double emissionPerLiterEngine = 23.2/10; // 23,2kg/10l= xx/mass   1kg=1l
		
		final double bufferBatteryCharge=0.0;
		
		final double batterySizeEV= 17*3600*1000; 
		final double batterySizePHEV= 17*3600*1000; 
		final double batteryMinEV= 0.1; 
		final double batteryMinPHEV= 0.1; 
		final double batteryMaxEV= 0.9; 
		final double batteryMaxPHEV= 0.9; 
		
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
							gasJoulesPerLiter,
							emissionPerLiterEngine
							);
					
					myDecentralizedSmartCharger.setBatteryConstants(
							batterySizeEV, 
							batterySizePHEV,
							batteryMinEV,
							batteryMinPHEV,
							batteryMaxEV,
							batteryMaxPHEV);
					
					myDecentralizedSmartCharger.initializeLP(bufferBatteryCharge);
					
					myDecentralizedSmartCharger.initializeChargingSlotDistributor(MINCHARGINGLENGTH);
					
					myDecentralizedSmartCharger.setLinkedListValueHashMapVehicles(
							e.getVehicles());
					
					myDecentralizedSmartCharger.initializeHubLoadDistributionReader(
							hubLinkMapping, hubLoadDistribution);
					
					
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
							
							/*//Parking Interval 	 start: 0.0	  end: 21609.0	  ChargingTime:  0.0	  Optimal:  true	  Joules per Interval:  1.897038219281625E14
							Driving Interval 	  start: 21609.0	  end: 22846.0	  consumption: 1.6388045816871705E7
							Parking Interval 	 start: 22846.0	  end: 36046.0	  ChargingTime:  4635.074196830796	  Optimal:  true	  Joules per Interval:  6.53919159828E14
							Driving Interval 	  start: 36046.0	  end: 38386.0	  consumption: 4.879471387207076E7
							Parking Interval 	 start: 38386.0	  end: 62490.0	  ChargingTime:  13988.57142857143	  Optimal:  true	  Joules per Interval:  3.5063335651397495E15
							Parking Interval 	 start: 62490.0	  end: 86400.0	  ChargingTime:  0.0	  Optimal:  false	  Joules per Interval:  -3.3411427243079994E14
							2011-03-31 14:54:21,105  INFO Controler:475 ### ITERATION 0 ENDS*/
							assertEquals(6, s.getNumberOfEntries());
							assertEquals(62490.0, s.timesInSchedule.get(4).getEndTime());
							ParkingInterval p= (ParkingInterval) s.timesInSchedule.get(0);
							assertEquals(1.897038219281625E14, p.getJoulesInInterval());
							
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
					
					//*****************************************
					//*****************************************
					
					myDecentralizedSmartCharger.assignChargingTimes();
					
					PolynomialFunction func= DecentralizedSmartCharger.myHubLoadReader.getPolynomialFunctionAtLinkAndTime(p5th.getLocation(),
							p5th.getStartTime(),
							p5th.getEndTime());
					
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
	
	
	
		
	
	
	public LinkedListValueHashMap<Integer, Schedule> readHubsTest() throws IOException{
		LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution1= new  LinkedListValueHashMap<Integer, Schedule>();
		hubLoadDistribution1.put(1, makeBullshitScheduleTest());
		hubLoadDistribution1.put(2, makeBullshitScheduleTest());
		hubLoadDistribution1.put(3, makeBullshitScheduleTest());
		hubLoadDistribution1.put(4, makeBullshitScheduleTest());
		return hubLoadDistribution1;
		
	}
	
	
	
	public Schedule makeBullshitScheduleTest() throws IOException{
		
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
		l1.makeXYSeries();
		bullShitSchedule.addTimeInterval(l1);
		
		
		LoadDistributionInterval l2= new LoadDistributionInterval(					
				62490.0,
				DecentralizedSmartCharger.SECONDSPERDAY,
				bullShitFunc2,//p
				false//boolean
		);
		l2.makeXYSeries();
		bullShitSchedule.addTimeInterval(l2);
		
		bullShitSchedule.visualizeLoadDistribution("BullshitSchedule");	
		return bullShitSchedule;
	}
	
	
	public void mapHubsTest(Controler controler, HubLinkMapping hubLinkMapping){
		/*LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution, 
		HubLinkMapping hubLinkMapping, 
		Controler controler){*/
	
	
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
	
	
	
	


	
}


