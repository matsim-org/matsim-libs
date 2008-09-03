package playground.wrashid.test.root.PDES.util;

import java.util.LinkedList;

import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PDES.util.ConcurrentListMPDSC;
import playground.wrashid.PDES2.Message;
import playground.wrashid.PDES2.StartingLegMessage;

public class ConcurrentListMPDSCTest extends MatsimTestCase {

	public void testAddMessageAndFlushEverything(){
		ConcurrentListMPDSC list=new ConcurrentListMPDSC(5,100);
		StartingLegMessage m1=new StartingLegMessage(null,null);
		m1.messageArrivalTime=10;
		list.add(m1, 0);
		
		m1=new StartingLegMessage(null,null);
		m1.messageArrivalTime=20;
		list.add(m1, 0);
		
		list.flushEverything();
		
		LinkedList<Message> messages = list.getCucurrencySafeElements();
		assertEquals(2,messages.size());
		assertEquals(10.0,messages.poll().messageArrivalTime);
		assertEquals(20.0,messages.poll().messageArrivalTime);
	}
	
}
