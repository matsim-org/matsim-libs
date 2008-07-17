package playground.wrashid.DES;

import java.util.TreeMap;

import org.matsim.plans.Person;



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
		String zeroPadded=padWithZeros(m.getMessageArrivalTime());
		//String zeroPadded=Double.toString(m.getMessageArrivalTime());
		m.queueKey=zeroPadded.concat("."+(new Long(rlong)).toString());
		queue.put(m.queueKey,m);
		

		
		if (SimulationParameters.debugMode && (counter % 1000000 == 0)){
			System.out.println("MessageQueue.counter:"+ counter);
		}
		
		/*
		if (counter> 500000){
			 
			for (String key : queue.keySet()) {
				System.out.println("key=" + key);
			}
			
			System.out.println();
		}
		*/
		
	}
	
	void removeMessage(Message m){
		queue.remove(m.queueKey);
	}
	
	Message getNextMessage(){
		Message m= queue.remove(queue.firstKey());
		//System.out.println(m.queueKey);
		return m;
	}
	
	public boolean hasElement(){
		return !queue.isEmpty();
	}
	
	// this is nesseary for comparison of strings,
	// because with string comparison 91 is bigger than 231 (because only 2 and 9 are compared)
	public static String feedWithZeros(double xd){
		String result="";
		long x=Math.round(Math.floor(xd));
		long j=Long.MAX_VALUE;
		
		while (j>0){
			if (x/j<=0){
				result=result.concat("0");
			}
			j/=10;
		}
		result=result.concat(new Double(xd).toString());
		return result;
	}
	
	
	// TODO: Make this part more efficient, as it is used in each iteration
	public static String padWithZeros(double xd){
		String result="";
		long x=Math.round(Math.floor(xd));
		int noOfDigitsMaxLong=19; // max long is 9223372036854775807 (signed version) and it has 19 digits
		
		for (int i=0;i<noOfDigitsMaxLong-Long.toString(x).length();i++){
			result=result.concat("0");
		}
		
		result=result.concat(new Double(xd).toString());
		return result;
	}
	
}
