///* *********************************************************************** *
// * project: org.matsim.*
// * MultidirectionalSearcher.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
//package playground.yu.parameterSearch;
//
//import org.apache.commons.math.FunctionEvaluationException;
//import org.apache.commons.math.optimization.GoalType;
//import org.apache.commons.math.optimization.OptimizationException;
//import org.apache.commons.math.optimization.SimpleScalarValueChecker;
//import org.apache.commons.math.optimization.direct.MultiDirectional;
//
///**
// * An attempt to use multi-directional direct search method to search the best
// * parameters in scoring function
// * 
// * @author yu
// * 
// */
//public class MultidirectionalSearcher {
//
//	public static void main(String[] args/*
//										 * TODO args for criterion and start
//										 * point
//										 */) throws OptimizationException,
//			FunctionEvaluationException, IllegalArgumentException {
//		MultiDirectional optimizer = new MultiDirectional();
//		optimizer.setMaxIterations(1000);
//		optimizer.setMaxEvaluations(1000);
//
//		optimizer.setConvergenceChecker(new SimpleScalarValueChecker(0.001,
//				0.001));
//		optimizer.optimize(new LLhParamFct(args[0]), GoalType.MAXIMIZE,
//				new double[] { -4.5, -1d });
//	}
//
//}
