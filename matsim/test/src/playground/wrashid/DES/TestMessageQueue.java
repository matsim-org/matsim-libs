package playground.wrashid.DES;

import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.DES.utils.DummyMessage;

public class TestMessageQueue extends MatsimTestCase {
	public void testPutMessage1(){
		MessageQueue mq=new MessageQueue();
		Message m1=new DummyMessage();
		m1.messageArrivalTime=1;
		
		Message m2=new DummyMessage();
		m2.messageArrivalTime=2;
		
		mq.putMessage(m1);
		mq.putMessage(m2);
		assertEquals(2, mq.getQueueSize());
		assertEquals(true, mq.getNextMessage()==m1);
	}
	
	public void testPutMessage2(){
		MessageQueue mq=new MessageQueue();
		Message m1=new DummyMessage();
		m1.messageArrivalTime=2;
		
		Message m2=new DummyMessage();
		m2.messageArrivalTime=1;
		
		mq.putMessage(m1);
		mq.putMessage(m2);
		assertEquals(2, mq.getQueueSize());
		assertEquals(true, mq.getNextMessage()==m2);
	}
	
	public void testPutMessage3(){
		MessageQueue mq=new MessageQueue();
		Message m1=new DummyMessage();
		m1.messageArrivalTime=2;
		
		Message m2=new DummyMessage();
		m2.messageArrivalTime=1;
		
		Message m3=new DummyMessage();
		m3.messageArrivalTime=1;
		
		mq.putMessage(m1);
		mq.putMessage(m2);
		mq.putMessage(m3);
		assertEquals(3, mq.getQueueSize());
		assertEquals(true, mq.getNextMessage().messageArrivalTime==1);
	}
	
	public void testRemoveMessage1(){
		MessageQueue mq=new MessageQueue();
		Message m1=new DummyMessage();
		m1.messageArrivalTime=1;
		
		Message m2=new DummyMessage();
		m2.messageArrivalTime=2;
		
		mq.putMessage(m1);
		mq.putMessage(m2);
		mq.removeMessage(m1);
		assertEquals(1, mq.getQueueSize());
		assertEquals(true, mq.getNextMessage()==m2);
		assertEquals(0, mq.getQueueSize());
	}
	
	public void testRemoveMessage2(){
		MessageQueue mq=new MessageQueue();
		Message m1=new DummyMessage();
		m1.messageArrivalTime=1;
		
		Message m2=new DummyMessage();
		m2.messageArrivalTime=2;
		
		mq.putMessage(m1);
		mq.putMessage(m2);
		mq.removeMessage(m2);
		assertEquals(1, mq.getQueueSize());
		assertEquals(true, mq.getNextMessage()==m1);
		assertEquals(0, mq.getQueueSize());
	}
	
	public void testRemoveMessage3(){
		MessageQueue mq=new MessageQueue();
		Message m1=new DummyMessage();
		m1.messageArrivalTime=1;
		
		Message m2=new DummyMessage();
		m2.messageArrivalTime=1;
		
		mq.putMessage(m1);
		mq.putMessage(m2);
		mq.removeMessage(m1);
		assertEquals(1, mq.getQueueSize());
		assertEquals(false, mq.isEmpty());
		assertEquals(true, mq.getNextMessage()==m2);
		assertEquals(0, mq.getQueueSize());
		assertEquals(true, mq.isEmpty());
	}
	
	
	
}
