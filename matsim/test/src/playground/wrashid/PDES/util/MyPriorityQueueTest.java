package playground.wrashid.PDES.util;

import java.util.LinkedList;
import java.util.PriorityQueue;

import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PDES2.Message;
import playground.wrashid.PDES2.StartingLegMessage;
import playground.wrashid.PDES2.util.ConcurrentListMPDSC;
import playground.wrashid.PDES2.util.MyPriorityQueue;

public class MyPriorityQueueTest extends MatsimTestCase {

	public void testCompareTo(){
		//TODO: continue working here...
		/*
		t_compareTo(1,2);
		t_compareTo(2,1);
		*/
	}
	
	public void t_compareTo(double timeMessage1,double timeMessage2){
		PriorityQueue<MyPriorityQueue> queue=new PriorityQueue<MyPriorityQueue>();
		MyPriorityQueue mpq1=new MyPriorityQueue(new PriorityQueue());
		MyPriorityQueue mpq2=new MyPriorityQueue(new PriorityQueue());
		
		StartingLegMessage m1=new StartingLegMessage(null,null);
		m1.messageArrivalTime=timeMessage1;
		
		mpq1.getQueue().add(m1);
		
		m1=new StartingLegMessage(null,null);
		m1.messageArrivalTime=timeMessage1;
		
		mpq2.getQueue().add(m1);
		
		queue.add(mpq1);
		queue.add(mpq2);
		
		if (timeMessage1<timeMessage2){
			assertEquals(true,queue.peek().equals(mpq1));
		} else if (timeMessage1>timeMessage2) {
			System.out.println(queue.peek());
			System.out.println(mpq2);
			System.out.println(mpq1);
			assertEquals(true,queue.peek().equals(mpq2));
		}
	}
	
	
}
