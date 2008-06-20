package playground.wrashid.PDES;

import java.util.TreeMap;



public class MessageQueue {
	TreeMap<String, Message> queue=new TreeMap<String, Message>();
	long counter=0;
	
	
	// A Map (TreeMap, HashMap, etc.) can only contain one value for the same key
	// This means, it is not possible to insert two messages into the queue, keyed with
	// the same time
	// Inorder to prevent this, a counter is attached to the time, for "assuring" uniqueness
	// (if more than 2^64 events are scheduled for the same time, than the uniqueness will not hold anymore...)
	// (but this is quite an impossible case: very very huge simulation... - which would not run on a normal computer anyway)
	void putMessage(Message m){
		long rlong=counter++;
		// padding with zeros is needed, inorder to be able to use firstKey property of the queue
		String zeroPadded=feedWithZeros(m.getMessageArrivalTime());
		queue.put(zeroPadded.concat((new Long(rlong)).toString()),m);
	}
	
	Message getNextMessage(){
		Message m=(Message) queue.remove(queue.firstKey());
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
