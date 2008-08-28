package playground.wrashid.PDES.util;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.events.BasicEvent;

import playground.wrashid.DES.utils.Timer;
// optimized for multiple producer, single consumer
// the producer decides, when the his inputBuffer should be emptied
// the parameter maxInputPutListSize can be set large for allowing high cuncurrency, if
// the application allows for this case
// - best effort, for creating output in priority order
// - minOutputBufferLength: defines, when the remove will start working (giving enough time
// to add, so that priority should work better
public class PriorityConcurrentListMPDSC {
	private LinkedList<ComparableEvent>[] inputBuffer;
	private LinkedList<LinkedList<ComparableEvent>>[] outputBuffer;
	private int maxInputPutListSize; 
	private LinkedList<ComparableEvent> outputWorkingBuffer=null;
	private int minOutputBufferLength;
	private PriorityQueue<ComparableEvent> outputQueue=new PriorityQueue<ComparableEvent>();
	
	// producerId 0>=
	public void add(BasicEvent element,int producerId){
			inputBuffer[producerId].add(new ComparableEvent(element));
			if (inputBuffer[producerId].size()>maxInputPutListSize){
				synchronized(outputBuffer[producerId]){
					outputBuffer[producerId].add(inputBuffer[producerId]);
				}
				inputBuffer[producerId]=new LinkedList<ComparableEvent>();
			}
	}
	
	// returns null, if empty, else the first element
	public BasicEvent remove(){
		if (outputQueue.size()>=minOutputBufferLength){
			return outputQueue.poll().getBasicEvent();
		}
		
		for (int i=0;i<outputBuffer.length;i++){
			if (!outputBuffer[i].isEmpty()){
				synchronized (outputBuffer[i]){
					//swap buffers
					outputWorkingBuffer=outputBuffer[i].poll();
				}
				outputQueue.addAll(outputWorkingBuffer);
				if (outputQueue.size()>=minOutputBufferLength){
					return outputQueue.poll().getBasicEvent();
				}
			}
		}
		
		return null;
	}
	
	public PriorityConcurrentListMPDSC (int numberOfProducers, int maxInputPutListSize, int minOutputBufferLength){
		inputBuffer=new LinkedList[numberOfProducers];
		outputBuffer=new LinkedList[numberOfProducers];
		for (int i=0;i<inputBuffer.length;i++){
			inputBuffer[i]=new LinkedList<ComparableEvent>();
			outputBuffer[i]=new LinkedList<LinkedList<ComparableEvent>>();
		}
		this.maxInputPutListSize=maxInputPutListSize;
		this.minOutputBufferLength=minOutputBufferLength;
	}
	
	
	// this method should be invoked especially, when all producers have finished production
	public void flushAllInputBuffers(){
		minOutputBufferLength=1;
		
		// push all input buffers to ouputBuffer
		for (int i=0;i<inputBuffer.length;i++){
			synchronized (inputBuffer[i]){
				synchronized (outputBuffer[i]){
					outputBuffer[i].add(inputBuffer[i]);
					inputBuffer[i]=new LinkedList<ComparableEvent>();
				}
			}
		}
		
		// push all output buffers to outputQueue
		for (int i=0;i<outputBuffer.length;i++){
			if (!outputBuffer[i].isEmpty()){
				synchronized (outputBuffer[i]){
					//swap buffers
					outputWorkingBuffer=outputBuffer[i].poll();
				}
				outputQueue.addAll(outputWorkingBuffer);
			}
		}
	}

	
	
}

