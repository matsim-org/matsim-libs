///* *********************************************************************** *
// * project: org.matsim.*
// * NelderMeadSearcher.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
//package playground.yu.parameterSearch;
//
//import org.apache.commons.math.FunctionEvaluationException;
//import org.apache.commons.math.optimization.GoalType;
//import org.apache.commons.math.optimization.OptimizationException;
//import org.apache.commons.math.optimization.RealPointValuePair;
//import org.apache.commons.math.optimization.SimpleScalarValueChecker;
//import org.apache.commons.math.optimization.direct.NelderMead;
//
///**
// * An attempt to use Nelder-Mead Method to search the best parameters in scoring
// * function
// *
// * @author C
// *
// */
//public class NelderMeadSearcher {
//
//	private final NelderMead optimizer;
//	private final LLhParamFct objectiveFunction;
//
//	public NelderMeadSearcher(String configFilename) {
//		objectiveFunction = new LLhParamFct(configFilename);
//
//		optimizer = new NelderMead();// used default setting in NelderMead
//		/* rho = 1.0; khi = 2.0; gamma = 0.5; sigma = 0.5; */
//		optimizer.setMaxIterations(objectiveFunction.getMaxIterations());
//		optimizer.setMaxEvaluations(objectiveFunction.getMaxEvaluations());
//		optimizer.setConvergenceChecker(new SimpleScalarValueChecker(
//				objectiveFunction.getRelativeThreshold(), objectiveFunction
//						.getAbsoluteThreshold()));
//	}
//
//	public void run() throws OptimizationException,
//			FunctionEvaluationException, IllegalArgumentException {
//		RealPointValuePair pointValPair = optimizer.optimize(objectiveFunction,
//				GoalType.MAXIMIZE, objectiveFunction.getFirstPoint());
//		System.out.println("We have found an optimum, point\t"
//				+ pointValPair.getPoint() + "\tvalue\t"
//				+ pointValPair.getValue());
//	}
//
//	/**
//	 * @param args
//	 * @throws IllegalArgumentException
//	 * @throws FunctionEvaluationException
//	 * @throws OptimizationException
//	 */
//	public static void main(String[] args) throws OptimizationException,
//			FunctionEvaluationException, IllegalArgumentException {
//		new NelderMeadSearcher(args[0]).run();
//	}
//}
