/* *********************************************************************** *
 * project: org.matsim.*
 * DgReferenceTest
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.dgrether.designdrafts;


/**
 * @author dgrether
 *
 */
public class DgReferenceTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		A a = new A();
		Object o = a.pointer;
		System.out.println(o);
		a.pointer = new Object();
		System.out.println(o);
		System.out.println(a.pointer);
		o = a.pointer;
		a.pointer = new Object();
		System.out.println(o);
		System.out.println(a.pointer);
		
	}

	
	private static class A {
		Object pointer = null;
	}
	
}
