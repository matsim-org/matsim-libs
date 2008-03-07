/* *********************************************************************** *
 * project: org.matsim.*
 * InterfaceTest.java
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

public class InterfaceTest {
	public static interface If {
		public double getScore();
	}

	public static class A implements If {
		private final If anotherIf;

		public A(final If anotheIf) {
			this.anotherIf = anotheIf;
		}

		public double getScore() {
			double rs=anotherIf.getScore() - 123;
			System.out.println("Nr. of A-object: "+rs);
			return rs;
		}

	}
	public static class B implements If{

		public double getScore() {
			double rd=Math.random();
			System.out.println("random-Nr. of B-object: "+rd);
			return rd;
		}
		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		B b=new B();
		A a=new A(b);
		System.out.println("output in main( String[] args): "+a.getScore());
	}

}
