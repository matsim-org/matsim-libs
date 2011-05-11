package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.io.IOException;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;

import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class TestSimulationSetUp {

	
	final Controler controler;
	final ParkingTimesPlugin parkingTimesPlugin;
	final EnergyConsumptionInit energyConsumptionInit;
	
	final VehicleTypeCollector myVehicleTypes;
	
	final LinkedListValueHashMap<Integer, Schedule> deterministicHubLoadDistribution;
	final LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution;
	
	
	/**
	 * 
	 * @param configPath
	 * @param phev
	 * @param ev
	 * @param combustion
	 * @throws IOException 
	 */
	public TestSimulationSetUp(String configPath, 
			double phev, 
			double ev, 
			double combustion) throws IOException{
		
		controler=new Controler(configPath);
		
		
		parkingTimesPlugin = new ParkingTimesPlugin(controler);
		
		energyConsumptionInit= new EnergyConsumptionInit(
				phev, ev, combustion);
		
		final double optimalPrice=0.25*1/1000*1/3600*3500;//0.25 CHF per kWh		
		final double suboptimalPrice=optimalPrice*3; // cost/second  
			
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
	
	
	public LinkedListValueHashMap<Integer, Schedule> getDeterministicLoadSchedule(){
		return deterministicHubLoadDistribution;
	}
	
	public LinkedListValueHashMap<Integer, Schedule> getDetermisiticPricing(){
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
	public static LinkedListValueHashMap<Integer, Schedule> readHubsTest() throws IOException{
		LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution1= new  LinkedListValueHashMap<Integer, Schedule>();
		hubLoadDistribution1.put(1, makeBullshitScheduleTest());
		hubLoadDistribution1.put(2, makeBullshitScheduleTest());
		hubLoadDistribution1.put(3, makeBullshitScheduleTest());
		hubLoadDistribution1.put(4, makeBullshitScheduleTest());
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
	public static LinkedListValueHashMap<Integer, Schedule> readHubsPricingTest(double optimal, double suboptimal) throws IOException{
		LinkedListValueHashMap<Integer, Schedule> hubLoadDistribution1= new  LinkedListValueHashMap<Integer, Schedule>();
		hubLoadDistribution1.put(1, makeBullshitPricingScheduleTest(optimal, suboptimal));
		hubLoadDistribution1.put(2, makeBullshitPricingScheduleTest(optimal, suboptimal));
		hubLoadDistribution1.put(3, makeBullshitPricingScheduleTest(optimal, suboptimal));
		hubLoadDistribution1.put(4, makeBullshitPricingScheduleTest(optimal, suboptimal));
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
	
	
	/**
	 * 4 hubs
	 * @return
	 */
	public HubLinkMapping mapHubsTest(){
		
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
