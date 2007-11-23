/* *********************************************************************** *
 * project: org.matsim.*
 * CollectionTest.java
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

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.SortedSet;
import java.util.TreeSet;

public class CollectionTest {

	public class TestObject {
		public int time;
		public int id;
		
		public TestObject(int time, int id) {
			this.time = time;
			this.id = id;
		}
		
		public String toString() {
			return "[" + id + " @ " + time + "]";
		}
	}
	
	
	public class TestComparator implements Comparator<TestObject> {

		public int compare(TestObject o1, TestObject o2) {
			if (o1.time < o2.time) return -1;
			if (o1.time > o2.time) return +1;
			return 0;
		}
		
	}
	
	public void run() {
		TestObject o = null;
		TestObject o1 = new TestObject(1, 10);
		TestObject o2 = new TestObject(2, 20);
		TestObject o3 = new TestObject(1, 11);
	
		System.out.println("test priority queue");
		
		PriorityQueue<TestObject> pq = new PriorityQueue<TestObject>(5, new TestComparator());
		pq.add(o1);
		pq.add(o2);
		pq.add(o3);
		
		System.out.println("poll entries one after the other, they should be ordered");

		o = pq.poll();
		System.out.println(o);
		o = pq.poll();
		System.out.println(o);
		o = pq.poll();
		System.out.println(o);

		System.out.println("use iterator, they must not ordered");

		// okay, after those polls, our queue is empty, fill it again

		pq.add(o1);
		pq.add(o2);
		pq.add(o3);

		for (Iterator<TestObject> iter = pq.iterator(); iter.hasNext(); ) {
			o = iter.next();
			System.out.println(o);
		}

		System.out.println("------------------------------");
		System.out.println("remove " + o3);
		pq.remove(o3);

		for (Iterator<TestObject> iter = pq.iterator(); iter.hasNext(); ) {
			o = iter.next();
			System.out.println(o);
		}
		
		System.out.println("oops, " + o3 + " is still in the queue, but " + o1 + " seems to be missing!");
		
		
		System.out.println();
		System.out.println("test sorted set");
		
		SortedSet<TestObject> set = new TreeSet<TestObject>(new TestComparator());
		set.add(o1);
		set.add(o2);
		set.add(o3);
		
		for (Iterator<TestObject> iter = set.iterator(); iter.hasNext(); ) {
			o = iter.next();
			System.out.println(o);
		}
		
		System.out.println("hmm, why are there only two entries??");
		
	}
	
	public static void main(String[] args) {
		CollectionTest test = new CollectionTest();
		test.run();		
	}

}
