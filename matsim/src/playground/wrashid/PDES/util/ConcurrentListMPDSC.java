package playground.wrashid.PDES.util;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

import playground.wrashid.DES.utils.Timer;
// optimized for multiple producer, single consumer
// the producer decides, if his inputBuffer should be emptied or not
// TODO: implemented yet...
public class ConcurrentListMPDSC<T> {
	private LinkedList<T>[] inputBufferZero;
	private LinkedList<T>[] inputBuffer;
	private LinkedList<T> outputBuffer=new LinkedList<T>(); 
	
	// producerId 0>=
	public void add(T element,int producerId){
		synchronized (inputBuffer[producerId]){
			inputBuffer[producerId].add(element);
			// write in inputBufferZero and only if contains a million elements, then write...
		}
	}
	
	// returns null, if empty, else the first element
	public T remove(){
		if (!outputBuffer.isEmpty()){
			return outputBuffer.poll();
		}
		for (int i=0;i<inputBuffer.length;i++){
			if (!inputBuffer[i].isEmpty()){
				synchronized (inputBuffer[i]){
					//swap buffers
					LinkedList<T> tempList=null; 
					tempList=inputBuffer[i];
					inputBuffer[i]=outputBuffer;
					outputBuffer=tempList;
				}
				return outputBuffer.poll();
			}
		}
		return null;
	}
	
	public ConcurrentListMPDSC (int numberOfProducers){
		inputBuffer=new LinkedList[numberOfProducers];
		for (int i=0;i<inputBuffer.length;i++){
			inputBuffer[i]=new LinkedList<T>();
			inputBufferZero[i]=new LinkedList<T>();
		}
	}
	
	// make a method for emptying all input buffers (sychronized, to be sued at the end of the simulation by the consoumer)
	

	
	
}

