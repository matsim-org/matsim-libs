package playground.wrashid.PDES.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

import playground.wrashid.DES.utils.Timer;
import playground.wrashid.PDES.util.ConcurrentListSPSC.ARunnable;
import playground.wrashid.PDES.util.ConcurrentListSPSC.BRunnable;
// optimized for multiple producer, single consumer
// the producer decides, when the his inputBuffer should be emptied
// the parameter maxInputPutListSize can be set large for allowing high cuncurrency, if
// the application allows for this case
// optimized for ZoneMessageQueue
public class ConcurrentListMPDSC<T> {
	private LinkedList<T>[] inputBuffer;
	private LinkedList<T> outputWorkingBuffer=new LinkedList<T>();
	
	// producerId 0>=
	public void add(T element,int producerId){
		synchronized (inputBuffer[producerId]){
			inputBuffer[producerId].add(element);
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
	
	
	public ConcurrentListMPDSC (int numberOfProducers){
		inputBuffer=new LinkedList[numberOfProducers];
		for (int i=0;i<inputBuffer.length;i++){
			inputBuffer[i]=new LinkedList<T>();
		}
	}
	
	
	// this method should be invoked especially, when all producers have finished production
	public void flushAllInputBuffers(){
		LinkedList<T> tempOutputBuffer=new LinkedList<T>();
		LinkedList<T> swap=null;
		for (int i=0;i<inputBuffer.length;i++){
			if (!inputBuffer[i].isEmpty()){
				synchronized (inputBuffer[i]){
					// only exchange tables, if something has changed
					swap=tempOutputBuffer;
					tempOutputBuffer=inputBuffer[i];
					inputBuffer[i]=swap;
				}
			}
			
			if (!tempOutputBuffer.isEmpty()){
				outputWorkingBuffer.addAll(tempOutputBuffer);
				tempOutputBuffer.clear();
			}
			
		}
	}

public static void main(String[] args){
		
		// part 1
		
		ConcurrentListMPDSC<Integer> cList=new ConcurrentListMPDSC<Integer>(5);
		for (int i=0;i<1000;i++){
			cList.add(i, 0);
		}
		
		
		System.out.println(cList.inputBuffer[0].size());
		//System.out.println(cList.outputBuffer[0].size());
		
		//for (int i=0;i<1000;i++){
		cList.flushAllInputBuffers();
		System.out.println(cList.inputBuffer[0].size());
		//System.out.println(cList.outputBuffer[0].size());
		
		
			System.out.println(cList.getCucurrencySafeElements().size());
		//}
		
		
		
	}
	
}

