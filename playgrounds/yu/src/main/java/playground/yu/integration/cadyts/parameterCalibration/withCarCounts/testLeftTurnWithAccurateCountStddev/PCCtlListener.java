///* *********************************************************************** *
// * project: org.matsim.*
// * BseControlerListener.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
//package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.testLeftTurnWithAccurateCountStddev;
//
//import static java.lang.Math.max;
//import static java.lang.Math.sqrt;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.logging.Logger;
//
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.api.core.v01.population.Population;
//import org.matsim.core.basic.v01.IdImpl;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.groups.CountsConfigGroup;
//import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
//import org.matsim.core.controler.Controler;
//import org.matsim.core.controler.OutputDirectoryHierarchy;
//import org.matsim.core.controler.events.IterationEndsEvent;
//import org.matsim.core.controler.events.ShutdownEvent;
//import org.matsim.core.controler.events.StartupEvent;
//import org.matsim.core.controler.listener.IterationEndsListener;
//import org.matsim.core.controler.listener.ShutdownListener;
//import org.matsim.core.controler.listener.StartupListener;
//import org.matsim.core.gbl.MatsimRandom;
//import org.matsim.core.utils.charts.XYLineChart;
//import org.matsim.counts.Count;
//import org.matsim.counts.Counts;
//import org.matsim.counts.Volume;
//
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.BseLinkCostOffsetsXMLFileIO;
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.mnlValidation.MultinomialLogitChoice;
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.parametersCorrection.BseParamCalibrationControlerListener;
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.scoring.ScoringConfigGetSetValues;
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.testAttRecorder.PCStrMn;
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.testLeftTurn.Events2ScoreWithLeftTurnPenalty4PC;
//import playground.yu.integration.cadyts.utils.SampleVarianceReader;
//import playground.yu.scoring.withAttrRecorder.Events2Score4AttrRecorder;
//import playground.yu.scoring.withAttrRecorder.ScorAttrReader;
//import playground.yu.scoring.withAttrRecorder.leftTurn.CharyparNagelScoringFunctionFactoryWithLeftTurnPenalty;
//import playground.yu.utils.io.SimpleWriter;
//import utilities.math.MultinomialLogit;
//import utilities.math.Vector;
//import utilities.misc.DynamicData;
//import cadyts.calibrators.Calibrator;
//import cadyts.calibrators.analytical.ChoiceParameterCalibrator4;
//import cadyts.interfaces.matsim.MATSimChoiceParameterCalibrator;
//import cadyts.measurements.SingleLinkMeasurement.TYPE;
//
//public class PCCtlListener extends BseParamCalibrationControlerListener
//		implements StartupListener, ShutdownListener, IterationEndsListener {
//	private int caliStartTime, caliEndTime;
//	private int avgLlhOverIters = 0, writeLlhInterval = 0;
//	// private Config config;
//	// private int cycleIdx = 0, cycle;
//
//	private SimpleWriter writer = null, writerCV = null;
//	private static List<Link> links = new ArrayList<Link>();
//	private static Set<Id> linkIds = new HashSet<Id>();
//	private XYLineChart chart;// paramChart
//
//	private int paramDim;
//
//	static final String PARAM_NAME_INDEX = "parameterName_",
//			PARAM_STDDEV_INDEX = "paramStddev_";
//	static String[] paramNames/* in configfile */;
//
//	private double[][] paramArrays/* performing, traveling and so on */;
//
//	private double llhSum = 0d;
//	private boolean writeQGISFile = true;
//
//	private void initializeCalibrator(Controler ctl) {
//		org.matsim.core.config.Config config = ctl.getConfig();
//		OutputDirectoryHierarchy ctlIO = ctl.getControlerIO();
//
//		// SETTING "parameter calibration" parameters
//		// watching = Boolean.parseBoolean(config.findParam(
//		// BSE_CONFIG_MODULE_NAME, "watching"));
//
//		String parameterDimensionStr = config.findParam(BSE_CONFIG_MODULE_NAME,
//				"parameterDimension");
//		if (parameterDimensionStr != null) {
//			paramDim = Integer.parseInt(parameterDimensionStr);// e.g.
//			// =2
//			// [traveling,performing]
//		} else {
//			throw new RuntimeException("bse.parameterDimension muss be filled!");
//		}
//
//		paramNames = new String[paramDim];
//		for (int i = 0; i < paramNames.length; i++) {
//			paramNames[i] = config.findParam(BSE_CONFIG_MODULE_NAME,
//					PARAM_NAME_INDEX + i);
//		}
//
//		// int timeBinSize_s = Integer.parseInt(config.findParam(
//		// BSE_CONFIG_MODULE_NAME, "timeBinSize_s"));
//
//		// SETTING REGRESSIONINERTIA
//		String regressionInertiaValue = config.findParam(
//				BSE_CONFIG_MODULE_NAME, "regressionInertia");
//		double regressionInertia = 0;
//		if (regressionInertiaValue == null) {
//			System.out.println("BSE:\tregressionInertia\t= default value\t"
//					+ Calibrator.DEFAULT_REGRESSION_INERTIA);
//			regressionInertia = Calibrator.DEFAULT_REGRESSION_INERTIA;
//		} else {
//			regressionInertia = Double.parseDouble(regressionInertiaValue);
//			System.out.println("BSE:\tregressionInertia\t=\t"
//					+ regressionInertia);
//		}
//
//		List<Integer> calibratedParameterIndices = new ArrayList<Integer>();
//		for (String paramName : paramNames) {
//			calibratedParameterIndices
//					.add(Events2Score4AttrRecorder.attrNameList
//							.indexOf(paramName));
//		}
//
//		// INITIALIZING "Calibrator"
//		MatsimRandom.reset(config.global().getRandomSeed());
//		calibrator = new MATSimChoiceParameterCalibrator<Link>(
//				ctlIO.getOutputFilename("calibration.log"),
//				MatsimRandom.getLocalInstance(), regressionInertia,
//				calibratedParameterIndices);
//
//		// SETTING staticsFile
//		calibrator.setStatisticsFile(ctlIO
//				.getOutputFilename("calibration-stats.txt"));
//	}
//
//	private void initializeOutput(Controler ctl) {
//		OutputDirectoryHierarchy ctlIO = ctl.getControlerIO();
//		{
//			writer = new SimpleWriter(ctlIO.getOutputFilename("parameters.log"));
//			StringBuffer sb = new StringBuffer("iter");
//			for (int i = 0; i < paramNames.length; i++) {
//				sb.append("\tavg. ");
//				sb.append(paramNames[i]);
//
//			}
//			for (int i = 0; i < paramNames.length; i++) {
//				sb.append("\t");
//				sb.append(paramNames[i]);
//
//			}
//			writer.writeln(sb);
//		}
//
//		{
//			writerCV = new SimpleWriter(
//					ctlIO.getOutputFilename("parameterCovariance.log"));
//			StringBuffer sb = new StringBuffer("iter\t" + "VAR{"
//					+ paramNames[0]
//			// "covariance ["
//			);
//			for (int i = 1; i < paramNames.length; i++) {
//				sb.append(", ");
//				sb.append(paramNames[i]);
//			}
//			sb.append("|eps}"
//			// "]\texpectation of variance\tvariance of expectation"
//			);
//
//			writerCV.writeln(sb);
//		}
//
//		{
//			StringBuffer sb = new StringBuffer(paramNames[0]);
//			for (int i = 1; i < paramNames.length; i++) {
//				sb.append(" & ");
//				sb.append(paramNames[i]);
//			}
//			chart = new XYLineChart(sb.toString(), "iter", "value of parameter");
//		}
//		int arraySize = ctl.getLastIteration() - ctl.getFirstIteration() + 1;
//		paramArrays = new double[paramDim][];
//		for (int i = 0; i < paramNames.length; i++) {
//			paramArrays[i] = new double[arraySize];// e.g. traveling,
//			// performing
//		}
//
//		Config config = ctl.getConfig();
//
//		String avgLlhOverItersStr = config.findParam(BSE_CONFIG_MODULE_NAME,
//				"averageLogLikelihoodOverIterations");
//		if (avgLlhOverItersStr != null) {
//			avgLlhOverIters = Integer.parseInt(avgLlhOverItersStr);
//		}
//
//		String writeLlhItervalStr = config.findParam(BSE_CONFIG_MODULE_NAME,
//				"writeLogLikelihoodInterval");
//		if (writeLlhItervalStr != null) {
//			writeLlhInterval = Integer.parseInt(writeLlhItervalStr);
//		}
//		// writeLinkUtilOffsetsInterval
//		String writeLinkUtilOffsetsIntervalStr = config.findParam(
//				BSE_CONFIG_MODULE_NAME, "writeLinkUtilOffsetsInterval");
//		if (writeLinkUtilOffsetsIntervalStr != null) {
//			writeLinkUtilOffsetsInterval = Integer
//					.parseInt(writeLinkUtilOffsetsIntervalStr);
//		}
//	}
//
//	private void initializeStrategyManager(Controler ctl) {
//		String deltaStr = ctl.getConfig().findParam(BSE_CONFIG_MODULE_NAME,
//				"delta");
//		double delta;
//		if (deltaStr == null) {
//			delta = 1e-6;
//			System.out.println("BSE:\tdelta\t= default value\t" + delta);
//		} else {
//			delta = Double.parseDouble(deltaStr);
//			System.out.println("BSE:\tdelta\t=\t" + delta);
//		}
//		((PCStrMn) ctl.getStrategyManager())
//				.init(calibrator, ctl.getTravelTimeCalculator(),
//						(MultinomialLogitChoice) chooser);
//	}
//
//	private void loadScoringAttributes(String scorAttrFilename,
//			Population population) {
//		new ScorAttrReader(scorAttrFilename, population).parser();
//
//	}
//
//	@Override
//	public void notifyIterationEnds(IterationEndsEvent event) {
//		PCCtlwithLeftTurnPenalty ctl = (PCCtlwithLeftTurnPenalty) event
//				.getControler();
//		Config config = ctl.getConfig();
//		int iter = event.getIteration();
//		int firstIter = ctl.getFirstIteration();
//		// this.chooser.finish();-->called in notifyScoring()
//		PCStrMn strategyManager = (PCStrMn) ctl.getStrategyManager();
//
//		// if (iter - firstIter > strategyManager.getMaxPlansPerAgent()) {
//		OutputDirectoryHierarchy io = ctl.getControlerIO();
//		// ***************************************************
//		calibrator.setFlowAnalysisFile(io.getIterationFilename(iter,
//				"flowAnalysis.log"));
//		calibrator.afterNetworkLoading(resultsContainer);
//		// ************************************************
//		if (iter % writeLinkUtilOffsetsInterval == 0) {
//			try {
//				DynamicData<Link> linkCostOffsets = calibrator
//						.getLinkCostOffsets();
//				new BseLinkCostOffsetsXMLFileIO(ctl.getNetwork()).write(
//						io.getIterationFilename(iter, "linkUtilOffsets.xml"),
//						linkCostOffsets);
//
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		// ************************************************
//		if (calibrator.getParameterCovarianceExpOfVarComponent()
//		// getParameterCovariance()
//		!= null) {
//			writerCV.writeln(iter
//					// + "\t"
//					// + calibrator.getParameterCovariance()
//					// .toSingleLineString()
//					+ "\t"
//					+ calibrator.getParameterCovarianceExpOfVarComponent()
//							.toSingleLineString()
//			// + "\t"
//			// + calibrator.getParameterCovarianceVarOfExpComponent()
//			// .toSingleLineString()
//			);
//		}
//		writerCV.flush();
//
//		// ###################################################
//		utilities.math.Vector avgParams = calibrator.getAvgParameters();
//		PlanCalcScoreConfigGroup scoringCfg = config.planCalcScore();
//
//		// ScoringConfigGetSetValues.setConfig(config);
//		// VERY IMPORTANT #########################################
//
//		// if (
//		// // !watching &&
//		// cycleIdx == 0) {
//		MultinomialLogit mnl = ((MultinomialLogitChoice) chooser)
//				.getMultinomialLogit();
//
//		// *******should after Scoring Listener!!!*******
//
//		if (calibrator.getInitialStepSize() != 0d) {
//
//			StringBuffer sb = new StringBuffer(Integer.toString(iter));
//
//			for (int i = 0; i < paramNames.length; i++) {
//				int paramNameIndex = Events2Score4AttrRecorder.attrNameList
//						.indexOf(paramNames[i]/*
//											 * pos. of param in Parameters in
//											 * Cadyts
//											 */);
//
//				// double paramScaleFactor =
//				// Events2Score4PC_mnl_mnl.paramScaleFactorList
//				// .get(paramNameIndex);
//
//				double value = avgParams.get(i);
//				// ****SET CALIBRATED PARAMETERS FOR SCORE CALCULATION
//				// AGAIN!!!***
//				if (scoringCfg.getParams().containsKey(paramNames[i])) {
//					scoringCfg.addParam(paramNames[i], Double.toString(value
//					// / paramScaleFactor
//							));
//					// ScoringConfigGetSetValues
//					// .setValue(paramNames[i], value);
//				} else/* bse */{
//					config.setParam(BSE_CONFIG_MODULE_NAME, paramNames[i],
//							Double.toString(value
//							// / paramScaleFactor
//							));
//				}
//				// *****************************************************
//				// ****SET CALIBRATED PARAMETERS IN MNL*****************
//				mnl.setParameter(paramNameIndex, value);
//
//				// text output
//				paramArrays[i][iter - firstIter] = value;
//				sb.append("\t");
//				sb.append(value);
//			}
//
//			// ********calibrator.getParameters() JUST FOR
//			// ANALYSIS********
//			utilities.math.Vector params = calibrator.getParameters();
//			for (int i1 = 0; i1 < paramNames.length; i1++) {
//				sb.append("\t");
//				sb.append(params.get(i1));
//			}
//
//			writer.writeln(sb);
//			writer.flush();
//		}
//		/*-----------------initialStepSize==0, no parameters are changed----------------------*/
//
//		((Events2ScoreWithLeftTurnPenalty4PC) chooser).setMultinomialLogit(mnl);
//
//		CharyparNagelScoringFunctionFactoryWithLeftTurnPenalty sfFactory = new CharyparNagelScoringFunctionFactoryWithLeftTurnPenalty(
//				config, ctl.getNetwork());
//		ctl.setScoringFunctionFactory(sfFactory);
//		((Events2ScoreWithLeftTurnPenalty4PC) chooser).setSfFactory(sfFactory);
//
//		strategyManager.setChooser(chooser);
//
//		// }
//		// cycleIdx++;
//		// if (cycleIdx == cycle) {
//		// cycleIdx = 0;
//		// }
//		// }
//
//		// TESTS: calculate log-likelihood -(q-y)^2/(2sigma^2)
//		if (writeLlhInterval > 0 && avgLlhOverIters > 0) {
//			int nextWriteLlhInterval = writeLlhInterval
//					* (iter / writeLlhInterval + 1);
//			if (iter <= nextWriteLlhInterval
//					&& iter > nextWriteLlhInterval - avgLlhOverIters
//					|| iter % writeLlhInterval == 0) {
//				Network network = ctl.getNetwork();
//
//				for (Map.Entry<Id, Count> entry : ctl.getCounts().getCounts()
//						.entrySet()) {
//					Link link = network.getLinks().get(entry.getKey());
//					if (link == null) {
//						System.err.println("could not find link "
//								+ entry.getKey().toString());
//					} else if (isInRange(entry.getKey(), network)) {
//						// ---------GUNNAR'S CODES---------------------
//						for (Volume volume : entry.getValue().getVolumes()
//								.values()) {
//							int hour = volume.getHourOfDayStartingWithOne();
//							if (hour >= caliStartTime && hour <= caliEndTime) {
//								// int start_s = (hour - 1) * 3600;
//								// int end_s = hour * 3600 - 1;
//								double cntVal = volume.getValue();
//
//								int[] linkVols = volumes.getVolumesForLink(link
//										.getId());
//								double simVal = 0d;
//								if (linkVols != null) {
//									simVal = linkVols[hour - 1]
//											* countsScaleFactor;
//								}
//
//								double minstddev = calibrator
//										.getMinStddev(TYPE.FLOW_VEH_H);
//								double var = Math.max(minstddev * minstddev,
//										calibrator.getVarianceScale() * cntVal);
//								double absLlh = (simVal - cntVal)
//										* (simVal - cntVal) / 2d / var;
//								llhSum -= absLlh;
//								// System.out.println("Accumulated Llh over "
//								// + avgLlhOverIters
//								// + " iterations at it." + iter + " =\t"
//								// + llhSum + "\tadded llh =\t-" + absLlh);
//							}
//						}
//					}
//				}
//			}
//			if (iter % writeLlhInterval == 0) {
//				// calculate avg. value of llh
//				double avgLlh = llhSum / avgLlhOverIters;
//				System.out.println("avgLlh over " + avgLlhOverIters
//						+ " iterations at it." + iter + " =\t" + avgLlh);
//				llhSum = 0d;// refresh
//			}
//		}
//		// output - chart etc.
//		String halfwayOutputIntervalStr = config.findParam(
//				BSE_CONFIG_MODULE_NAME, "halfwayOutputInterval");
//		int halfwayOutputInterval = 0;
//		if (halfwayOutputIntervalStr != null) {
//			halfwayOutputInterval = Integer.parseInt(halfwayOutputIntervalStr);
//		}
//		outputHalfway(ctl, halfwayOutputInterval);
//	}
//
//	@Override
//	public void notifyShutdown(ShutdownEvent event) {
//		writer.close();
//		writerCV.close();
//
//		Controler ctl = event.getControler();
//		OutputDirectoryHierarchy ctlIO = ctl.getControlerIO();
//		int firstIter = ctl.getFirstIteration(), lastIter = ctl
//				.getLastIteration();
//
//		double[] xs = new double[lastIter - firstIter + 1];
//		for (int i = firstIter; i <= lastIter; i++) {
//			xs[i - firstIter] = i;
//		}
//
//		String chartFilename = "parameters_";
//		for (int i = 0; i < paramNames.length; i++) {
//			chart.addSeries(paramNames[i], xs, paramArrays[i]);
//			chartFilename += paramNames[i] + ".";
//		}
//
//		chart.saveAsPng(ctlIO.getOutputFilename(chartFilename + "png"), 1024,
//				768);
//
//		// this.travPerfChart.saveAsPng(ctlIO
//		// .getOutputFilename("statistics-travPerf.png"), 1024, 768);
//	}
//
//	@Override
//	public void notifyStartup(final StartupEvent event) {
//		final PCCtlwithLeftTurnPenalty ctl = (PCCtlwithLeftTurnPenalty) event
//				.getControler();
//		Config config = ctl.getConfig();
//
//		setMatsimParameters(ctl);
//
//		String scorAttrFilename = config.findParam(BSE_CONFIG_MODULE_NAME,
//				"scorAttrFilename");
//		if (scorAttrFilename != null) {
//			loadScoringAttributes(scorAttrFilename, ctl.getPopulation());
//			// ctl.getScenario().setPopulation(population2);
//			System.out
//					.println("ENDING of loading scoring function attributes.");
//		}
//
//		initializeCalibrator(ctl);
//
//		// *********SETTING PARAMETERS FOR "PARAMETER CALIBRATION"****
//		setInitialParametersInCalibrator(config);
//		setInitialParameterVariancesInCalibrator(config);
//
//		// *******************SETTING CALIBRATOR********************
//		setCalibratorParameters(config);
//
//		// ***************SETTING PARAMETERS FOR INTEGRATION***********
//		// SETTING countsScale
//		{
//			countsScaleFactor = config.counts().getCountsScaleFactor();
//			System.out.println("BSE:\tusing the countsScaleFactor of "
//					+ countsScaleFactor + " as packetSize from config.");
//		}
//		// cycle = Integer.parseInt(config.findParam(BSE_CONFIG_MODULE_NAME,
//		// "cycle"));
//
//		// READING countsdata
//		readCounts(ctl);
//
//		// INITIALIZING chooser
//		chooser = ctl.getPlansScoring4PC().getPlanScorer();
//
//		// INITIALIZING StrategyManager
//		initializeStrategyManager(ctl);
//		// ******************************************************************
//
//		// INITIALIZING resultContainer
//		resultsContainer = new SimResultsContainerImpl();
//
//		// INITIALIZING OUTPUT
//		initializeOutput(ctl);
//	}
//
//	private void outputHalfway(Controler ctl, int outputIterInterval) {
//		int iter = ctl.getIterationNumber();
//		if (outputIterInterval == 0) {
//			return;
//		}
//		if (iter % outputIterInterval == 0) {
//			OutputDirectoryHierarchy ctlIO = ctl.getControlerIO();
//			int firstIter = ctl.getFirstIteration();
//
//			double[] xs = new double[iter - firstIter + 1];
//			for (int i = firstIter; i <= iter; i++) {
//				xs[i - firstIter] = i;
//			}
//
//			String chartTitle = "Calibrated Parameter(s):";
//			for (String paramName : paramNames) {
//				chartTitle += " " + paramName;
//			}
//
//			XYLineChart chart = new XYLineChart(chartTitle, "iter",
//					"value of parameters") // ,travPerf = new
//			// XYLineChart("traveling & performing - avg. and sqrt(var)",
//			// "iteration","[h]")
//			;
//			String chartFilename = "";
//			for (int i = 0; i < paramNames.length; i++) {
//				chart.addSeries(paramNames[i], xs, paramArrays[i]);
//				chartFilename += paramNames[i] + ".";
//			}
//
//			chart.saveAsPng(
//					ctlIO.getIterationFilename(iter, chartFilename + "png"),
//					1024, 768);
//
//			// travPerf.saveAsPng(ctlIO.getIterationFilename(iter,
//			// "travTravPt.png"), 1024, 768);
//		}
//	}
//
//	private void readCounts(Controler ctl) {
//		final Counts counts = ctl.getCounts();
//		final Network network = ctl.getNetwork();
//		final Config config = ctl.getConfig();
//
//		if (counts == null) {
//			throw new RuntimeException("BSE requires counts-data.");
//		}
//
//		String caliStartTimeStr = config.findParam(BSE_CONFIG_MODULE_NAME,
//				"startTime");
//
//		caliStartTime = caliStartTimeStr != null ? Integer
//				.parseInt(caliStartTimeStr) : DEFAULT_CALIBRATION_START_TIME;
//
//		caliEndTime = Integer.parseInt(config.findParam(BSE_CONFIG_MODULE_NAME,
//				"endTime"));
//
//		Map<String/* countId */, Map<Integer/* timeStep */, Double/* sample variance */>> countsSampleVariances = null;
//		String countsSampleVarianceFilename = config.findParam(
//				BSE_CONFIG_MODULE_NAME, "countsSampleVarianceFile");
//		if (countsSampleVarianceFilename != null) {
//			SampleVarianceReader svReader = new SampleVarianceReader(
//					countsSampleVarianceFilename);
//			svReader.read();
//			countsSampleVariances = svReader.getCountsSampleVariances();
//		} else {
//			Logger.getLogger(this.getClass().getName())
//					.warning(
//							"BSE:\tThere is not countsSampleVarianceFilename set in configfile, this means, the variance for each count will be replaced with the count value by using poisson distribution in substitution for gauss distribution.");
//		}
//		Map<Id, Count> countsMap = counts.getCounts();
//		for (Id countId : countsMap.keySet()) {
//			Link link = network.getLinks().get(countId);
//			if (link == null) {
//				System.err.println("could not find link " + countId.toString());
//			} else if (isInRange(countId, network)) {
//
//				// for ...2QGIS
//				links.add(network.getLinks().get(countId));
//				linkIds.add(countId);
//				// ---------GUNNAR'S CODES---------------------
//				for (Volume volume : countsMap.get(countId).getVolumes()
//						.values()) {
//					int hour = volume.getHourOfDayStartingWithOne();
//					if (hour >= caliStartTime && hour <= caliEndTime) {
//						int start_s = (hour - 1) * 3600;
//						int end_s = hour * 3600 - 1;
//						double val_veh_h = volume.getValue();
//						// -------------------------------------------------
//						// calibrator.addMeasurement(link, start_s, end_s,
//						// val_veh_h, TYPE.FLOW_VEH_H);
//						final double stddev = max(
//								calibrator.getMinStddev(TYPE.FLOW_VEH_H), sqrt(
//								// this variance should be read from other
//								// resource with index countId \t hour ...
//								countsSampleVariances != null ? countsSampleVariances
//										.get(countId.toString()).get(hour)
//										: calibrator.getVarianceScale()
//												* val_veh_h));
//						calibrator.addMeasurement(link, start_s, end_s,
//								val_veh_h, stddev, TYPE.FLOW_VEH_H);
//						// -------------------------------------------------
//					}
//				}
//			}
//		}
//	}
//
//	private void setCalibratorParameters(Config config) {
//		// SETTING setUseApproximateNewton
//		// {
//		// String useApproximateNewtonStr = config.findParam(
//		// BSE_CONFIG_MODULE_NAME, "useApproximateNewton");
//		// if (useApproximateNewtonStr != null) {
//		// boolean useApproximateNewton = Boolean
//		// .parseBoolean(useApproximateNewtonStr);
//		// calibrator.setUseApproximateNetwton(useApproximateNewton);
//		// System.out.println("BSE:\tuseApproximateNetwton\t=\t"
//		// + useApproximateNewton);
//		// } else {
//		// System.out
//		// .println("BSE:\tuseApproximateNetwton\t= default value\t"
//		// + ChoiceParameterCalibrator4.DEFAULT_USE_APPROXIMATE_NEWTON);
//		// }
//		// }
//
//		// SETTING proportionalAssignment
//		{
//			String proportionalAssignmentStr = config.findParam(
//					BSE_CONFIG_MODULE_NAME, "proportionalAssignment");
//			if (proportionalAssignmentStr != null) {
//				boolean proportionalAssignment = Boolean
//						.parseBoolean(proportionalAssignmentStr);
//				System.out.println("BSE:\tproportionalAssignment\t= "
//						+ proportionalAssignment);
//				calibrator.setProportionalAssignment(proportionalAssignment);
//			} else {
//				System.out
//						.println("BSE:\tproportionalAssignment\t= default value\t"
//								+ Calibrator.DEFAULT_PROPORTIONAL_ASSIGNMENT);
//			}
//		}
//
//		// SETTING FREEZE ITERATION
//		{
//			final String freezeIterationStr = config.findParam(
//					BSE_CONFIG_MODULE_NAME, "freezeIteration");
//			if (freezeIterationStr != null) {
//				final int freezeIteration = Integer
//						.parseInt(freezeIterationStr);
//				System.out.println("BSE:\tfreezeIteration\t= "
//						+ freezeIteration);
//				calibrator.setFreezeIteration(freezeIteration);
//			} else {
//				System.out.println("BSE:\tfreezeIteration\t= default value\t"
//						+ Calibrator.DEFAULT_FREEZE_ITERATION);
//			}
//		}
//
//		// SETTING MINSTDDEV
//		{
//			String minStdDevStr = config.findParam(BSE_CONFIG_MODULE_NAME,
//					"minFlowStddevVehH");
//			if (minStdDevStr != null) {
//				double minStdDev = Double.parseDouble(minStdDevStr);
//				calibrator.setMinStddev(minStdDev, TYPE.FLOW_VEH_H);
//				System.out.println("BSE:\tminStdDev\t= " + minStdDev);
//			} else {
//				System.out.println("BSE:\tminStdDev\t= default value\t"
//						+ Calibrator.DEFAULT_MIN_FLOW_STDDEV_VEH_H);
//			}
//		}
//		// SETTING Preparatory Iterations
//		{
//			final String preparatoryIterationsStr = config.findParam(
//					BSE_CONFIG_MODULE_NAME, "preparatoryIterations");
//			if (preparatoryIterationsStr != null) {
//				final int preparatoryIterations = Integer
//						.parseInt(preparatoryIterationsStr);
//				System.out.println("BSE:\tpreparatoryIterations\t= "
//						+ preparatoryIterations);
//				calibrator.setPreparatoryIterations(preparatoryIterations);
//			} else {
//				System.out
//						.println("BSE:\tpreparatoryIterations\t= default value\t"
//								+ Calibrator.DEFAULT_PREPARATORY_ITERATIONS);
//			}
//		}
//		// SETTING varianceScale
//		{
//			final String varianceScaleStr = config.findParam(
//					BSE_CONFIG_MODULE_NAME, "varianceScale");
//			if (varianceScaleStr != null) {
//				final double varianceScale = Double
//						.parseDouble(varianceScaleStr);
//				System.out.println("BSE:\tvarianceScale\t= " + varianceScale);
//				calibrator.setVarianceScale(varianceScale);
//			} else {
//				System.out.println("BSE:\tvarianceScale\t= default value\t"
//						+ Calibrator.DEFAULT_VARIANCE_SCALE);
//			}
//		}
//
//		// SETTING parameterStepSize
//		// {
//		// String initialStepSizeStr = config.findParam(
//		// BSE_CONFIG_MODULE_NAME, "initialStepSize");
//		// System.out.print("BSE:\tparameterStepSize\t= ");
//		// if (initialStepSizeStr != null) {
//		// double initialStepSize = Double.parseDouble(initialStepSizeStr);
//		// System.out.println(initialStepSize);
//		// calibrator.setInitialStepSize(initialStepSize);
//		// } else {
//		// System.out.println("default value\t"
//		// + ChoiceParameterCalibrator4.DEFAULT_INITIAL_STEP_SIZE);
//		// }
//		// }
//
//		// SETTING msaExponent deprecated
//		// {
//		// /*
//		// * initialStepSize * 1.0 /(iteration ^ msaExponent), if msaExponent
//		// * == 0.5, the step won't so quickly smaller, default value: 1.0
//		// */
//		// String msaExponentStr = config.findParam(BSE_CONFIG_MODULE_NAME,
//		// "msaExponent");
//		// if (msaExponentStr != null) {
//		// double msaExponent = Double.parseDouble(msaExponentStr);
//		// calibrator.setMsaExponent(msaExponent);
//		// System.out.println("BSE:\tmsaExponent\t=" + msaExponent);
//		// } else {
//		// System.out.println("BSE:\tmsaExponent\t= default value\t"
//		// + ChoiceParameterCalibrator4.DEFAULT_MSA_EXPONENT);
//		// }
//		// }
//		// SETTING parameterUpdateInterval
//		{
//			String parameterUpdateIntervalStr = config.findParam(
//					BSE_CONFIG_MODULE_NAME, "parameterUpdateInterval");
//			if (parameterUpdateIntervalStr != null) {
//				int parameterUpdateInterval = Integer
//						.parseInt(parameterUpdateIntervalStr);
//				calibrator.setParameterUpdateInterval(parameterUpdateInterval);
//				System.out.println("BSE:\tparameterUpdateInterval\t="
//						+ parameterUpdateInterval);
//			} else {
//				System.out
//						.println("BSE:\tparameterUpdateInterval\t= default value\t"
//								+ ChoiceParameterCalibrator4.DEFAULT_PARAMETER_UPDATE_INTERVAL);
//			}
//		}
//	}
//
//	private void setInitialParametersInCalibrator(Config config) {
//		ScoringConfigGetSetValues.setConfig(config);
//		PlanCalcScoreConfigGroup scoringCfg = config.planCalcScore();
//
//		Vector initialParams = new Vector(paramDim);
//		for (int i = 0; i < initialParams.size(); i++) {
//			// double paramScaleFactor =
//			// Events2Score4PC_mnl_mnl.paramScaleFactorList
//			// .get(Events2Score4PC_mnl_mnl.attrNameList.indexOf(paramNames[i]));
//
//			if (scoringCfg.getParams().containsKey(paramNames[i])) {
//
//				initialParams.set(i, Double
//						.parseDouble(ScoringConfigGetSetValues
//								.getValue(paramNames[i]))
//				// * paramScaleFactor
//						);
//
//			} else/* bse */{
//				String paramStr = config.findParam(BSE_CONFIG_MODULE_NAME,
//						paramNames[i]);
//				initialParams.set(i,
//						paramStr != null ? Double.parseDouble(paramStr) : 0d)
//				// * paramScaleFactor
//				;
//			}
//		}
//
//		calibrator.setInitialParameters(initialParams);// without ASC
//	}
//
//	private void setInitialParameterVariancesInCalibrator(Config config) {
//		String setInitialParamVarStr = config.findParam(BSE_CONFIG_MODULE_NAME,
//				"setInitialParameterVariances");
//		if (setInitialParamVarStr != null) {
//			if (Boolean.parseBoolean(setInitialParamVarStr)) {
//				Vector initialParamVars = new Vector(paramDim);
//				// =2 without ASC
//				for (int i = 0; i < paramDim; i++) {
//					initialParamVars.set(i, Double.parseDouble(config
//							.findParam(BSE_CONFIG_MODULE_NAME,
//									PARAM_STDDEV_INDEX + i)));// [traveling,performing]
//				}
//
//				calibrator.setInitialParameterVariances(initialParamVars);
//			}
//		} else {
//			Logger.getLogger("BSE:\tNo setting of setInitialParameterVariances, so it is accepted as FALSE");
//		}
//	}
//
//	// private ChoiceParameterCalibrator3<Link> calibrator = null;
//	private void setMatsimParameters(Controler ctl) {
//		Config config = ctl.getConfig();
//
//		CountsConfigGroup ccg = config.counts();
//		String distFilterCenterNodeStr = ccg.getDistanceFilterCenterNode();
//		if (distFilterCenterNodeStr != null) {
//			// set up center and radius of counts stations locations
//			distanceFilterCenterNodeCoord = ctl.getNetwork().getNodes()
//					.get(new IdImpl(distFilterCenterNodeStr)).getCoord();
//			distanceFilter = ccg.getDistanceFilter();
//		}
//		// set up volumes analyzer
//		volumes = ctl.getVolumes();
//		/*
//		 * volumes = new VolumesAnalyzer(3600, 30 * 3600,
//		 * network);ctl.getEvents().addHandler(volumes);
//		 */
//	}
//
//	public void setWriteQGISFile(boolean writeQGISFile) {
//		this.writeQGISFile = writeQGISFile;
//	}
//}
