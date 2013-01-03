/* *********************************************************************** *
 * project: org.matsim.*
 * SpeedTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.matsim.core.gbl.MatsimRandom;

public class SpeedTest {

	private static final int NUM_TESTS = 10000000;
	private final int size = 10;
	private final List<Integer> data;
	public SpeedTest() {
		this.data = new ArrayList<Integer>();
		for (int i = 0; i < 1000; i++) {
			this.data.add(i);
		}
	}

	public void testArrayList() {
		MatsimRandom.reset();
		Random r = MatsimRandom.getLocalInstance();
		ArrayList<Integer> test = new ArrayList<Integer>(this.size);
		for (int i = 0; i < this.size; i++){
			int idx = r.nextInt(1000);
			test.add(this.data.get(idx));
		}

		int [] idx0s = new int[NUM_TESTS];
		int [] idx1s = new int[NUM_TESTS];
		for (int i = 0; i < NUM_TESTS; i++) {
			int idx0 = r.nextInt(this.size);
			int idx1 = r.nextInt(this.size);
			idx0s[i] = idx0;
			idx1s[i] = idx1;
		}

		long start = System.currentTimeMillis();
		for (int i = 0; i < NUM_TESTS; i++) {
			int idx0 = idx0s[i];
			int idx1 = idx1s[i];
			Integer tmp = test.get(idx0);
			test.set(idx0, test.get(idx1));
			test.set(idx1,tmp);
		}
		long stop = System.currentTimeMillis();
		System.out.println("ArrayList took:\t\t" + (stop-start));
	}

	public void testLinkedList() {
		MatsimRandom.reset();
		Random r = MatsimRandom.getLocalInstance();
		LinkedList<Integer> test = new LinkedList<Integer>();
		for (int i = 0; i < this.size; i++){
			int idx = r.nextInt(1000);
			test.add(this.data.get(idx));
		}

		int [] idx0s = new int[NUM_TESTS];
		int [] idx1s = new int[NUM_TESTS];
		for (int i = 0; i < NUM_TESTS; i++) {
			int idx0 = r.nextInt(this.size);
			int idx1 = r.nextInt(this.size);
			idx0s[i] = idx0;
			idx1s[i] = idx1;
		}

		long start = System.currentTimeMillis();
		for (int i = 0; i < NUM_TESTS; i++) {
			int idx0 = idx0s[i];
			int idx1 = idx1s[i];
			Integer tmp = test.get(idx0);
			test.set(idx0, test.get(idx1));
			test.set(idx1,tmp);
		}
		long stop = System.currentTimeMillis();
		System.out.println("LinkedList took:\t" + (stop-start));
	}
	
	public void testArray() {
		MatsimRandom.reset();
		Random r = MatsimRandom.getLocalInstance();
		Integer [] test = new Integer [this.size];
		for (int i = 0; i < this.size; i++){
			int idx = r.nextInt(1000);
			test[i]=(this.data.get(idx));
		}

		int [] idx0s = new int[NUM_TESTS];
		int [] idx1s = new int[NUM_TESTS];
		for (int i = 0; i < NUM_TESTS; i++) {
			int idx0 = r.nextInt(this.size);
			int idx1 = r.nextInt(this.size);
			idx0s[i] = idx0;
			idx1s[i] = idx1;
		}

		long start = System.currentTimeMillis();
		for (int i = 0; i < NUM_TESTS; i++) {
			int idx0 = idx0s[i];
			int idx1 = idx1s[i];
			Integer tmp = test[idx0];
			test[idx0] = test[idx1];
			test[idx1] = tmp;
		}
		long stop = System.currentTimeMillis();
		System.out.println("Array took:\t\t" + (stop-start));
	}

	public static void main(String [] args) {
		SpeedTest test = new SpeedTest();
		for (int i = 0; i < 100; i++) {
			test.testArrayList();
			test.testLinkedList();
			test.testArray();
		}
	}

}
