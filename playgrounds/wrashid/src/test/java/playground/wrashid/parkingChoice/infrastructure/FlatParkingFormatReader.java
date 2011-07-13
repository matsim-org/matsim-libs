package playground.wrashid.parkingChoice.infrastructure;

import org.matsim.testcases.MatsimTestCase;


import junit.framework.TestCase;

public class FlatParkingFormatReader extends MatsimTestCase {
	
	public void testParking1(){
		FlatParkingFormatReaderV1 flatParkingFormatReader = new FlatParkingFormatReaderV1();
		String path=super.getPackageInputDirectory();
		flatParkingFormatReader.parse(path + "flatParkingFormat1.xml");
		
		assertEquals(5, flatParkingFormatReader.getParkings().size());
	}
	
}
