package playground.wrashid.sschieffer;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ConventionalVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import junit.framework.TestCase;
import org.junit.Test;


public class MyTest extends TestCase {

	public static DecentralizedChargerV1 decentralizedChargerV1;
	
	
	@Test  public void testDecentralizedChargerv1(){
		// create a checkDecentralized ChargerV1 object and test it
		String configPath="test/input/playground/wrashid/sschieffer/config.xml";
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		
		Controler controler=new Controler(configPath);
		Main.parkingTimesPlugin = new ParkingTimesPlugin(controler);
		eventHandlerAtStartupAdder.addEventHandler(Main.parkingTimesPlugin);
		controler.addControlerListener(new EnergyConsumptionInit());
		controler.addControlerListener(eventHandlerAtStartupAdder);
		controler.setOverwriteFiles(true);
		controler.addControlerListener(new IterationEndsListener() {
		
		public DecentralizedChargerInfo myChargerInfo;
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				decentralizedChargerV1=new DecentralizedChargerV1(event.getControler(),Main.energyConsumptionPlugin,Main.parkingTimesPlugin);

				try {

					myChargerInfo = new DecentralizedChargerInfo(decentralizedChargerV1.getPeakLoad(), decentralizedChargerV1.calcNumberOfPHEVs(decentralizedChargerV1.controler), decentralizedChargerV1.getAveragePHEVConsumption(), decentralizedChargerV1.getPriceBase(), decentralizedChargerV1.getPricePeak()); 
					//add Tests for...
				} catch (Exception e) {
					e.printStackTrace();
				}				
			}
		});

		
		controler.run();	
		

	}
	
	/*
	 * Check the calculation of PHEVs according to penetration in Decenralized Charger
	 */
	@Test public void testCalcNumberOfPHEVs(){
		double realResult=decentralizedChargerV1.calcNumberOfPHEVs(decentralizedChargerV1 .controler);
		double expectedResult=decentralizedChargerV1.controler.getPopulation().getPersons().size()*Main.penetrationPercent;
		assertEquals(realResult, expectedResult);
	}
	
	
	
	@Test public void testAreVehiclesSameClass(){
		PlugInHybridElectricVehicle dummyPHEV= new PlugInHybridElectricVehicle(new IdImpl(1));
		ConventionalVehicle dummyCar= new ConventionalVehicle(null, new IdImpl(2));
		assertFalse(decentralizedChargerV1.areVehiclesSameClass(dummyPHEV, dummyCar));
		assertTrue(decentralizedChargerV1.areVehiclesSameClass(dummyPHEV, dummyPHEV));
	}
	
	/**
	 * 
	 * @param d pass Decentralized object to use its function
	 */
	@Test public void testSumLinkedListEntries(){
		LinkedList<Double> l=new LinkedList();
		l.add(1.0);
		l.add(2.0);
		l.add(3.0);
		
		assertEquals(6.0, decentralizedChargerV1.sumUpLinkedListEntries(l));
	}
	
	
	//myChargerInfo.getBaseLoadCurve();
	
	
}
