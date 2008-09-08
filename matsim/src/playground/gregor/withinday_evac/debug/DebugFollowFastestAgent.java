/* *********************************************************************** *
 * project: org.matsim.*
 * DebugFollowFastestAgent.java
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

package playground.gregor.withinday_evac.debug;

public class DebugFollowFastestAgent {
	
	private static int GREEDY = 0;
	private static int ALTERNATIVES = 0;
	private static int CALLS = 0;
	private static int NULL = 0;
	
	public static void updateAlt(final int alt) {
		CALLS++;
		ALTERNATIVES += alt;
	}
	
	public static void incrGreedy(){
		GREEDY++;
	}
	
	public static void increNull() {
		NULL++;
	}
	public static void reset(){
		GREEDY = 0;
		ALTERNATIVES = 0;
		CALLS = 0;
		NULL  = 0;
	}
	
	public static void print() {
		System.out.println("================D E B U G  I N F O (FollowFastest)================");
		System.out.println("non-Greedy:");
		System.out.println("\ttotal: " + GREEDY);
		System.out.println("\tperc: " +  (double)GREEDY / (double)CALLS);
		System.out.println();
		System.out.println("Alternatives:");
		System.out.println("\taverage: " + (double)ALTERNATIVES / (double)CALLS);
		System.out.println("Null:");
		System.out.println("\ttotal: " + NULL);
		System.out.println("================D E B U G  I N F O (FollowFastest)================");
	}


}
