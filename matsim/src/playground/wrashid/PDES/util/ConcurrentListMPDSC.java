package playground.wrashid.PDES.util;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

import playground.wrashid.DES.utils.Timer;
import playground.wrashid.PDES.util.ConcurrentListSPSC.ARunnable;
import playground.wrashid.PDES.util.ConcurrentListSPSC.BRunnable; // optimized for multiple producer, single consumer
// the producer decides, when the his inputBuffer should be emptied
// the parameter maxInputPutListSize can be set large for allowing high cuncurrency, if
// the application allows for this case
// optimized for ZoneMessageQueue
import playground.wrashid.PDES2.Message;

public class ConcurrentListMPDSC {
	private LinkedList<Message>[] inputBuffer;
	private LinkedList<LinkedList<Message>>[] middleBuffer;
	private LinkedList<Message>[] outputBuffer;
	private LinkedList<Message> outputWorkingBuffer = new LinkedList<Message>();
	// private volatile double timeOfEarliestMessage = 0;
	private Object lock = new Object();
	private int minListSize; // if inputBuffer bigger than this, then put it

	// into middleBuffer

	// producerId 0>=
	public void add(Message message, int producerId) {
		synchronized (inputBuffer[producerId]) {
			inputBuffer[producerId].add(message);
			if (inputBuffer[producerId].size() > minListSize) {
				synchronized (middleBuffer[producerId]) {
					middleBuffer[producerId].add(inputBuffer[producerId]);
					inputBuffer[producerId] = new LinkedList<Message>();
				}
			}
		}
	}

	// public double getTimeOfLatestMessageAfterLastFlush() {
	/*
	 * double earliestTimeStamp=Double.MAX_VALUE; for (int i=0;i<inputBuffer.length;i++){
	 * if (inputBuffer[i].size()>0 &&
	 * inputBuffer[i].peek().getMessageArrivalTime()<earliestTimeStamp){
	 * earliestTimeStamp=inputBuffer[i].peek().getMessageArrivalTime(); } }
	 * return earliestTimeStamp;
	 */
	// return timeOfEarliestMessage;
	// }
	// returns null, if empty, else the first element
	/*
	 * public T remove(){ if (outputWorkingBuffer!=null &&
	 * !outputWorkingBuffer.isEmpty()){ return outputWorkingBuffer.poll(); }
	 * 
	 * for (int i=0;i<outputBuffer.length;i++){ if
	 * (!outputBuffer[i].isEmpty()){ synchronized (outputBuffer[i]){ //swap
	 * buffers outputWorkingBuffer=outputBuffer[i].poll(); } return
	 * outputWorkingBuffer.poll(); } } return null; }
	 */

	// only remove elements from outputWorkingBuffer and ignore new elements
	// this means return null, when outputWorkingBuffer is empty
	/*
	 * public T nonCuncurrentRemove(){ if (outputWorkingBuffer!=null &&
	 * !outputWorkingBuffer.isEmpty()){ return outputWorkingBuffer.poll(); }
	 * return null; }
	 */

	// gives back a list, until null
	// side effect: the consumer must empty the result list before next
	// invocation!!!
	public LinkedList<Message> getCucurrencySafeElements() {
		for (int i = 0; i < outputBuffer.length; i++) {
			if (outputBuffer[i].size() > 0) {
				return outputBuffer[i];
			}
		}
		return null;
	}

	public ConcurrentListMPDSC(int numberOfProducers, int minListSize) {
		outputBuffer = new LinkedList[numberOfProducers];
		inputBuffer = new LinkedList[numberOfProducers];
		middleBuffer = new LinkedList[numberOfProducers];
		for (int i = 0; i < inputBuffer.length; i++) {
			inputBuffer[i] = new LinkedList<Message>();
			outputBuffer[i] = new LinkedList<Message>();
			middleBuffer[i] = new LinkedList<LinkedList<Message>>();
		}
		this.minListSize = minListSize;
	}

	// flush all messages, with time stamp smaller than queueTime
	public void flushAllInputBuffers(double queueTime) {
		LinkedList<Message> swap = null;
		boolean breakLoop = false;
		Message tempMessage = null;
		int numberOfListsInMiddleBuffer = 0;
		for (int i = 0; i < inputBuffer.length; i++) {
			breakLoop = false;

			// empty middleBuffer, as far as needed
			synchronized (middleBuffer[i]) {
				numberOfListsInMiddleBuffer = middleBuffer[i].size();
			}
			for (int j = 0; j < numberOfListsInMiddleBuffer; j++) {
				synchronized (middleBuffer[i]) {
					swap = middleBuffer[i].poll();
				}
				tempMessage = swap.poll();
				while (tempMessage != null) {
					outputBuffer[i].add(tempMessage);
					if (tempMessage.messageArrivalTime > queueTime) {
						breakLoop = true;
					}
					tempMessage = swap.poll();
				}
				if (breakLoop) {
					break;
				}
			}

			// if middleBuffer emptied => try to empty inputBuffers
			if (!breakLoop) {
				flushEverything(i);
			}
		}
	}

	// at the time of invocation of this method: everything in input buffer and
	// middle buffer should
	// be emptied 
	public void flushEverything(){
		for (int i = 0; i < inputBuffer.length; i++) {
			flushEverything(i);
		}
	}
			
	
	
	// empty input and middle buffer for the given bufferId
	public void flushEverything(int bufferId) {
		LinkedList<Message> tempInputBuffer = null;
		LinkedList<Message> swap = null;
		int numberOfListsInMiddleBuffer = 0;
		int i = bufferId;
		synchronized (inputBuffer[i]) {
			// empty inputBuffer
			tempInputBuffer = inputBuffer[i];
			inputBuffer[i] = new LinkedList<Message>();
			synchronized (middleBuffer[i]) {
				numberOfListsInMiddleBuffer = middleBuffer[i].size();
			}
		}

		// empty middleBuffer
		for (int j = 0; j < numberOfListsInMiddleBuffer; j++) {
			synchronized (middleBuffer[i]) {
				swap = middleBuffer[i].poll();
			}
			while (swap.size() > 0) {
				outputBuffer[i].add(swap.poll());
			}
		}

		while (tempInputBuffer.size() > 0) {
			outputBuffer[i].add(tempInputBuffer.poll());
		}
	}

	public static void main(String[] args) {

		// part 1
		/*
		  ConcurrentListMPDSC cList=new ConcurrentListMPDSC(5,10);
		  for (int i=0;i<1000;i++){ cList.add(i, 0); }
		  
		  
		  System.out.println(cList.inputBuffer[0].size());
		  //System.out.println(cList.outputBuffer[0].size());
		  
		  //for (int i=0;i<1000;i++){ cList.flushAllInputBuffers();
		  System.out.println(cList.inputBuffer[0].size());
		  //System.out.println(cList.outputBuffer[0].size());
		  
		  System.out.println(cList.getCucurrencySafeElements().size()); //}
		  */
		 

	}

}
