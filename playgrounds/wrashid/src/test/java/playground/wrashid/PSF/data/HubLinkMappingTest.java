package playground.wrashid.PSF.data;

import java.util.HashMap;

import junit.framework.TestCase;

public class HubLinkMappingTest extends TestCase {

	public void testReader(){
		HubLinkMapping hublinkMapping=new HubLinkMapping("test/input/playground/wrashid/PSF/data/hublinkMapping.txt",4);
		
		// check, that the right number of links have been read
		assertEquals(31, hublinkMapping.getNumberOfLinks());
		
		// check some of the links, that they have been mapped to the right hub
		assertEquals(1, hublinkMapping.getHubNumber("7308"));
		assertEquals(2, hublinkMapping.getHubNumber("8483"));
		assertEquals(1, hublinkMapping.getHubNumber("7308"));
		assertEquals(3, hublinkMapping.getHubNumber("8791"));
		assertEquals(1, hublinkMapping.getHubNumber("8562"));
	}
	
	public void testReader1(){
		HubLinkMapping hublinkMapping=new HubLinkMapping("test/input/playground/wrashid/PSF/data/hublinkMapping1.txt",4);
		
		// check, that the right number of links have been read
		assertEquals(15, hublinkMapping.getNumberOfLinks());
		
		// check some of the links, that they have been mapped to the right hub
		assertEquals(1, hublinkMapping.getHubNumber("7308"));
		assertEquals(2, hublinkMapping.getHubNumber("8474"));
		
		
	}
	
	
	public void testTooHighNumberOfHubs(){
		try {
			HubLinkMapping hublinkMapping=new HubLinkMapping("test/input/playground/wrashid/PSF/data/hublinkMapping.txt",5);
			fail("a run time expection should have been thrown!");
		} catch (Exception exception){
			assertTrue(exception instanceof RuntimeException);
		}
	}
	
	public void testTooLowNumberOfHubs(){
		try {
			HubLinkMapping hublinkMapping=new HubLinkMapping("test/input/playground/wrashid/PSF/data/hublinkMapping.txt",3);
			fail("a RuntimeException should have been thrown!");
		} catch (Exception exception){
			assertTrue(exception instanceof RuntimeException);
		}
	} 
	
	public void testHashMap(){
		HashMap<String,Integer> linkHubMapping=new HashMap<String,Integer>();
		int a=1;
		linkHubMapping.put(Integer.toString(a), a);
		assertEquals (1,linkHubMapping.get("1").intValue());
	}
	
}     
  