package playground.wrashid.parkingSearch.planLevel.occupancy;

import junit.framework.TestCase;

public class ParkingCapacityFullLoggerTest extends TestCase {

	public void testIsParkingFullAtTime(){
		ParkingCapacityFullLogger pcfl=new ParkingCapacityFullLogger();
		
		pcfl.logParkingFull(10.0);
		pcfl.logParkingNotFull(12.0);
		
		assertEquals(true, pcfl.isParkingFullAtTime(11.0));
		assertEquals(true, pcfl.isParkingFullAtTime(10.0));
		assertEquals(true, pcfl.isParkingFullAtTime(12.0));
		assertEquals(false, pcfl.isParkingFullAtTime(13.0));
		
		pcfl.logParkingFull(20.0);
		pcfl.logParkingNotFull(4.0);
		
		assertEquals(true, pcfl.isParkingFullAtTime(22.0));
		assertEquals(true, pcfl.isParkingFullAtTime(3.0));
		assertEquals(false, pcfl.isParkingFullAtTime(6.0));
		assertEquals(true, pcfl.isParkingFullAtTime(11.0));
	}
	
	public void testDoesParkingGetFullInInterval(){
		ParkingCapacityFullLogger pcfl=new ParkingCapacityFullLogger();
		
		pcfl.logParkingFull(10.0);
		pcfl.logParkingNotFull(12.0);
		
		pcfl.logParkingFull(18.0);
		pcfl.logParkingNotFull(20.0);
		
		assertEquals(false, pcfl.doesParkingGetFullInInterval(14.0, 16.0));
		assertEquals(true, pcfl.doesParkingGetFullInInterval(11.0, 16.0));
		assertEquals(false, pcfl.doesParkingGetFullInInterval(25.0, 3.0));
		assertEquals(true, pcfl.doesParkingGetFullInInterval(25.0, 11.0));
	}
	
}
