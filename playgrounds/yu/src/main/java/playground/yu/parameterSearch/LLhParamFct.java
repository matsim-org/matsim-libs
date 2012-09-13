///* *********************************************************************** *
// * project: org.matsim.*
// * LLhParamFct.java
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
//import java.util.Map;
//import java.util.TreeMap;
//
//import org.apache.commons.math.FunctionEvaluationException;
//import org.apache.commons.math.analysis.MultivariateRealFunction;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//
//import playground.yu.integration.cadyts.CalibrationConfig;
//import playground.yu.scoring.withAttrRecorder.leftTurn.LeftTurnPenaltyControler;
//import playground.yu.tests.parameterCalibration.naiveWithoutUC.SimCntLogLikelihoodCtlListener;
//import playground.yu.utils.io.SimpleWriter;
//
///**
// * @author C
// * 
// */
//public class LLhParamFct implements MultivariateRealFunction {
//	private final Config cfg;
//	private final String[] paramNames;
//	private final SimpleWriter writer;
//
//	public LLhParamFct(String configFilename) {
//		cfg = ConfigUtils.loadConfig(configFilename);
//		String parameterDimensionStr = cfg.findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME, "parameterDimension");
//
//		int paramDim;
//		if (parameterDimensionStr != null) {
//			paramDim = Integer.parseInt(parameterDimensionStr);
//		} else {
//			throw new RuntimeException(
//					"bse.parameterDimension should NOT be null!");
//		}
//
//		paramNames = new String[paramDim];
//		for (int i = 0; i < paramDim; i++) {
//			paramNames[i] = cfg.findParam(
//					CalibrationConfig.BSE_CONFIG_MODULE_NAME,
//					CalibrationConfig.PARAM_NAME_INDEX + i);
//			if (paramNames[i] == null) {
//				throw new RuntimeException("bse.parameterName_" + i
//						+ " should NOT be null!");
//			}
//		}
//
//		writer = new SimpleWriter(cfg.controler().getOutputDirectory()
//				+ "/optimization.log");
//	}
//
//	@Override
//	public double value(double[] point) throws FunctionEvaluationException,
//			IllegalArgumentException {
//		// check dimension of point and parameters
//		int dim = paramNames.length;
//		if (dim != point.length) {
//			throw new RuntimeException(
//					"The point dimension should equals the number of parameters that are being searching.");
//		}
//
//		Map<String, Double> nameParameters = new TreeMap<String, Double>();
//		for (int i = 0; i < dim; i++) {
//			nameParameters.put(paramNames[i], point[i]);
//		}
//		ParametersSetter.setParametersInConfig(cfg, nameParameters);
//
//		LeftTurnPenaltyControler controler = new LeftTurnPenaltyControler(cfg);
//
//		SimCntLogLikelihoodCtlListener llhListener = new SimCntLogLikelihoodCtlListener();
//		controler.addControlerListener(llhListener);
//
//		controler.setOverwriteFiles(true);
//		controler.setCreateGraphs(false);
//
//		controler.run();
//
//		writer.write("point:");
//		for (int i = 0; i < dim; i++) {
//			writer.write("\t" + point[i]);
//		}
//		double avgLlh = llhListener.getAverageLoglikelihood();
//		writer.writeln("\t--> avg. Log-likelihood:\t" + avgLlh);
//		writer.flush();
//
//		return avgLlh;
//	}
//
//	/**
//	 * @return the initial parameter values as the first search point
//	 */
//	public double[] getFirstPoint() {
//		double[] point = new double[paramNames.length];
//		int n = 0;
//		for (String parameterName : paramNames) {
//			point[n] = ParametersGetter.getValueOfParameter(cfg, parameterName);
//			n++;
//		}
//		return point;
//	}
//
//	public double getRelativeThreshold() {
//		String relativeThresholdStr = cfg.findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME, "relativeThreshold");
//		if (relativeThresholdStr != null) {
//			return Double.parseDouble(relativeThresholdStr);
//		}
//		return 1e-6;
//	}
//
//	public double getAbsoluteThreshold() {
//		String absoluteThresholdStr = cfg.findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME, "absoluteThreshold");
//		if (absoluteThresholdStr != null) {
//			return Double.parseDouble(absoluteThresholdStr);
//		}
//		return 1e-3;
//	}
//
//	/**
//	 * maxIterations is not the lastIteration in MATSim Config
//	 * ControlerConfigGroup, but the maximal iterations for parameter search.
//	 * 
//	 * @return
//	 */
//	public int getMaxIterations() {
//		String maxIterationsStr = cfg.findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME, "maxIterations");
//		if (maxIterationsStr != null) {
//			return Integer.parseInt(maxIterationsStr);
//		}
//		return 10000;
//	}
//
//	public int getMaxEvaluations() {
//		String maxEvaluationsStr = cfg.findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME, "maxEvaluations");
//		if (maxEvaluationsStr != null) {
//			return Integer.parseInt(maxEvaluationsStr);
//		}
//		return 10000;
//	}
//}
