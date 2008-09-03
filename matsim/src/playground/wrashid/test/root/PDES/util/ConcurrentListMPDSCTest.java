package playground.wrashid.test.root.PDES.util;

import java.util.LinkedList;

import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PDES.util.ConcurrentListMPDSC;
import playground.wrashid.PDES2.Message;
import playground.wrashid.PDES2.StartingLegMessage;

public class ConcurrentListMPDSCTest extends MatsimTestCase {

	public void testFlushEverything(){
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
		
		messages = list.getCucurrencySafeElements();
		assertEquals(messages,null);
	}
		
	public void testFlushAllInputBuffers(){
		t_FlushAllInputBuffersBorderCases(5,1,0);
		t_FlushAllInputBuffersBorderCases(5,2,1);
		t_FlushAllInputBuffersBorderCases(20,3,1);
		t_FlushAllInputBuffersBorderCases(30,3,1);
	}
	
	private void t_FlushAllInputBuffersBorderCases(double queueTime, int expectedNumberOfMessages, int minListSize){
		ConcurrentListMPDSC list=new ConcurrentListMPDSC(5,minListSize);
		StartingLegMessage m1=new StartingLegMessage(null,null);
		m1.messageArrivalTime=10;
		list.add(m1, 0);
		
		m1=new StartingLegMessage(null,null);
		m1.messageArrivalTime=20;
		list.add(m1, 0);
		
		m1=new StartingLegMessage(null,null);
		m1.messageArrivalTime=30;
		list.add(m1, 0);
		
		list.flushAllInputBuffers(queueTime);
		
		LinkedList<Message> messages = list.getCucurrencySafeElements();
		assertEquals(expectedNumberOfMessages,messages.size());
		assertEquals(10.0,messages.poll().messageArrivalTime);
	}
	
	
	
}
