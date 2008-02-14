/* *********************************************************************** *
 * project: org.matsim.*
 * HashMapTest.java
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

package playground.yu.test;

import java.util.HashMap;

public class HashMapTest {
	private static class MyClass {
		private StringBuffer sb;

		public MyClass(int i) {
			sb = new StringBuffer(Double.toString((double) i * Math.PI));
		}

		public StringBuffer append(String s) {
			return sb.append(s);
		}

		public String toString() {
			return sb.toString();
		}
	}

	public static void main(String[] args) {
		HashMap<Integer, MyClass> hm = new HashMap<Integer, MyClass>();
		for (int i = 0; i < 100; i++) {
			hm.put(i, new MyClass(i));
		}
		System.out.println("101-remove:" + hm.remove(101));
		System.out.println("102-remove:" + hm.remove(102));
		hm.get(98).append("100- appended String...");
		hm.get(99).append("99- appended String...");
		System.out.println("97: "+hm.get(97));
		System.out.println("98: "+hm.get(98));
		System.out.println("99: "+hm.get(99));
		System.out.println("100: "+hm.get(100));
	}
}
