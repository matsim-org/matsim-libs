/* *********************************************************************** *
 * project: org.matsim.*
 * WordleGenerator.java
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

package playground.jjoubert.Utilities;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

public class WordleGenerator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String filename = args[0];
		
		List<Tuple<String,Integer>> list = getDone();
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			for(Tuple<String,Integer> t : list){
				for(int i = 0; i < t.getSecond(); i++){
					bw.write(t.getFirst());
					bw.write(" ");
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter "
					+ filename);
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter "
						+ filename);
			}
		}
	}
	
	private static List<Tuple<String,Integer>> getDone(){
		List<Tuple<String,Integer>> list = new ArrayList<Tuple<String,Integer>>();
		/* Top list */
		list.add(new Tuple<String,Integer>("recylce", 100));
		list.add(new Tuple<String,Integer>("reduce", 100));
		list.add(new Tuple<String,Integer>("reuse", 100));
		
		/* Higher list, 20 - 40 */
		int lower = 25;
		int upper = 40;
		list.add(new Tuple<String,Integer>("compost", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("bokashi", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("environment", (int) (lower + Math.random()*(upper - lower))));

		
		/* Lower list, 1 - 20. */
		lower = 10;
		upper = 25;
		list.add(new Tuple<String,Integer>("plant", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("food scraps", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("garden refuse", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("organisms", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("bacteria", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("organic", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("fermentation", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("biodegradable", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("microorganism", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("leftovers", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("cycle", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("waste", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("carbon", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("greenhouse", (int) (lower + Math.random()*(upper - lower))));
		
		return list;
	}
	
	
	
	private static List<Tuple<String,Integer>> getBoz(){
		List<Tuple<String,Integer>> list = new ArrayList<Tuple<String,Integer>>();
		
		/* Top list */
		list.add(new Tuple<String,Integer>("BOZ312", 100));
		list.add(new Tuple<String,Integer>("Operations Research", 80));
		list.add(new Tuple<String,Integer>("Optimisation", 65));
		list.add(new Tuple<String,Integer>("Objective", 65));
		list.add(new Tuple<String,Integer>("Constraint", 65));
		list.add(new Tuple<String,Integer>("Mathematical programming", 65));
		list.add(new Tuple<String,Integer>("decision variable", 50));

		/* Higher list, 20 - 40 */
		int lower = 25;
		int upper = 40;
		list.add(new Tuple<String,Integer>("Model", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("parameter", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("choice", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("function", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("for all", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("Sum", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("set", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("pareto", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("optimal", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("strategic", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("planning", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("inventory", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("logistics", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("network", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("graph", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("Convex", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("NP-hard", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("heuristic", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("incumbent", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("linear", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("game", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("integer", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("binary", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("Combinatorial", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("minimize", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("maximisation", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("Economic", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("evaluation", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("Location", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("resources", (int) (lower + Math.random()*(upper - lower))));
		
		/* Lower list, 1 - 20. */
		upper = 25;
		lower = 10;
		list.add(new Tuple<String,Integer>("multiple", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("chance-constraint", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("recourse", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("iterative", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("Simplex", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("assignment", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("scheduling", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("workforce", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("sizing", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("routing", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("VRP", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("VRPTW", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("Lingo", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("GAMS", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("Java", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("Semester test", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("Industrial Engineering", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("algebra", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("Industrial Engineering", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("Theory", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("analysis", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("sensitivity", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("duality", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("nonlinear", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("mixed", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("branch-and-bound", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("transshipment", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("path", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("Knapsack", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("enumeration", (int) (lower + Math.random()*(upper - lower))));
		return list;
	}
	
	
	private static List<Tuple<String,Integer>> getMatsim(){
		List<Tuple<String,Integer>> list = new ArrayList<Tuple<String,Integer>>();

		/* Primary */
		list.add(new Tuple<String,Integer>("MATSim", 100));
		list.add(new Tuple<String,Integer>("population", 40));
		list.add(new Tuple<String,Integer>("network", 40));
		list.add(new Tuple<String,Integer>("agent", 40));

		/* Secondary */
		double upper = 25.0;
		double lower = 10.0;
		list.add(new Tuple<String,Integer>("scoring", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("initial", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("demand", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("transport", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("link", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("routing", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("event", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("replanning", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("mobility", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("simulation", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("multi-agent", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("analysis", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("analysis", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("shortest-path", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("utility", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("best-response", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("beta", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("changeExpBeta", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("mutation", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("strategy", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("replanning", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("opensource", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("senozon", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("via", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("gtfs", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("households", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("facilities", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("value-of-time", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("scenario", (int) (lower + Math.random()*(upper - lower))));
		
		
		return list;
	}
	
	private static List<Tuple<String,Integer>> getIE(){
		List<Tuple<String,Integer>> list = new ArrayList<Tuple<String,Integer>>();
		
		/* Primary */
//		list.add(new Tuple<String,Integer>("Bedryfsingenieurswese", 80));
		list.add(new Tuple<String,Integer>("Industrial", 80));
		list.add(new Tuple<String,Integer>("Engineering", 60));
		list.add(new Tuple<String,Integer>("Optimisation", 40));
		
		/* Secondary */
		double upper = 25.0;
		double lower = 10.0;
		list.add(new Tuple<String,Integer>("decision", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("modelling", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("simulation", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("operations", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("research", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("logistics", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("supply chain", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("analysis", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("labour", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("business", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("quality", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("engineering", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("enterprise", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("facilities", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("planning", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("science", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("analytics", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("inventory", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("routing", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("strategy", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("business", (int) (lower + Math.random()*(upper - lower))));
		list.add(new Tuple<String,Integer>("industrial", (int) (lower + Math.random()*(upper - lower))));
		
		return list;
		
	}

}

