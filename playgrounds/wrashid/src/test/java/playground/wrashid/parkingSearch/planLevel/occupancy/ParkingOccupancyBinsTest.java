package playground.wrashid.parkingSearch.planLevel.occupancy;

import junit.framework.TestCase;

public class ParkingOccupancyBinsTest extends TestCase {

	public void testBinIndex(){
		ParkingOccupancyBins pob=new ParkingOccupancyBins();
		
		assertEquals(0, pob.getBinIndex(5));
		assertEquals(0, pob.getBinIndex(900-0.5));
		assertEquals(1, pob.getBinIndex(900));
		
		assertEquals(95, pob.getBinIndex(60*60*24-0.5));
		assertEquals(0, pob.getBinIndex(60*60*24));
		assertEquals(0, pob.getBinIndex(60*60*24+0.5));
		assertEquals(1, pob.getBinIndex(60*60*24+900));
	}
	
	public void testOccupancy(){
		ParkingOccupancyBins pob=new ParkingOccupancyBins();
	
		pob.setParkingOccupancy(1, 0);
		pob.setParkingOccupancy(10, 0);
		pob.setParkingOccupancy(5, 0);
	
		assertEquals(1, pob.getMinOccupancy(0));
		assertEquals(10, pob.getMaxOccupancy(0));
		assertEquals(5.33, pob.getAverageOccupancy(0),0.01);
	}	
	
}
