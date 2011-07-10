package playground.wrashid.parkingChoice;

import playground.wrashid.parkingChoice.trb2011.ParkingDefControler;
import junit.framework.TestCase;

public class IntegrationTest  extends TestCase {

	public void testBase(){
		String[] args=new String[1];
		args[0]="test/input/playground/wrashid/parkingChoice/utils/chessConfig5.xml";
		ParkingDefControler.main(args);
	}
	
}
