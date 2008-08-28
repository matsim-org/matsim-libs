package playground.wrashid.PDES.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

import playground.wrashid.DES.utils.Timer;
// optimized for multiple producer, single consumer
// the producer decides, when the his inputBuffer should be emptied
// the parameter maxInputPutListSize can be set large for allowing high cuncurrency, if
// the application allows for this case
public class ConcurrentListMPDSC<T> {
	private LinkedList<T>[] inputBuffer;
	private LinkedList<LinkedList<T>>[] outputBuffer;
	private int maxInputPutListSize; 
	private LinkedList<T> outputWorkingBuffer=new LinkedList<T>();
	
	// producerId 0>=
	public void add(T element,int producerId){
		synchronized (inputBuffer[producerId]){
			inputBuffer[producerId].add(element);
			if (inputBuffer[producerId].size()>maxInputPutListSize){
				synchronized(outputBuffer[producerId]){
					outputBuffer[producerId].add(inputBuffer[producerId]);
				}
				inputBuffer[producerId]=new LinkedList<T>();
			}
		}	
	}
	
	// returns null, if empty, else the first element
	/*
	public T remove(){
		if (outputWorkingBuffer!=null && !outputWorkingBuffer.isEmpty()){
			return outputWorkingBuffer.poll();
		}
		
		for (int i=0;i<outputBuffer.length;i++){
			if (!outputBuffer[i].isEmpty()){
				synchronized (outputBuffer[i]){
					//swap buffers
					outputWorkingBuffer=outputBuffer[i].poll();
				}
				return outputWorkingBuffer.poll();
			}
		}
		return null;
	}
	*/
	
	// only remove elements from outputWorkingBuffer and ignore new elements
	// this means return null, when outputWorkingBuffer is empty
	/*
	public T nonCuncurrentRemove(){
		if (outputWorkingBuffer!=null && !outputWorkingBuffer.isEmpty()){
			return outputWorkingBuffer.poll();
		}
		return null;
	}
	*/
	
	public LinkedList <T> getCucurrencySafeElements(){
		LinkedList<T> result=outputWorkingBuffer;
		outputWorkingBuffer = new LinkedList<T>();
		return result;
	}
	
	
	public ConcurrentListMPDSC (int numberOfProducers, int maxInputPutListSize){
		inputBuffer=new LinkedList[numberOfProducers];
		outputBuffer=new LinkedList[numberOfProducers];
		for (int i=0;i<inputBuffer.length;i++){
			inputBuffer[i]=new LinkedList<T>();
			outputBuffer[i]=new LinkedList<LinkedList<T>>();
		}
		this.maxInputPutListSize=maxInputPutListSize;
	}
	
	
	// this method should be invoked especially, when all producers have finished production
	public void flushAllInputBuffers(){
		LinkedList<LinkedList<T>> tempOutputBuffer;
		for (int i=0;i<inputBuffer.length;i++){
			synchronized (inputBuffer[i]){
				synchronized (outputBuffer[i]){
					outputBuffer[i].add(inputBuffer[i]);
					inputBuffer[i]=new LinkedList<T>();
				}
			}
			
			synchronized (outputBuffer[i]){
				tempOutputBuffer=outputBuffer[i];
				outputBuffer[i]=new LinkedList<LinkedList<T>>();
			}
			

			for (int j=0;j<tempOutputBuffer.size();j++){
				if (!tempOutputBuffer.peek().isEmpty()){
					outputWorkingBuffer.addAll(tempOutputBuffer.poll());
				} else {
					tempOutputBuffer.removeFirst();
				}
			}
		}
	}

	
	
}

