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
	private volatile double timeOfEarliestMessage = 0;
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
				}
			}
		}
	}

	public double getTimeOfLatestMessageAfterLastFlush() {
		/*
		 * double earliestTimeStamp=Double.MAX_VALUE; for (int i=0;i<inputBuffer.length;i++){
		 * if (inputBuffer[i].size()>0 &&
		 * inputBuffer[i].peek().getMessageArrivalTime()<earliestTimeStamp){
		 * earliestTimeStamp=inputBuffer[i].peek().getMessageArrivalTime(); } }
		 * return earliestTimeStamp;
		 */
		return timeOfEarliestMessage;
	}

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

	// this method should be invoked especially, when all producers have
	// finished production
	public void flushAllInputBuffers(double queueTime) {
		LinkedList<Message> swap = null;
		boolean breakLoop = false;
		Message tempMessage = null;
		for (int i = 0; i < inputBuffer.length; i++) {
			breakLoop = false;

			synchronized (middleBuffer[i]) {
				swap = middleBuffer[i].poll();
			}
			while (swap != null) {
				tempMessage = swap.poll();
				while (tempMessage != null) {
					outputBuffer[i].add(tempMessage);
					
					if (tempMessage.messageArrivalTime>queueTime){
						breakLoop=true;
					}
					
					tempMessage = swap.poll();
				}

				if (breakLoop) {
					break;
				} else {
					synchronized (middleBuffer[i]) {
						swap = middleBuffer[i].poll();
					}
				}
			}
			
			// if middleBuffer empty
			if (swap==null) {
				// try to get the first element of middleBuffer
				synchronized (middleBuffer[i]) {
					swap = middleBuffer[i].poll();
				}
				
				if (swap!=null){
					// do invoke method: really flush input buffer...
					
				} else {
					// TODO: continue here...
				}
				
			}

		}
	}
	
	
	

	public static void main(String[] args) {

		// part 1
		/*
		 * ConcurrentListMPDSC<Integer> cList=new ConcurrentListMPDSC<Integer>(5);
		 * for (int i=0;i<1000;i++){ cList.add(i, 0); }
		 * 
		 * 
		 * System.out.println(cList.inputBuffer[0].size());
		 * //System.out.println(cList.outputBuffer[0].size());
		 * 
		 * //for (int i=0;i<1000;i++){ cList.flushAllInputBuffers();
		 * System.out.println(cList.inputBuffer[0].size());
		 * //System.out.println(cList.outputBuffer[0].size());
		 * 
		 * System.out.println(cList.getCucurrencySafeElements().size()); //}
		 * 
		 */

	}

}
