package playground.wrashid.DES;

import java.util.TreeMap;



public class MessageQueue {
	TreeMap<String, Message> queue=new TreeMap<String, Message>();
	long counter=0;
	
	
	// A Map (TreeMap, HashMap, etc.) can only contain one value for the same key
	// This means, it is not possible to insert two messages into the queue, keyed with
	// the same time
	// Inorder to prevent this, a counter is attached to the time, for "assuring" uniqueness
	// (if more than 2^64 (approx. 10^19) events are scheduled for the same time, than the uniqueness will not hold anymore...)
	// (although this case seems quite impossible, never the less it is mentioned as a precondition for using this scheduler)
	void putMessage(Message m){
		long rlong=counter++;
		// padding with zeros is needed, inorder to be able to use firstKey property of the queue
		//String zeroPadded=feedWithZeros(m.getMessageArrivalTime());
		String zeroPadded=Double.toString(m.getMessageArrivalTime());
		m.queueKey=zeroPadded.concat("."+(new Long(rlong)).toString());
		queue.put(m.queueKey,m);
		

		
		if (SimulationParameters.debugMode && (counter % 1000000 == 0)){
			System.out.println("MessageQueue.counter:"+ counter);
		}
	}
	
	void removeMessage(Message m){
		queue.remove(m.queueKey);
	}
	
	Message getNextMessage(){
		Message m=(Message) queue.remove(queue.firstKey());
		//System.out.println(m.queueKey);
		return m;
	}
	
	public boolean hasElement(){
		return !queue.isEmpty();
	}
	
	public static String feedWithZeros(double x){
		String result="";
		long j=Long.MAX_VALUE;
		
		while (j>0){
			if (x/j<=0){
				result=result.concat("0");
			}
			j/=10;
		}
		result=result.concat(new Double(x).toString());
		return result;
	}
	
}
