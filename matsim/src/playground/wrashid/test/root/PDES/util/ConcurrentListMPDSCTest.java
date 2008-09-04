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
		
		// TODO: uncomment this.
		list.flushEverything();
		
		LinkedList<Message>[] messages = list.getCucurrencySafeElements();
		assertEquals(2,messages[0].size());
		assertEquals(10.0,messages[0].poll().messageArrivalTime);
		assertEquals(20.0,messages[0].poll().messageArrivalTime);
		
		messages = list.getCucurrencySafeElements();
		//assertEquals(messages,null);
	}
		
	public void testFlushAllInputBuffers(){
		t_FlushAllInputBuffersBorderCases(5,0,0);
		t_FlushAllInputBuffersBorderCases(5,0,1);
		t_FlushAllInputBuffersBorderCases(20,3,1);
		t_FlushAllInputBuffersBorderCases(30,3,1);
		
		t_FlushAllInputBuffersBorderCases2(70,3,0,10.0);
		t_FlushAllInputBuffersBorderCases2(70,3,1,20.0);
	}
	
	// considering normal messages
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
		
		// TODO: uncomment this.
		list.flushAllInputBuffers(queueTime);
		
		LinkedList<Message>[] messages = list.getCucurrencySafeElements();
		if (messages==null){
			assertEquals(expectedNumberOfMessages,0);
		} else {
			assertEquals(expectedNumberOfMessages,messages[0].size());
			assertEquals(10.0,messages[0].poll().messageArrivalTime);
		}
	}
	
	// considering the out of order messages
	private void t_FlushAllInputBuffersBorderCases2(double queueTime, int expectedNumberOfMessages, int minListSize, double timeOfFirstOutput){
		ConcurrentListMPDSC list=new ConcurrentListMPDSC(5,minListSize);
		StartingLegMessage m1=new StartingLegMessage(null,null);
		
		list.flushAllInputBuffers(50);
		
		m1.messageArrivalTime=30;
		list.add(m1, 0);
		
		m1=new StartingLegMessage(null,null);
		m1.messageArrivalTime=20;
		list.add(m1, 0);
		
		m1=new StartingLegMessage(null,null);
		m1.messageArrivalTime=10;
		list.add(m1, 0);
		
		// TODO: uncomment this.
		list.flushAllInputBuffers(queueTime);
		
		LinkedList<Message>[] messages = list.getCucurrencySafeElements();
		if (messages==null){
			assertEquals(expectedNumberOfMessages,0);
		} else {
			assertEquals(expectedNumberOfMessages,messages[0].size());
			assertEquals(timeOfFirstOutput,messages[0].poll().messageArrivalTime);
		}
	}
	
	
	
}
