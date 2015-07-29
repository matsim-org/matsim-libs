///* *********************************************************************** *
// * project: org.matsim.*
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2013 by the members listed in the COPYING,     *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package playground.jjoubert.optimisation;
//
//import net.sf.javailp.Linear;
//import net.sf.javailp.OptType;
//import net.sf.javailp.Problem;
//import net.sf.javailp.Result;
//import net.sf.javailp.Solver;
//import net.sf.javailp.SolverFactory;
//import net.sf.javailp.SolverFactoryGLPK;
//
//public class Test {
//
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		SolverFactory factory = new SolverFactoryGLPK();
//		factory.setParameter(Solver.VERBOSE, 0);
//		factory.setParameter(Solver.TIMEOUT, 30);
//
//		/**
//		* Constructing a Problem:
//		* Maximize: 143x+60y
//		* Subject to:
//		* 120x+210y <= 15000
//		* 110x+30y <= 4000
//		* x+y <= 75
//		*
//		* With x,y being integers
//		*
//		*/
//		Problem problem = new Problem();
//
//		Linear linear = new Linear();
//		linear.add(143, "x");
//		linear.add(60, "y");
//
//		problem.setObjective(linear, OptType.MAX);
//
//		linear = new Linear();
//		linear.add(120, "x");
//		linear.add(210, "y");
//
//		problem.add(linear, "<=", 15000);
//
//		linear = new Linear();
//		linear.add(110, "x");
//		linear.add(30, "y");
//
//		problem.add(linear, "<=", 4000);
//
//		linear = new Linear();
//		linear.add(1, "x");
//		linear.add(1, "y");
//
//		problem.add(linear, "<=", 75);
//
//		problem.setVarType("x", Integer.class);
//		problem.setVarType("y", Integer.class);
//
//		Solver solver = factory.get(); // you should use this solver only once for one problem
//		Result result = solver.solve(problem);
//
//		System.out.println(result);
//
//		/**
//		* Extend the problem with x <= 16 and solve it again
//		*/
//		problem.setVarUpperBound("x", 16);
//
//		solver = factory.get();
//		result = solver.solve(problem);
//
//		System.out.println(result);
//
//	}
//
//}
