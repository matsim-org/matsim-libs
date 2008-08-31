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
	private LinkedList<Message>[] outputBuffer;
	private LinkedList<Message> outputWorkingBuffer = new LinkedList<Message>();

	private Object lock = new Object();

	// producerId 0>=
	public void add(Message message, int producerId) {
		synchronized (inputBuffer[producerId]) {
			inputBuffer[producerId].add(message);
		}
	}

	public double getTimeOfLatestMessageAfterLastFlush() {
		double earliestTimeStamp=Double.MAX_VALUE;
		for (int i=0;i<inputBuffer.length;i++){
			if (inputBuffer[i].size()>0 && inputBuffer[i].peek().getMessageArrivalTime()<earliestTimeStamp){
				earliestTimeStamp=inputBuffer[i].peek().getMessageArrivalTime();
			}
		}
		return earliestTimeStamp;
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

	public ConcurrentListMPDSC(int numberOfProducers) {
		outputBuffer = new LinkedList[numberOfProducers];
		inputBuffer = new LinkedList[numberOfProducers];
		for (int i = 0; i < inputBuffer.length; i++) {
			inputBuffer[i] = new LinkedList<Message>();
			outputBuffer[i] = new LinkedList<Message>();
		}
	}

	// this method should be invoked especially, when all producers have
	// finished production
	public void flushAllInputBuffers() {
		LinkedList<Message> swap = null;
		for (int i = 0; i < inputBuffer.length; i++) {
			if (inputBuffer[i].size() > 0) {
				synchronized (inputBuffer[i]) {
					// only exchange tables, if something has changed
					swap = outputBuffer[i];
					outputBuffer[i] = inputBuffer[i];
					inputBuffer[i] = swap;
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
