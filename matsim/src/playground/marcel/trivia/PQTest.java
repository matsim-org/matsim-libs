/* *********************************************************************** *
 * project: org.matsim.*
 * PQTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.marcel.trivia;

import java.util.PriorityQueue;

/**
 * A trivial test how a PriorityQueue reacts to changes to the key of inserted objects.
 *
 * @author mrieser
 */
public class PQTest {

	static class Entry implements Comparable<Entry> {
		public int key;
		public int data;
		
		public Entry(int key, int data) {
			this.key = key;
			this.data = data;
		}
		
		public int compareTo(Entry o) {
			if (this.key < o.key) return -1;
			if (this.key > o.key) return +1;
			if (this.data < o.data) return -1;
			if (this.data > o.data) return +1;
			return 0;
		}
	}

	public static void main(String[] args) {
		PriorityQueue<Entry> pq = new PriorityQueue<Entry>();
		Entry e[] = new Entry[12];
		for (int i = 0; i < 12; i++) {
			e[i] = new Entry(i, i);
		}
		
		// insert the entries "randomly"
		pq.add(e[3]);
		pq.add(e[8]);
		pq.add(e[5]);
		pq.add(e[6]);
		pq.add(e[11]);
		pq.add(e[2]);
		pq.add(e[9]);
		pq.add(e[1]);
		pq.add(e[10]);
		pq.add(e[7]);
		pq.add(e[4]);
		
//		e[1].key = 6; // doesn't work
//		e[2].key = 6; // doesn't work
//		e[4].key = 8; // works luckily
		// in the examples above, we increased the key. 
		// But in our MATSim cases, we would only decrease the keys...
		// so let's try that:		
//		e[9].key = 1; // doesn't work, but we try to modify the "head" of the queue, which may be a special case
//		e[11].key = 5; // works luckily
		e[11].key = 3; // doesn't work
		
		Entry ee;
		while ((ee = pq.poll()) != null) {
			System.out.println("got entry: key=" + ee.key + ", data=" + ee.data);
		}
		
	}
	
}
