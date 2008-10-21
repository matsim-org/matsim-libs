/* *********************************************************************** *
 * project: org.matsim.*
 * DebugDecisionTree.java
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

package playground.gregor.withindayevac.debug;

import playground.gregor.withindayevac.analyzer.Analyzer;
import playground.gregor.withindayevac.analyzer.ChooseRandomLinkAnalyzer;
import playground.gregor.withindayevac.analyzer.FollowFastestAgentAnalyzer;
import playground.gregor.withindayevac.analyzer.FollowHerdAnalyzer;
import playground.gregor.withindayevac.analyzer.FollowPlanAnalyzer;
import playground.gregor.withindayevac.analyzer.ReRouteAnalyzer;

public class DebugDecisionTree {
	
	private static int CALLS = 0;
	private static int FOLLOW_PLAN = 0;
	private static int FOLLOW_FASTEST = 0;
	private static int RND = 0;
	private static int RR = 0;
	private static int HERD = 0;
	private static int ELSE = 0;
	
	
	public static void reset() {
		CALLS = 0;
		FOLLOW_PLAN = 0;
		FOLLOW_FASTEST = 0;
		RND = 0;
		RR = 0;
		HERD = 0;
		ELSE = 0;
	}
	
	public static void incr(final Analyzer ana){
		CALLS++;
		if (ana instanceof FollowPlanAnalyzer) {
			FOLLOW_PLAN++;
		} else if (ana instanceof FollowFastestAgentAnalyzer) {
			FOLLOW_FASTEST++;
		} else if (ana instanceof ChooseRandomLinkAnalyzer) {
			RND++;
		} else if (ana instanceof ReRouteAnalyzer) {
			RR++;
		}  else if (ana instanceof FollowHerdAnalyzer) {
			HERD++;
		}else {
			ELSE++;
		}
	}

	public static void print() {
		System.out.println("================D E B U G  I N F O================");
		System.out.println("Follow Plan:");
		System.out.println("\ttotal: " + FOLLOW_PLAN);
		System.out.println("\tperc: " +  (double)FOLLOW_PLAN / (double)CALLS);
		System.out.println();
		System.out.println("Follow Fastest:");
		System.out.println("\ttotal: " + FOLLOW_FASTEST);
		System.out.println("\tperc: " + (double)FOLLOW_FASTEST / (double)CALLS);
		System.out.println();
		System.out.println("Choose Random:");
		System.out.println("\ttotal: " + RND);
		System.out.println("\tperc: " + (double)RND / (double)CALLS);
		System.out.println();
		System.out.println("ReRoute:");
		System.out.println("\ttotal: " + RR);
		System.out.println("\tperc: " + (double)RR / (double)CALLS);
		System.out.println();
		System.out.println("Herd:");
		System.out.println("\ttotal: " + HERD);
		System.out.println("\tperc: " + (double)HERD / (double)CALLS);
		System.out.println();		
		System.out.println("Else:");
		System.out.println("\ttotal: " + ELSE);
		System.out.println("\tperc: " + (double)ELSE / (double)CALLS);
		System.out.println("================D E B U G  I N F O================");
	}
}
