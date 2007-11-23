/* *********************************************************************** *
 * project: org.matsim.*
 * ExceptionTest.java
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

public class ExceptionTest {

	static final int MAX_LOOP = 100000;
	
	long counter = 0;
	
	public void doSomething() {
		counter = counter + 1;
	}
	
	public void doSomething1() {
		doSomething();
		if (Math.random() > 2.0) {
			System.out.println("Math.random() should never be larger than 2.0!");
		}
	}
	
	public void doSomething2() throws Exception {
		doSomething();
		if (Math.random() > 2.0) {
			throw new Exception("Math.random() should never be larger than 2.0!");
		}
	}

	public void doSomething3() throws RuntimeException {
		doSomething();
		if (Math.random() > 2.0) {
			throw new RuntimeException("Math.random() should never be larger than 2.0!");
		}
	}
	
	public void run() {

		long start = 0;
		long time0 = 0;
		long time1 = 0;
		long time2 = 0;
		long time3 = 0;
		long time4 = 0;
		
		counter = 0;

		// TEST no exceptions
		start = System.currentTimeMillis();
		for (int i = 0; i < MAX_LOOP; i++) {
			doSomething1();
		}
		time0 = System.currentTimeMillis() - start;
		
		// TEST no exceptions
		start = System.currentTimeMillis();
		for (int i = 0; i < MAX_LOOP; i++) {
			doSomething1();
		}
		time1 = System.currentTimeMillis() - start;

		// TEST Exception, multiple try-catch-blocks
		start = System.currentTimeMillis();
		for (int i = 0; i < MAX_LOOP; i++) {
			try {
				doSomething2();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		time2 = System.currentTimeMillis() - start;

		// TEST Exception, single try-catch-block
		start = System.currentTimeMillis();
		try {
			for (int i = 0; i < MAX_LOOP; i++) {
				doSomething2();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		time3 = System.currentTimeMillis() - start;

		// TEST, RuntimeException, no try-catch-block
		start = System.currentTimeMillis();
		for (int i = 0; i < MAX_LOOP; i++) {
			doSomething3();
		}
		time4 = System.currentTimeMillis() - start;

		// OUTPUT times
		System.out.println(time0 + "\t" + time1 + "\t" + time2 + "\t" + time3 + "\t" + time4 + "\t" + counter);
	
	}
	
	public static void main(String[] args) {
		ExceptionTest test = new ExceptionTest();
		for (int i = 0; i < 50; i++) {
			test.run();
		}
	}

}
