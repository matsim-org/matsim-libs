package playground.wrashid.PSF.data;

import junit.framework.TestCase;

public class HubPriceInfoTest extends TestCase {
// assert, that 96 entries have been read
//
	
	public void test1(){
		HubPriceInfo hubPriceInfo=new HubPriceInfo("test/input/playground/wrashid/PSF/data/hublinkMapping.txt",4);
		
		double price=hubPriceInfo.getPrice(1.5, 0);
		// CONTINUE here...
		//assertEquals(8.0000000e+001,price);
		
	}
	
		
}
 