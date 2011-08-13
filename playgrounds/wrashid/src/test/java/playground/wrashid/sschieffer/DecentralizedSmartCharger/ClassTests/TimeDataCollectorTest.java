package playground.wrashid.sschieffer.DecentralizedSmartCharger.ClassTests;

import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.TimeDataCollector;
import junit.framework.TestCase;

public class TimeDataCollectorTest extends TestCase{

	public TimeDataCollectorTest() {		
	}
	
	public void testExtrapolateValueAtTime( ){
		TimeDataCollector myCollector =setUpTimeDataCollector();
		//(0,5), (8*3600,10), (16*3600,20)
		double ex1= myCollector.extrapolateValueAtTimeFromDataCollector(0.25*(3600*24));
		assertEquals(7.5, ex1);
		
		double ex2= myCollector.extrapolateValueAtTimeFromDataCollector(0.75*(3600*24.0));
		assertEquals(15.0, ex2);
		
		
	}
	
	
	public void testIncreaseDatPoint(){
		TimeDataCollector myCollector= setUpTimeDataCollector();
		
		 myCollector.increaseYEntryAtEntryByDouble(0, 10.0);
		assertEquals(15.0, myCollector.getYAtEntry(0));
		
		myCollector.increaseYEntryAtEntryByDouble(1, -20.0);
		assertEquals(-10.0, myCollector.getYAtEntry(1));
		
		
	}
	
	/**
	 * // 3 entries - (0,5), (8*3600,10), (16*3600,20)
	 * @return
	 */
	public TimeDataCollector setUpTimeDataCollector(){
		TimeDataCollector myCollector = new TimeDataCollector(3);
		// 3 entries - (0,5), (8*3600,10), (16*3600,20)
		myCollector.addDataPoint(0, 0.0, 5);
		myCollector.addDataPoint(1, 0.5*(3600*24.0), 10);
		myCollector.addDataPoint(2, (3600*24.0), 20);
		return myCollector;
	}
	
	
}
