package playground.wrashid.PDES2.util;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.BasicEvent;
import org.matsim.mobsim.jdeqsim.util.Timer;

import playground.wrashid.deqsim.PDESStarter2;
// optimized for multiple producer, single consumer
// the producer decides, when the his inputBuffer should be emptied
// the parameter maxInputPutListSize can be set large for allowing high cuncurrency, if
// the application allows for this case
// - best effort, for creating output in priority order
// - minOutputBufferLength: defines, when the remove will start working (giving enough time
// to add, so that priority should work better
// TODO: update comments
public class SimplePriorityConcurrentListMPDSC {
	//private LinkedList<ComparableEvent>[] inputBuffer;
	private LinkedList<ComparableEvent>[] inputBuffer;
	private int minOutputBufferLength;
	private PriorityQueue<ComparableEvent> outputQueue=new PriorityQueue<ComparableEvent>();
	public Integer incounter=0;
	public Integer outcounter=0;
	private int sumInputPutListSize;
	LinkedList<ComparableEvent> outputWorkingBuffer=new LinkedList<ComparableEvent>();
	
	// producerId 0>=
	public void add(BasicEvent element,int producerId){
			synchronized (outputQueue){
				incounter++;
				outputQueue.add(new ComparableEvent(element));
			}
	}
	
	// returns null, if empty, else the first element
	public BasicEvent remove(){
		if (outputQueue.size()>0){
			outcounter++;
			return outputQueue.poll().getBasicEvent();
		}
		return null;
	}
	
	public SimplePriorityConcurrentListMPDSC (int numberOfProducers, int maxInputPutListSize, int minOutputBufferLength){
		inputBuffer=new LinkedList[numberOfProducers];
		for (int i=0;i<inputBuffer.length;i++){
			inputBuffer[i]=new LinkedList<ComparableEvent>();
		}
		this.sumInputPutListSize=maxInputPutListSize;
		this.minOutputBufferLength=minOutputBufferLength;
	}
	
	
	// this method should be invoked especially, when all producers have finished production
	public void flushAllInputBuffers(){
		
	}
	
	private void swapBuffers(){
		
	}

	
	public static void main(String[] args) {
		PriorityConcurrentListMPDSC queue=new PriorityConcurrentListMPDSC(2,100,10000);
		int outEventCount=0;
		int inEventCount=1000000;
		for (int i=0;i<inEventCount;i++){
			queue.add(new AgentArrivalEvent(1,"","",1), 0);
		}
		
		BasicEvent be=queue.remove();
		outEventCount++;
		//System.out.println(queue.remove());
		
		queue.flushAllInputBuffers();
		
		be=queue.remove();
		while (be!=null){
			outEventCount++;
			be=queue.remove();
		}
		assert(inEventCount==outEventCount): "in: " + inEventCount + "; out: " + outEventCount;
		System.out.println("Test passed (if -ea flag set).");
		
	}
	
}

