package playground.wrashid.PDES1;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.PriorityBlockingQueue;

import org.matsim.gbl.Gbl;
import org.matsim.population.Person;

public class MessageQueue {
	//TreeMap<String, Message> queue = new TreeMap<String, Message>();
	//private PriorityBlockingQueue<Message> queue1 = new PriorityBlockingQueue<Message>(10000);
	// PriorityQueue is better for performance than PriorityBlockingQueue, but requires all methods of this class
	// to be set synchronized
	private PriorityQueue<Message> queue1 = new PriorityQueue<Message>();
	//private LinkedList<Message> addBuffer=new LinkedList<Message>();
	//private LinkedList<Message> deleteBuffer=new LinkedList<Message>();
	private volatile static int counter=0;
	public double arrivalTimeOfLastRemovedMessage=0;
	private Object bufferLock=new Object();
	private LinkedList<Message>[] addMessageBuffer=new LinkedList[SimulationParameters.numberOfMessageExecutorThreads];
	private LinkedList<Message>[] deleteMessageBuffer=new LinkedList[SimulationParameters.numberOfMessageExecutorThreads];
	
	synchronized public void putMessage(Message m) {
		assert(!queue1.contains(m)):"inconsistency";
		assert(m.messageArrivalTime>=0):"simulation time cannot be negative";
		//assert(m.firstLock!=null);
		//assert(m.messageArrivalTime>=arrivalTimeOfLastRemovedMessage):"big inconsistency!"; // this condition does not hold anymore!!!
		queue1.add(m);
		//assert(queue1.contains(m)):"inconsistency";
		// This assertion was removed, because of concurrent access this might be violated
	
		if (!(m instanceof NullMessage)){
			//System.out.println(m + " - " + m.messageArrivalTime + " - " + arrivalTimeOfLastRemovedMessage);
		}
		
		
		if (m.messageArrivalTime>2000){	
			//System.out.println("m.messageArrivalTime:"+m.messageArrivalTime);
		}
	}
	
	public MessageQueue(){
		for (int i=1;i<SimulationParameters.numberOfMessageExecutorThreads+1;i++){
			addMessageBuffer[i-1]=new LinkedList<Message>();
			deleteMessageBuffer[i-1]=new LinkedList<Message>();
		}
	}
	
	synchronized public double getArrivalTimeOfNextMessage(){
		return queue1.peek().messageArrivalTime;
	}
	

	public static int getCounter() {
		return counter;
	}

	synchronized public void removeMessage(Message m) {
		//assert(queue1.contains(m)):"inconsistency";
		//if (!queue1.contains(m)){
			// if the message (e.g. DeadlockPreventionMessage) has already been fetched, it should not be allowed
			// to execute!!!
		//	m.killMessage(); 
		//}


		
		queue1.remove(m);
	}

	synchronized public Message getNextMessage() {
		counter++;

		
		
		
		//System.out.println(arrivalTimeOfLastRemovedMessage);
		
		emptyBuffers();
		
		Message m = queue1.poll();
		//Message m = queue1.removeLast();
		if (m!=null){
			
			if (counter % 10000==0){
				System.out.println("event:" + counter);
				System.out.println(arrivalTimeOfLastRemovedMessage);
			}
			
			
			//System.out.println("event:" + counter);
			if (arrivalTimeOfLastRemovedMessage>1000){	
				//System.out.println(queue1.size());
				//System.out.println("the time has come:" + 1000);
			}
			
			//if (queue1.size() % 1000 ==0){
			if (arrivalTimeOfLastRemovedMessage>27000){	
				//System.out.println(queue1.size());
				//System.out.println("the time has come:" + 27000);
			}
			
			assert(arrivalTimeOfLastRemovedMessage>=0):"simulation time cannot be negative";
			
			//System.out.println("arrivalTimeOfLastRemovedMessage:"+arrivalTimeOfLastRemovedMessage);
		
			if (!(m instanceof NullMessage)){
				System.out.println(m + " - " + m.messageArrivalTime + " - " + arrivalTimeOfLastRemovedMessage);
			}
				arrivalTimeOfLastRemovedMessage=m.messageArrivalTime;
		}
		
		
		
		
		
		return m;
	}

	synchronized public boolean isEmpty() {
		boolean allBuffersEmpty=true;
		for (int i=1;i<SimulationParameters.numberOfMessageExecutorThreads+1;i++){
			if (!addMessageBuffer[i-1].isEmpty()){
				allBuffersEmpty=false; break;
			}
		}
		
		if (queue1.isEmpty() && allBuffersEmpty){
			return true;
		} else {
			return false;
		}
	}

	/*
	public void addBuffer(Message m){
		synchronized(addBuffer){
			addBuffer.add(m);
		}
	}
	
	public void deleteBuffer(Message m){
		synchronized(deleteBuffer){
			deleteBuffer.add(m);
		}
	}
	*/
	
	public void addBuffer(Message m,int threadId){
		synchronized(addMessageBuffer[threadId-1]){
			addMessageBuffer[threadId-1].add(m);
		}
	}
	
	public void deleteBuffer(Message m,int threadId){
		synchronized(deleteMessageBuffer[threadId-1]){
			deleteMessageBuffer[threadId-1].add(m);
		}
	}
	
	
	
	
	
	public void emptyBuffers(){
		/*
		if (!addBuffer.isEmpty()){
			synchronized(addBuffer){
				while (!addBuffer.isEmpty()){
					queue1.add(addBuffer.poll());
				}
			}
		}
		if (!deleteBuffer.isEmpty()){
			synchronized(deleteBuffer){
				while (!deleteBuffer.isEmpty()){
					queue1.remove(deleteBuffer.poll());
				}
			}
		}
		*/
		for (int i=1;i<SimulationParameters.numberOfMessageExecutorThreads+1;i++){             
             if (!addMessageBuffer[i-1].isEmpty()){
     			synchronized(addMessageBuffer[i-1]){
     				while (!addMessageBuffer[i-1].isEmpty()){
     					queue1.add(addMessageBuffer[i-1].poll());
     				}
     			}
     		}

			if (!deleteMessageBuffer[i-1].isEmpty()){
				synchronized(deleteMessageBuffer[i-1]){
					while (!deleteMessageBuffer[i-1].isEmpty()){
						queue1.remove(deleteMessageBuffer[i-1].poll());
					}
				}
			}
		}
	}
}
