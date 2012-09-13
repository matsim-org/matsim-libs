///* *********************************************************************** *
// * project: org.matsim.*
// * LJParaemterSearchListener.java
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
//package playground.yu.parameterSearch.LJ;
//
//import java.util.Map;
//import java.util.TreeMap;
//
//import org.matsim.core.config.Config;
//import org.matsim.core.controler.Controler;
//import org.matsim.core.controler.events.IterationEndsEvent;
//import org.matsim.core.controler.events.StartupEvent;
//import org.matsim.core.controler.listener.IterationEndsListener;
//import org.matsim.core.controler.listener.StartupListener;
//
//import playground.yu.integration.cadyts.CalibrationConfig;
//import playground.yu.parameterSearch.ParametersGetter;
//import playground.yu.parameterSearch.ParametersSetter;
//import playground.yu.parameterSearch.PatternSearchAlgoI;
//import playground.yu.parameterSearch.PatternSearchListener;
//import playground.yu.scoring.ReScoringFromCustomAttr;
//import playground.yu.tests.parameterCalibration.naiveWithoutUC.SimCntLogLikelihoodCtlListener;
//
//public class LJParaemterSearchListener extends PatternSearchListener implements
//		IterationEndsListener, StartupListener {
//	private PatternSearchAlgoI patternSearch;
//	static int paramDim;
//	static String[] paramNames/* in configfile */;
//
//	@Override
//	public void notifyStartup(StartupEvent event) {
//		super.notifyStartup(event);
//
//		Controler ctl = event.getControler();
//		Config config = ctl.getConfig();
//
//		String parameterDimensionStr = config.findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME, "parameterDimension");
//		if (parameterDimensionStr != null) {
//			paramDim = Integer.parseInt(parameterDimensionStr);
//		} else {
//			throw new RuntimeException(
//					"bse.parameterDimension should NOT be null!");
//		}
//
//		paramNames = new String[paramDim];
//		double[] lowerBoundaries = new double[paramDim], upperBoundaries = new double[paramDim];
//		for (int i = 0; i < paramDim; i++) {
//			paramNames[i] = config.findParam(
//					CalibrationConfig.BSE_CONFIG_MODULE_NAME,
//					CalibrationConfig.PARAM_NAME_INDEX + i);
//			if (paramNames[i] == null) {
//				throw new RuntimeException("bse.parameterName_" + i
//						+ " should NOT be null!");
//			}
//
//			lowerBoundaries[i] = -20d;
//			String lowBdryStr = config.findParam(
//					CalibrationConfig.BSE_CONFIG_MODULE_NAME,
//					LOWER_BOUNDARY_OF_PARAMETER_ + i);
//			if (lowBdryStr != null) {
//				lowerBoundaries[i] = Double.parseDouble(lowBdryStr);
//			}
//
//			upperBoundaries[i] = 10;
//			String upBdryStr = config.findParam(
//					CalibrationConfig.BSE_CONFIG_MODULE_NAME,
//					UPPER_BOUNDARY_OF_PARAMETER_ + i);
//			if (upBdryStr != null) {
//				upperBoundaries[i] = Double.parseDouble(upBdryStr);
//			}
//		}
//
//		double criterion = 0.1;
//		String criterionStr = config.findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME, CRITERION);
//		if (criterionStr != null) {
//			criterion = Double.parseDouble(criterionStr);
//		}
//
//		int maxIter = 300;
//		String maxIterStr = config.findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME, MAX_ITER);
//		if (maxIterStr != null) {
//			maxIter = Integer.parseInt(maxIterStr);
//		}
//
//		patternSearch = new LJAlgorithm(lowerBoundaries, upperBoundaries,
//				criterion, maxIter);
//		((LJAlgorithm) patternSearch).setOutputFilename(ctl.getControlerIO()
//				.getOutputFilename("LJ.log"));
//		double[] parameters = new double[paramDim];
//		for (int i = 0; i < paramDim; i++) {
//			parameters[i] = ParametersGetter.getValueOfParameter(config,
//					paramNames[i]);
//		}
//		((LJAlgorithm) patternSearch).initializeParameters(parameters);
//	}
//
//	@Override
//	public void notifyIterationEnds(IterationEndsEvent event) {
//		LJParameterHunter hunter = (LJParameterHunter) event.getControler();
//		int iteration = event.getIteration();
//		SimCntLogLikelihoodCtlListener llhListener = hunter.getLlhListener();
//		if (iteration % llhListener.getWriteLlhInterval() == 0
//				&& iteration > hunter.getFirstIteration()) {
//
//			// set objective into patternSearch
//			patternSearch
//					.setObjective(-llhListener.getAverageLoglikelihood()/* negative */);
//
//			// set trials of parameters from patternSearch into config and
//			// Controler ...
//			double[] trial = patternSearch.getTrial();
//			Map<String, Double> nameParameters = new TreeMap<String, Double>();
//			for (int i = 0; i < paramDim; i++) {
//				nameParameters.put(paramNames[i], trial[i]);
//				nameParametersMap.put(paramNames[i], trial[i]);
//			}
//			ParametersSetter.setParameters(hunter, nameParameters);
//
//			// reScoring
//			new ReScoringFromCustomAttr().run(hunter.getPopulation());
//		}
//
//	}
//
//}
