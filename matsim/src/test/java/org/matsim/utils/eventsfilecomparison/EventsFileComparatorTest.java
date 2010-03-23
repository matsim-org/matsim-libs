package org.matsim.utils.eventsfilecomparison;

import org.matsim.testcases.MatsimTestCase;

public class EventsFileComparatorTest  extends MatsimTestCase{

	public void testRetCode0() {
		String f1 = getInputDirectory().replace("/testRetCode0/", "") + "/events0.xml.gz";
		String f2 = getInputDirectory().replace("/testRetCode0/", "") + "/events5.xml.gz";
		EventsFileComparator e = new EventsFileComparator(f1, f2);
		int i = e.compareEvents();
		assertEquals("return val = 0",0,i);
		
		e = new EventsFileComparator(f2, f1);
		i = e.compareEvents();
		assertEquals("return val = 0",0,i);
	}
	
	public void testRetCodeM1() {
		String f1 = getInputDirectory().replace("/testRetCodeM1/", "") + "/events0.xml.gz";
		String f2 = getInputDirectory().replace("/testRetCodeM1/", "") + "/events1.xml.gz";
		EventsFileComparator e = new EventsFileComparator(f1, f2);
		int i = e.compareEvents();
		assertEquals("return val = 0",-1,i);
		
		e = new EventsFileComparator(f2, f1);
		i = e.compareEvents();
		assertEquals("return val = 0",-1,i);
	}
	
	public void testRetCodeM2() {
		String f1 = getInputDirectory().replace("/testRetCodeM2/", "") + "/events0.xml.gz";
		String f2 = getInputDirectory().replace("/testRetCodeM2/", "") + "/events2.xml.gz";
		EventsFileComparator e = new EventsFileComparator(f1, f2);
		int i = e.compareEvents();
		assertEquals("return val = 0",-2,i);
		
		e = new EventsFileComparator(f2, f1);
		i = e.compareEvents();
		assertEquals("return val = 0",-2,i);		
	}
	
	public void testRetCodeM3() {
		String f1 = getInputDirectory().replace("/testRetCodeM3/", "") + "/events0.xml.gz";
		String f2 = getInputDirectory().replace("/testRetCodeM3/", "") + "/events3.xml.gz";
		EventsFileComparator e = new EventsFileComparator(f1, f2);
		int i = e.compareEvents();
		assertEquals("return val = 0",-3,i);
		
		e = new EventsFileComparator(f2, f1);
		i = e.compareEvents();
		assertEquals("return val = 0",-3,i);		
	}
	
	public void testRetCodeM4() {
		String f1 = getInputDirectory().replace("/testRetCodeM4/", "") + "/events0.xml.gz";
		String f2 = getInputDirectory().replace("/testRetCodeM4/", "") + "/events4.xml.gz";
		EventsFileComparator e = new EventsFileComparator(f1, f2);
		int i = e.compareEvents();
		assertEquals("return val = 0",-4,i);
		
		e = new EventsFileComparator(f2, f1);
		i = e.compareEvents();
		assertEquals("return val = 0",-4,i);		
	}
}
