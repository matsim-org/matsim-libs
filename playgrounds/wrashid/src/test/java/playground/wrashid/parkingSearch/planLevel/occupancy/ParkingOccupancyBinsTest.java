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
	
		pob.inrementParkingOccupancy(0, 900);
		
		assertEquals(1, pob.getOccupancy(0));
		assertEquals(1, pob.getOccupancy(905));
		assertEquals(0, pob.getOccupancy(1800));
		
		pob.inrementParkingOccupancy(60*60*24-5, 60*60*24+5);
		
		assertEquals(2, pob.getOccupancy(0));
		assertEquals(1, pob.getOccupancy(60*60*24-5));
	}	
	
}
