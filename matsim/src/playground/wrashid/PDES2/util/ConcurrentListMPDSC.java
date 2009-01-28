package playground.wrashid.PDES2.util;

import java.util.LinkedList;
import java.util.PriorityQueue;

// the producer decides, when the his inputBuffer should be emptied
// the parameter maxInputPutListSize can be set large for allowing high cuncurrency, if
// the application allows for this case
// optimized for ZoneMessageQueue
import playground.wrashid.PDES2.Message;


public class ConcurrentListMPDSC {
	private MyPriorityQueue[] inputBuffer;
	private PriorityQueue<MyPriorityQueue>[] middleBuffer;
	private LinkedList<Message>[] outputBuffer;
	//private LinkedList<Message> outputWorkingBuffer = new LinkedList<Message>();
	// private volatile double timeOfEarliestMessage = 0;
	private Object lock = new Object();
	private int minListSize; // if inputBuffer bigger than this, then put it
	private double lastQueueTime = Double.MAX_VALUE;
	private LinkedList<Message>[] outOfOrderMessages;

	// into middleBuffer

	// producerId 0>=
	// precondition: a producer has to add messages in time stamp order
	public void add(Message message, int producerId) {
		synchronized (outputBuffer[producerId]) {
			outputBuffer[producerId].add(message);
		}
		// TODO: uncomment this. (and comment above)
		/*
		if (lastQueueTime < message.messageArrivalTime) {
			synchronized (outOfOrderMessages[producerId]) {
				outOfOrderMessages[producerId].add(message);
			}
		} else {
			synchronized (inputBuffer[producerId]) {
				// the main thread is not allowed to insert anything here!
				assert (SimulationParameters.mainThreadId != Thread
						.currentThread().getId());

				// Find out, why the following assertion does not hold and why
				// we need to use priotyqueues for input buffer
				// assert(inputBuffer[producerId].size()>0?inputBuffer[producerId].getLast().getMessageArrivalTime()<=message.messageArrivalTime:true):
				// inputBuffer[producerId].getLast().getMessageArrivalTime() +
				// "- " + message.messageArrivalTime;
				// just for debugging

				// if inputBuffer not empty, then only the main thread may
				// insert out of order messages (and no other thread)
				// this does not work: the start leg messages already inserted
				// have time stemps bigger than the new messages
				// assert(inputBuffer[producerId].size()>0?(SimulationParameters.mainThreadId
				// !=
				// Thread.currentThread().getId())?inputBuffer[producerId].peek().messageArrivalTime<=message.messageArrivalTime:true:true)
				// : SimulationParameters.mainThreadId +" - "+
				// Thread.currentThread().getId();

				inputBuffer[producerId].getQueue().add(message);
				if (inputBuffer[producerId].getQueue().size() > minListSize) {
					synchronized (middleBuffer[producerId]) {
						middleBuffer[producerId].add(inputBuffer[producerId]);
						//assert (middleBuffer[producerId].getLast() == inputBuffer[producerId]);
						inputBuffer[producerId] = new MyPriorityQueue();
					}
				}
			}
		}
	*/
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
	public LinkedList<Message>[] getCucurrencySafeElements() {
		LinkedList<Message>[] result=new LinkedList[inputBuffer.length];
		for (int i = 0; i < outputBuffer.length; i++) {
			if (outputBuffer[i].size() > 0) {
				synchronized(outputBuffer[i]){
					result[i]=outputBuffer[i];
					outputBuffer[i]=new LinkedList<Message>();
				}
			}
		}
		return result;
	}

	public ConcurrentListMPDSC(int numberOfProducers, int minListSize) {
		outputBuffer = new LinkedList[numberOfProducers];
		outOfOrderMessages = new LinkedList[numberOfProducers];
		inputBuffer = new MyPriorityQueue[numberOfProducers];
		middleBuffer = new PriorityQueue[numberOfProducers];
		for (int i = 0; i < inputBuffer.length; i++) {
			inputBuffer[i] = new MyPriorityQueue();
			outputBuffer[i] = new LinkedList<Message>();
			outOfOrderMessages[i] = new LinkedList<Message>();
			middleBuffer[i] = new PriorityQueue<MyPriorityQueue>();
		}
		this.minListSize = minListSize;
	}

	// flush all messages, with time stamp smaller than queueTime
	public void flushAllInputBuffers(double queueTime) {
		lastQueueTime = queueTime;
		PriorityQueue<Message> swap = null;
		LinkedList<Message> swap1 = null;
		boolean breakLoop = false;
		Message tempMessage = null;
		int numberOfListsInMiddleBuffer = 0;
		for (int i = 0; i < inputBuffer.length; i++) {
			// System.out.print(inputBuffer[i].size()>0 &&
			// inputBuffer[i].peek().getMessageArrivalTime()<queueTime?inputBuffer[i].peek().getMessageArrivalTime()+
			// " - " +queueTime + "\n":"");
			
			// flush out of order messages first
			
			if (outOfOrderMessages[i].size()>0){
				synchronized (outOfOrderMessages[i]) {
					swap1=outOfOrderMessages[i];
					outOfOrderMessages[i]=new LinkedList<Message>();
				}
				while (swap1.size()>0){
					outputBuffer[i].add(swap1.poll());
				}
			}
			
			breakLoop = false;

			// empty middleBuffer, as far as needed
			synchronized (middleBuffer[i]) {
				numberOfListsInMiddleBuffer = middleBuffer[i].size();
			}
			for (int j = 0; j < numberOfListsInMiddleBuffer; j++) {
				synchronized (middleBuffer[i]) {
					if (middleBuffer[i].size()>0 && middleBuffer[i].peek().getQueue().peek().messageArrivalTime  > queueTime){
						breakLoop = true;
						break;
					}
					swap = middleBuffer[i].poll().getQueue();			
				}
				tempMessage = swap.poll();
				while (tempMessage != null) {
					outputBuffer[i].add(tempMessage);
					tempMessage = swap.poll();
				}
			}

			// if middleBuffer emptied => try to empty inputBuffers
			if (!breakLoop || (inputBuffer[i].getQueue().peek()!=null && inputBuffer[i].getQueue().peek().messageArrivalTime<=queueTime)) {
				flushEverything(i);
			}

			// there is a message in the inputBuffer, which shouldn't be there,
			// as we for some wrong reason did a breakLoop
			// assert(inputBuffer[i].size()>0?inputBuffer[i].peek().getMessageArrivalTime()>=queueTime
			// && breakLoop:true):
			// inputBuffer[i].peek().getMessageArrivalTime()+ " - " +queueTime +
			// " - " + i + " - " +MessageExecutor.getThreadId();
		}
	}

	// at the time of invocation of this method: everything in input buffer and
	// middle buffer should
	// be emptied
	public void flushEverything() {
		
		for (int i = 0; i < inputBuffer.length; i++) {
			
			flushEverything(i);
		}
	}

	// empty input and middle buffer for the given bufferId
	public void flushEverything(int bufferId) {
		PriorityQueue<Message> tempInputBuffer = null;
		PriorityQueue<Message> swap = null;
		LinkedList<Message> swap1 = null;
		int numberOfListsInMiddleBuffer = 0;
		int i = bufferId;
		
		// flush out of order messages 
		if (outOfOrderMessages[i].size()>0){
			synchronized (outOfOrderMessages[i]) {
				swap1=outOfOrderMessages[i];
				outOfOrderMessages[i]=new LinkedList<Message>();
			}
			while (swap1.size()>0){
				outputBuffer[i].add(swap1.poll());
			}
		}
		
		
		synchronized (inputBuffer[i]) {
			// empty inputBuffer
			tempInputBuffer = inputBuffer[i].getQueue();
			inputBuffer[i] = new MyPriorityQueue();
			synchronized (middleBuffer[i]) {
				numberOfListsInMiddleBuffer = middleBuffer[i].size();
			}
		}

		// empty middleBuffer
		for (int j = 0; j < numberOfListsInMiddleBuffer; j++) {
			synchronized (middleBuffer[i]) {
				swap = middleBuffer[i].poll().getQueue();
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
		 * ConcurrentListMPDSC cList=new ConcurrentListMPDSC(5,10); for (int
		 * i=0;i<1000;i++){ cList.add(i, 0); }
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
		 */

	}

	// assume, single threading at this point
	public boolean assert_EverythingEmpty() {
		boolean result = true;
		for (int i = 0; i < inputBuffer.length; i++) {
			if (inputBuffer[i].getQueue().size() != 0 || middleBuffer[i].size() != 0
					|| outputBuffer[i].size() != 0 || outOfOrderMessages[i].size()!= 0 ) {
				 System.out.println(inputBuffer[i].getQueue().size());
				 System.out.println(middleBuffer[i].size());
				 System.out.println(outputBuffer[i].size());
				 System.out.println(outOfOrderMessages[i].size());
				 
				result = false;
				break;
			}
		}

		return result;
	}

}
