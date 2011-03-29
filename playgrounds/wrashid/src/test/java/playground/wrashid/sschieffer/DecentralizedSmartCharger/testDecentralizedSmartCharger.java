package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.io.IOException;
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
	
	public static DecentralizedSmartCharger myDecentralizedSmartCharger;
	
	
	public static void testMain(String[] args) {
		
		
	}
	
	
	public void testReadAgentSchedules() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, OptimizationException, IOException{
		
		setUp();
		
		myDecentralizedSmartCharger.readAgentSchedules();
		
		myDecentralizedSmartCharger.findRequiredChargingTimes();
		
		myDecentralizedSmartCharger.assignChargingTimes();	
		
		//100%
		LinkedList<Id> agentsWithPHEV = myDecentralizedSmartCharger.getAllAgentsWithPHEV();
		Id id= agentsWithPHEV.get(0);
		
		Schedule s1 = myDecentralizedSmartCharger.getAllAgentParkingAndDrivingSchedules().getValue(id);
		
		s1.printSchedule();
		/*Parking Interval 	 start: 0.0	  end: 21609.0	  ChargingTime:  0.0	  Optimal:  true	  Joules per Interval:  1.897038219281625E14
		Driving Interval 	  start: 21609.0	  end: 22846.0	  consumption: 1.6388045816871705E7
		Parking Interval 	 start: 22846.0	  end: 36046.0	  ChargingTime:  4635.074196830796	  Optimal:  true	  Joules per Interval:  6.53919159828E14
		Driving Interval 	  start: 36046.0	  end: 38386.0	  consumption: 4.879471387207076E7
		Parking Interval 	 start: 38386.0	  end: 62490.0	  ChargingTime:  13988.57142857143	  Optimal:  true	  Joules per Interval:  3.5063335651397495E15
		Parking Interval 	 start: 62490.0	  end: 86400.0	  ChargingTime:  0.0	  Optimal:  false	  Joules per Interval:  -3.3411427243079994E14
		*/
		
		assertEquals(1,1);
		// assert consumption
		// assert...
	}
	
	
	public void setUp(){
		final LinkedListValueHashMap<Id, Vehicle> vehicles;
		final ParkingTimesPlugin parkingTimesPlugin;
		final EnergyConsumptionPlugin energyConsumptionPlugin;
		
		final HubLinkMapping hubLinkMapping= new HubLinkMapping(0);
		
		final LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution = new  LinkedListValueHashMap<Integer, Schedule>();
		
		String configPath="test/input/playground/wrashid/sschieffer/config.xml";
				
		final String outputPath="C:\\Users\\stellas\\Output\\V1G\\";
		
		final double phev=1.0;
		final double ev=0.0;
		final double combustion=0.0;
		
		final double gasJoulesPerLiter = 43.0*1000000.0;// Benzin 42,7–44,2 MJ/kg
		final double emissionPerLiterEngine = 23.2/10; // 23,2kg/10l= xx/mass   1kg=1l
		
		final double bufferBatteryCharge=0.0;
		
		final double MINCHARGINGLENGTH=5*60;//5 minutes
		
		final Controler controler=new Controler(configPath);
		
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
					
					readHubs(hubLoadDistribution);
					mapHubs(hubLoadDistribution, 
							hubLinkMapping, 
							controler);
					
					
					DecentralizedSmartCharger myDecentralizedSmartCharger = new DecentralizedSmartCharger(
							event.getControler(), 
							parkingTimesPlugin,
							e.getEnergyConsumptionPlugin(),
							outputPath, 
							MINCHARGINGLENGTH, 
							e.getVehicles(),
							gasJoulesPerLiter,
							emissionPerLiterEngine,
							hubLinkMapping,
							hubLoadDistribution);
					
					
					
					myDecentralizedSmartCharger.run();
					
					LinkedListValueHashMap<Id, Schedule> agentSchedule= 
						myDecentralizedSmartCharger.getAllAgentChargingSchedules();
					
					
					LinkedList<Id> agentsWithEVFailure = 
						myDecentralizedSmartCharger.getIdsOfEVAgentsWithFailedOptimization();
					
					LinkedList<Id> agentsWithEV = myDecentralizedSmartCharger.getAllAgentsWithEV();
					
					if(agentsWithEV.isEmpty()==false){
						Id id= agentsWithEV.get(0);
						myDecentralizedSmartCharger.getAgentChargingSchedule(id).printSchedule();
					}
					
					LinkedList<Id> agentsWithPHEV = myDecentralizedSmartCharger.getAllAgentsWithPHEV();
					
					if(agentsWithEV.isEmpty()==false){
						
						Id id= agentsWithEV.get(0);
						myDecentralizedSmartCharger.getAgentChargingSchedule(id).printSchedule();
						System.out.println("Total consumption from battery [joules]" 
								+ myDecentralizedSmartCharger.getTotalDrivingConsumptionOfAgentFromBattery(id));
						
						System.out.println("Total consumption from engine [joules]" +
								myDecentralizedSmartCharger.getTotalDrivingConsumptionOfAgentFromOtherSources(id));
						
						System.out.println("Total emissions from this agent [joules]" + 
								myDecentralizedSmartCharger.joulesToEmissionInKg(
										myDecentralizedSmartCharger.getTotalDrivingConsumptionOfAgentFromOtherSources(id)));
								
						
						System.out.println("Total consumption [joules]" +
								myDecentralizedSmartCharger.getTotalDrivingConsumptionOfAgent(id));
						
						
						
					}
					
					
					
				} catch (Exception e1) {
					
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
							
			}
		});
		
				
		controler.run();		
	}
	
	
	
	public static void readHubs(LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution) throws IOException{
		hubLoadDistribution = new LinkedListValueHashMap<Integer, Schedule>();		
		hubLoadDistribution.put(2, makeBullshitSchedule());
		hubLoadDistribution.put(3, makeBullshitSchedule());
		hubLoadDistribution.put(4, makeBullshitSchedule());
		
		
	}
	
	
	public static Schedule makeBullshitSchedule() throws IOException{
		
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
	

	public static void mapHubs(LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution, 
			HubLinkMapping hubLinkMapping, 
			Controler controler){
		
		hubLinkMapping=new HubLinkMapping(hubLoadDistribution.size());
		
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


