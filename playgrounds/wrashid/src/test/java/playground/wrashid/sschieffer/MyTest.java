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

					myChargerInfo = new DecentralizedChargerInfo(decentralizedChargerV1.calcNumberOfPHEVs(decentralizedChargerV1.controler), decentralizedChargerV1.getAveragePHEVConsumptionInWatt() ); 
					
						// if defineCase=0; valleyTimes are not at beginning or end of day
						// if defineCase=1; valleyTime is at beginning of day
						// if defineCase=2; valleyTime is at  end of day
						// if defineCase=3; valleyTime are at beginning and end
						double [][] caseZero= {{1.0,10}, {10000,15000}};
						double [][] caseOne= {{0.0,10}, {10000,15000}};
						double [][] caseTwo= {{1,10}, {10000,Main.secondsPerDay}};
						double [][] caseThree= {{0,10}, {10000,Main.secondsPerDay}};
						double peakTimes[][];
						
						peakTimes=myChargerInfo.getPeakTimes(caseZero);
						//double [][] caseZero= {{1.0,10}, {10000,15000}};
						assertEquals(0.0, peakTimes[0][0]);
						assertEquals(1.0, peakTimes[0][1]);
						assertEquals(10.0, peakTimes[1][0]);
						assertEquals(10000.0, peakTimes[1][1]);
						assertEquals(15000.0, peakTimes[2][0]);
						assertEquals(Main.secondsPerDay, peakTimes[2][1]);
						
						// if defineCase=1; valleyTime is at beginning of day
						peakTimes=myChargerInfo.getPeakTimes(caseOne);
						assertEquals(10.0, peakTimes[0][0]);
						assertEquals(10000.0, peakTimes[0][1]);
						assertEquals(15000.0, peakTimes[1][0]);
						assertEquals(Main.secondsPerDay, peakTimes[1][1]);
						
						// if defineCase=2; valleyTime is at  end of day
						//double [][] caseTwo= {{1,10}, {10000,Main.secondsPerDay}};
						peakTimes=myChargerInfo.getPeakTimes(caseTwo);
						assertEquals(0.0, peakTimes[0][0]);
						assertEquals(1.0, peakTimes[0][1]);
						assertEquals(10.0, peakTimes[1][0]);
						assertEquals(10000.0, peakTimes[1][1]);
					
						// if defineCase=3; valleyTime are at beginning and end
						//double [][] caseThree= {{0,10}, {10000,Main.secondsPerDay}};
						peakTimes=myChargerInfo.getPeakTimes(caseThree);
						assertEquals(10.0, peakTimes[0][0]);
						assertEquals(10000.0, peakTimes[0][1]);
						
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
	
	public void testArraySortingMethod(){
		double [][] toSort= {{5, 0, 0}, {2, 1, 0},{4, 0, 1}};
		double [][] Sorted= decentralizedChargerV1.minAtStartMaxAtEnd(toSort, 3);
		assertEquals(Sorted[0][0], 2.0);
		assertEquals(Sorted[0][1], 1.0);
		assertEquals(Sorted[0][2], 0.0);
		
		assertEquals(Sorted[2][0], 5.0);
		assertEquals(Sorted[2][1], 0.0);
		assertEquals(Sorted[2][2], 0.0);
		// {{2, 1, 0},{4, 0, 1},{5, 0, 0}}== Sorted);
		
	}
	
	public void testArraySortingMethod2(){
		double [][] toSort= {{5, 0, 0}, {2, 1, 0},{4, 0, 1}};
		double [][] removedSorted=  decentralizedChargerV1.removeEntryIFromDoubleArray(toSort, 0, 3);
		
		assertEquals(removedSorted[0][0], 2.0);
		assertEquals(removedSorted[0][1], 1.0);
		assertEquals(removedSorted[0][2], 0.0);
		
		assertEquals(removedSorted[1][0], 4.0);
		assertEquals(removedSorted[1][1], 0.0);
		assertEquals(removedSorted[1][2], 1.0);
	}
	
	public void testMinAtStartMaxAtEndArray5Entries(){

		//public double[][] minAtStartMaxAtEnd(double [][]d, int elementsPerRow){
		double [][] toSort= {{5, 0, 0}, {2, 1, 0},{4, 0, 1},{6, 0, 7},{1, 0, 2}};
		// expect {{1, 0, 2},{2, 1, 0}, {4, 0, 1},{5, 0, 0},{6, 0, 7},};
		double [][] sorted=  decentralizedChargerV1.minAtStartMaxAtEnd(toSort, 3);
		
		assertEquals(1.0, sorted[0][0]);
		assertEquals(2.0, sorted[1][0]);
		assertEquals(4.0, sorted[2][0]);
		assertEquals(5.0, sorted[3][0]);
		
	}
	
	public void testMinAtStartMaxAtEndArray4Entries(){

		//public double[][] minAtStartMaxAtEnd(double [][]d, int elementsPerRow){
		double [][] toSort= {{5, 0, 0}, {2, 1, 0},{4, 0, 1},{1, 0, 2}};
		// expect {{1, 0, 2},{2, 1, 0}, {4, 0, 1},{5, 0, 0},{6, 0, 7},};
		double [][] sorted=  decentralizedChargerV1.minAtStartMaxAtEnd(toSort, 3);
		
		assertEquals(1.0, sorted[0][0]);
		assertEquals(2.0, sorted[1][0]);
		assertEquals(4.0, sorted[2][0]);
		assertEquals(5.0, sorted[3][0]);
		
	}
	
	
	
}
