package playground.wrashid.PDES2;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.matsim.network.Link;

import org.matsim.gbl.Gbl;
import org.matsim.population.Person;

public class ZoneMessageQueue {
	// The MessageQueue can only progress if messages from all incoming links (from neighbouring zones) have arrived,
	// and then only until the first ZoneBorderMessage or EnterRequestMessage
	
	private int zoneId=0;
	
	private PriorityQueue<Message> queue1 = new PriorityQueue<Message>();

	private volatile static int counter=0;
	private volatile static int incounter=0;
	public double arrivalTimeOfLastRemovedMessage=0;
	private Object bufferLock=new Object();
	private LinkedList<Message>[] addMessageBuffer=new LinkedList[SimulationParameters.numberOfMessageExecutorThreads];
	private LinkedList<Message>[] deleteMessageBuffer=new LinkedList[SimulationParameters.numberOfMessageExecutorThreads];
	public Lock lock=new ReentrantLock();
	
	public int numberOfIncomingLinks=0;
	public LinkedList<Link> tempIncomingLinks=new LinkedList<Link>();
	
	synchronized public void putMessage(Message m) {
		assert(!queue1.contains(m)):"inconsistency";
		assert(m.messageArrivalTime>=0):"simulation time cannot be negative";
		//assert(m.firstLock!=null);
		//assert(m.messageArrivalTime>=arrivalTimeOfLastRemovedMessage):"big inconsistency!"; // this condition does not hold anymore!!!
		queue1.add(m);
		//assert(queue1.contains(m)):"inconsistency";
		// This assertion was removed, because of concurrent access this might be violated
	
		if (!(m instanceof ZoneBorderMessage)){
			//System.out.println(m + " - " + m.messageArrivalTime + " - " + arrivalTimeOfLastRemovedMessage);
		}
		
		
		if (incounter % 10000 ==0){	
			System.out.println("incounter:"+incounter);
		}
		incounter++;
	}
	
	public ZoneMessageQueue(int zoneId){
		for (int i=0;i<SimulationParameters.numberOfMessageExecutorThreads;i++){
			addMessageBuffer[i]=new LinkedList<Message>();
			deleteMessageBuffer[i]=new LinkedList<Message>();
		}
		this.zoneId=zoneId;
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
		

		
		
		
		//System.out.println(arrivalTimeOfLastRemovedMessage);
		
		emptyBuffers();
		
		Message m = queue1.poll();
		//Message m = queue1.removeLast();
		if (m!=null){
			counter++;
			if (counter % 10000==0){
				System.out.println("event:" + counter);
				System.out.println(arrivalTimeOfLastRemovedMessage);
				System.out.println("MessageExecutor.getThreadId():"+MessageExecutor.getThreadId());
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
			
			// this assertion does not need to be true: assume, a road with long incoming roads receiving all messages.
			// already at the beginning of the simulation this can be wrong
			// assert(m.messageArrivalTime>=arrivalTimeOfLastRemovedMessage):"something is wrong here...";
			
			//System.out.println("arrivalTimeOfLastRemovedMessage:"+arrivalTimeOfLastRemovedMessage);
		
			if (!(m instanceof ZoneBorderMessage)){
				System.out.println(m + " - " + m.messageArrivalTime + " - " + arrivalTimeOfLastRemovedMessage);
			}
				arrivalTimeOfLastRemovedMessage=m.messageArrivalTime;
		}
		
		
		
		
		
		return m;
	}

	synchronized public boolean isEmpty() {
		boolean allBuffersEmpty=true;
		for (int i=0;i<SimulationParameters.numberOfMessageExecutorThreads;i++){
			if (!addMessageBuffer[i].isEmpty()){
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
		synchronized(addMessageBuffer[threadId]){
			addMessageBuffer[threadId].add(m);
		}
	}
	
	public void deleteBuffer(Message m,int threadId){
		synchronized(deleteMessageBuffer[threadId]){
			deleteMessageBuffer[threadId].add(m);
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
		for (int i=0;i<SimulationParameters.numberOfMessageExecutorThreads;i++){             
             if (!addMessageBuffer[i].isEmpty()){
     			synchronized(addMessageBuffer[i]){
     				while (!addMessageBuffer[i].isEmpty()){
     					queue1.add(addMessageBuffer[i].poll());
     				}
     			}
     		}

			if (!deleteMessageBuffer[i].isEmpty()){
				synchronized(deleteMessageBuffer[i]){
					while (!deleteMessageBuffer[i].isEmpty()){
						queue1.remove(deleteMessageBuffer[i].poll());
					}
				}
			}
		}
	}
}
