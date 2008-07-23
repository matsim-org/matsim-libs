package playground.wrashid.PDES;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.PriorityBlockingQueue;

import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;

public class MessageQueue {
	//TreeMap<String, Message> queue = new TreeMap<String, Message>();
	PriorityBlockingQueue<Message> queue1 = new PriorityBlockingQueue<Message>(10000);
	long counter = 0;

	// A Map (TreeMap, HashMap, etc.) can only contain one value for the same
	// key
	// This means, it is not possible to insert two messages into the queue,
	// keyed with
	// the same time
	// Inorder to prevent this, a counter is attached to the time, for
	// "assuring" uniqueness
	// (if more than 2^64 (approx. 10^19) events are scheduled for the same
	// time, than the uniqueness will not hold anymore...)
	// (although this case seems quite impossible, never the less it is
	// mentioned as a precondition for using this scheduler)
	void putMessage(Message m) {
		long rlong = counter++;
		// padding with zeros is needed, inorder to be able to use firstKey
		// property of the queue
		// String zeroPadded=feedWithZeros(m.getMessageArrivalTime());
		/*
		String zeroPadded = padWithZeros(m.getMessageArrivalTime());

		m.queueKey = zeroPadded.concat("." + (new Long(rlong)).toString());
		queue.put(m.queueKey, m);
*/
		
		queue1.add(m);

	}
	

	void removeMessage(Message m) {
		//queue.remove(m.queueKey);
		queue1.remove(m);
	}

	Message getNextMessage() {
		//Message m = queue.remove(queue.firstKey());
		Message m = queue1.poll();
		return m;
	}

	public boolean hasElement() {
		//return !queue.isEmpty();
		return !queue1.isEmpty();
	}

	// this is nesseary for comparison of strings,
	// because with string comparison 91 is bigger than 231 (because only 2 and
	// 9 are compared)
	public static String feedWithZeros(double xd) {
		String result = "";
		long x = Math.round(Math.floor(xd));
		long j = Long.MAX_VALUE;

		while (j > 0) {
			if (x / j <= 0) {
				result = result.concat("0");
			}
			j /= 10;
		}
		result = result.concat(new Double(xd).toString());
		return result;
	}

	// TODO: Make this part more efficient, as it is used in each iteration
	public static String padWithZeros(Double xd) {
		String result = "";
		final String zeros[] = new String[] { "", "0", "00", "000", "0000",
				"00000", "000000", "0000000", "00000000", "000000000",
				"0000000000", "00000000000", "000000000000", "0000000000000",
				"00000000000000", "000000000000000", "0000000000000000",
				"00000000000000000", "000000000000000000",
				"0000000000000000000", "00000000000000000000" };

		long x = Math.round(Math.floor(xd));
		final int noOfMaxPaddingChars = getNumberOfDigits(SimulationParameters.maxSimulationLength)+1;
		int numberOfDigits = 0;
		
		numberOfDigits=getNumberOfDigits(x);
		
		result = result.concat(zeros[noOfMaxPaddingChars-numberOfDigits]);

		result = result.concat(xd.toString());
		return result;
	}

	// input: 0 => output: 1
	// input: 4 => output: 1
	// input: 55 => output: 2
	public static int getNumberOfDigits(long lon) {
		int count = 1;
		while ((lon /= 10) != 0) {
			count++;
		}
		return count;
	}

	// not used any more, only here for testing purpose
	public static String padWithZeros_old(double xd) {
		String result = "";
		long x = Math.round(Math.floor(xd));
		int noOfDigitsMaxLong = 19; // max long is 9223372036854775807 (signed
									// version) and it has 19 digits

		for (int i = 0; i < noOfDigitsMaxLong - Long.toString(x).length(); i++) {
			result = result.concat("0");
		}

		result = result.concat(new Double(xd).toString());
		return result;
	}

	public static void main(String[] args) {
		Random r = new Random();
		double d = 0;
		for (int i = 0; i < 1; i++) {
			d = r.nextDouble();
			// System.out.println(padWithZeros(d) + " - " + feedWithZeros(d));
			// String s=padWithZeros(d);
			// padWithZeros(d);
			// feedWithZeros(d);
			System.out.println(d * 1);
			System.out.println(padWithZeros(d * 1));
			//System.out.println(getNumberOfDigits(222));
			// System.out.println(padWithZeros_old(d));
			// assert(padWithZeros(d).equalsIgnoreCase(feedWithZeros(d))==true);
			// assert(padWithZeros(d).equalsIgnoreCase(padWithZeros_old(d))==true);
		}
	}

}
