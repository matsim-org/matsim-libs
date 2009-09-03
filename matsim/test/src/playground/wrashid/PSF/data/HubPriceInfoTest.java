package playground.wrashid.PSF.data;

import junit.framework.TestCase;

public class HubPriceInfoTest extends TestCase {
// TODO: assert, that 96 entries have been read
//
	
	String pathFile="test/input/playground/wrashid/PSF/data/hubPriceInfo.txt";
	
	public void testPrice1(){
		HubPriceInfo hubPriceInfo=new HubPriceInfo(pathFile,4);
		
		double price=hubPriceInfo.getPrice(1.5, 0);
		assertEquals(8.0000000e+001,price);
		
	}
	
	public void testPrice2(){
		HubPriceInfo hubPriceInfo=new HubPriceInfo(pathFile,4);
		
		double price=hubPriceInfo.getPrice(29600.0, 0);
		assertEquals(3.7553204e+002,price);	
	}
	
	public void testPrice3(){
		HubPriceInfo hubPriceInfo=new HubPriceInfo(pathFile,4);
		
		double price=hubPriceInfo.getPrice(42200, 1);
		assertEquals(1.2511799e+002,price);	
	}
	
	public void testTooHighNumberOfHubs(){
		try {
			HubPriceInfo hubPriceInfo=new HubPriceInfo(pathFile,5);
			fail("a run time expection should have been thrown!");
		} catch (Exception exception){
			assertTrue(exception instanceof RuntimeException);
		}
	}
	
	public void testTooLowNumberOfHubs(){
		try {
			HubPriceInfo hubPriceInfo=new HubPriceInfo(pathFile,3);
			fail("a RuntimeException should have been thrown!");
		} catch (Exception exception){
			assertTrue(exception instanceof RuntimeException);
		}
	} 
	
	public void testSetWithWrongNumberOfRows(){
		
		try {
			HubPriceInfo hubPriceInfo=new HubPriceInfo("test/input/playground/wrashid/PSF/data/badHubPriceInfo.txt",4);
			fail("a run time expection should have been thrown!");
		} catch (Exception exception){
			assertTrue(exception instanceof RuntimeException);
		}
	}
	
} 
 