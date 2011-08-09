package vrp.algorithms.ruinAndRecreate.basics;

import vrp.VRPTestCase;
import vrp.api.Customer;
import vrp.basics.Tour;

public class TourActivityStatusUpdaterImplTest extends VRPTestCase{
	
	Tour tour;
	
	public void setUp(){
		init();
		Customer depot = getDepot();
		
		tour = new Tour();
		
	}
	
	public void tearDown(){
		
	}
	
	public void testUpdate(){
		String foo = "foo";
		assertEquals("foo", foo);
	}

}
