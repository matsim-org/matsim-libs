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
//package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.pseudo;
//
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.api.core.v01.population.Plan;
//import org.matsim.api.core.v01.population.Population;
//import org.matsim.core.basic.v01.IdImpl;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.groups.CountsConfigGroup;
//import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
//import org.matsim.core.controler.Controler;
//import org.matsim.core.controler.OutputDirectoryHierarchy;
//import org.matsim.core.controler.events.BeforeMobsimEvent;
//import org.matsim.core.controler.events.IterationEndsEvent;
//import org.matsim.core.controler.events.ShutdownEvent;
//import org.matsim.core.controler.events.StartupEvent;
//import org.matsim.core.controler.listener.BeforeMobsimListener;
//import org.matsim.core.controler.listener.IterationEndsListener;
//import org.matsim.core.controler.listener.ShutdownListener;
//import org.matsim.core.controler.listener.StartupListener;
//import org.matsim.core.gbl.MatsimRandom;
//import org.matsim.core.population.PlanImpl;
//import org.matsim.core.utils.charts.XYLineChart;
//import org.matsim.counts.Count;
//import org.matsim.counts.Counts;
//import org.matsim.counts.Volume;
//
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.PlanToPlanStep;
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.mnlValidation.MultinomialLogitChoice;
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.parametersCorrection.BseParamCalibrationControlerListener;
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.scoring.ScoringConfigGetSetValues;
//import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.testLeftTurn.Events2ScoreWithLeftTurnPenalty4PC;
//import playground.yu.scoring.withAttrRecorder.Events2Score4AttrRecorder;
//import playground.yu.scoring.withAttrRecorder.ScorAttrReader;
//import playground.yu.scoring.withAttrRecorder.leftTurn.CharyparNagelScoringFunctionFactoryWithLeftTurnPenalty;
//import playground.yu.utils.io.SimpleWriter;
//import utilities.math.Vector;
//import cadyts.calibrators.Calibrator;
//import cadyts.demand.PlanStep;
//import cadyts.interfaces.matsim.MATSimChoiceParameterCalibrator;
//import cadyts.measurements.SingleLinkMeasurement.TYPE;
//
//public class PCCtlListener extends BseParamCalibrationControlerListener
//		implements StartupListener, ShutdownListener, IterationEndsListener,
//		BeforeMobsimListener {
//	private int caliStartTime, caliEndTime;
//	// private int avgLlhOverIters = 0, writeLlhInterval = 0;
//
//	private SimpleWriter writer = null;
//	// private static List<Link> links = new ArrayList<Link>();
//	// private static Set<Id> linkIds = new HashSet<Id>();
//	private XYLineChart chart;// paramChart
//
//	static int paramDim;
//
//	static final String PARAM_NAME_INDEX = "parameterName_",
//			PARAM_STDDEV_INDEX = "paramStddev_";
//	static String[] paramNames/* in configfile */;
//
//	private double[][] paramArrays/* performing, traveling and so on */;
//
//	private PlanToPlanStep planConverter = null;
//	private Counts counts;
//	private static int countTimeBin = 3600;
//	private Network network = null;
//	private double minStdDev, varianceScale;
//
//	// private final boolean writeQGISFile = true;
//
//	private void initializeCalibrator(Controler ctl) {
//		Config config = ctl.getConfig();
//		OutputDirectoryHierarchy ctlIO = ctl.getControlerIO();
//
//		// SETTING "parameter calibration" parameters
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
//		final String timeBinStr = config.findParam(BSE_CONFIG_MODULE_NAME,
//				"timeBinSize_s");
//		if (timeBinStr != null) {
//			countTimeBin = Integer.parseInt(timeBinStr);
//		}
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
//
//		String caliStartTimeStr = config.findParam(BSE_CONFIG_MODULE_NAME,
//				"startTime");
//		caliStartTime = caliStartTimeStr != null ? Integer
//				.parseInt(caliStartTimeStr) : DEFAULT_CALIBRATION_START_TIME;
//
//		String caliEndTimeStr = config.findParam(BSE_CONFIG_MODULE_NAME,
//				"endTime");
//		caliEndTime = caliEndTimeStr != null ? Integer.parseInt(caliEndTimeStr)
//				: DEFAULT_CALIBRATION_END_TIME;
//	}
//
//	private void initializeOutput(Controler ctl) {
//		OutputDirectoryHierarchy ctlIO = ctl.getControlerIO();
//		{
//			writer = new SimpleWriter(ctlIO.getOutputFilename("parameters.log"));
//			StringBuffer sb = new StringBuffer("iter");
//			for (int i = 0; i < paramNames.length; i++) {
//				sb.append("\t");
//				sb.append(paramNames[i]);
//
//			}
//			writer.writeln(sb);
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
//
//		((PCStrMn) ctl.getStrategyManager()).init(this,
//				(MultinomialLogitChoice) chooser);
//	}
//
//	double getUtilityCorrection(Plan plan) {
//		double uc = 0d;
//		planConverter.convert((PlanImpl) plan);
//
//		for (Iterator<PlanStep<Link>> planStepIt = planConverter.getPlanSteps()
//				.iterator(); planStepIt.hasNext();) {
//			PlanStep<Link> planStep = planStepIt.next();
//			Id linkId = planStep.getLink().getId();
//			Count count = counts.getCount(linkId);
//			if (count != null) {
//				int entryTime_s = planStep.getEntryTime_s();
//
//				int hour = entryTime_s / countTimeBin + 1;
//				if (hour >= caliStartTime && hour <= caliEndTime) {
//
//					Volume vol = count.getVolume(hour);
//
//					double[] avgVols = volumes.getVolumesPerHourForLink(linkId);
//					if (avgVols == null) {
//						avgVols = new double[24];
//					}
//
//					if (vol != null) {
//						double countVal = vol.getValue();
//						if (countVal != 0d/* zeroCount */) {
//
//							double simVal = resultsContainer.getSimValue(
//									network.getLinks().get(linkId),
//									entryTime_s, entryTime_s + 3599,
//									TYPE.FLOW_VEH_H);
//
//							simVal = avgVols[hour - 1] * countsScaleFactor;
//							uc += (countVal - simVal)
//									/ Math.max(minStdDev * minStdDev,
//											varianceScale * countVal);
//							// System.out.println("uc =\t"+uc);
//						}
//					}
//
//				}
//
//			}
//		}
//
//		return uc;
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
//		CtlWithLeftTurnPenaltyLs ctl = (CtlWithLeftTurnPenaltyLs) event
//				.getControler();
//		Config config = ctl.getConfig();
//		int iter = event.getIteration();
//		int firstIter = ctl.getFirstIteration();
//		OutputDirectoryHierarchy io = ctl.getControlerIO();
//		// ***************************************************
//		calibrator.setFlowAnalysisFile(io.getIterationFilename(iter,
//				"flowAnalysis.log"));
//
//		PlanCalcScoreConfigGroup scoringCfg = config.planCalcScore();
//
//		// if (calibrator.getInitialStepSize() != 0d) {
//
//		StringBuffer sb = new StringBuffer(Integer.toString(iter));
//
//		for (int i = 0; i < paramNames.length; i++) {
//
//			double value;
//			// ****SET CALIBRATED PARAMETERS FOR SCORE CALCULATION
//			// AGAIN!!!***
//			String valStr = scoringCfg.getParams().get(paramNames[i]);
//			if (valStr == null) {
//				valStr = config
//						.findParam(BSE_CONFIG_MODULE_NAME, paramNames[i]);
//				if (valStr == null) {
//					throw new RuntimeException(
//							"The calibrated parameter can NOT be found");
//				}
//			}
//			value = Double.parseDouble(valStr);
//
//			// text output
//			paramArrays[i][iter - firstIter] = value;
//			sb.append("\t");
//			sb.append(value);
//		}
//
//		writer.writeln(sb);
//		writer.flush();
//		// }
//	}
//
//	@Override
//	public void notifyShutdown(ShutdownEvent event) {
//		writer.close();
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
//		final CtlWithLeftTurnPenaltyLs ctl = (CtlWithLeftTurnPenaltyLs) event
//				.getControler();
//		Config config = ctl.getConfig();
//		network = ctl.getNetwork();
//		counts = ctl.getCounts();
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
//		countsScaleFactor = config.counts().getCountsScaleFactor();
//		System.out.println("BSE:\tusing the countsScaleFactor of "
//				+ countsScaleFactor + " as packetSize from config.");
//
//		// INITIALIZING chooser
//		chooser = ctl.getPlansScoring4PC().getPlanScorer();
//
//		// INITIALIZING StrategyManager
//		initializeStrategyManager(ctl);
//
//		planConverter = new PlanToPlanStep(ctl.getTravelTimeCalculator(),
//				network);
//		// ******************************************************************
//
//		// INITIALIZING resultContainer
//		resultsContainer = new SimResultsContainerImpl();
//
//		// INITIALIZING OUTPUT
//		initializeOutput(ctl);
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
//				minStdDev = Double.parseDouble(minStdDevStr);
//				calibrator.setMinStddev(minStdDev, TYPE.FLOW_VEH_H);
//				System.out.println("BSE:\tminStdDev\t= " + minStdDev);
//			} else {
//				minStdDev = Calibrator.DEFAULT_MIN_FLOW_STDDEV_VEH_H;
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
//				varianceScale = Double.parseDouble(varianceScaleStr);
//				System.out.println("BSE:\tvarianceScale\t= " + varianceScale);
//				calibrator.setVarianceScale(varianceScale);
//			} else {
//				varianceScale = Calibrator.DEFAULT_VARIANCE_SCALE;
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
//		// {
//		// String parameterUpdateIntervalStr = config.findParam(
//		// BSE_CONFIG_MODULE_NAME, "parameterUpdateInterval");
//		// if (parameterUpdateIntervalStr != null) {
//		// int parameterUpdateInterval = Integer
//		// .parseInt(parameterUpdateIntervalStr);
//		// calibrator.setParameterUpdateInterval(parameterUpdateInterval);
//		// System.out.println("BSE:\tparameterUpdateInterval\t="
//		// + parameterUpdateInterval);
//		// } else {
//		// System.out
//		// .println("BSE:\tparameterUpdateInterval\t= default value\t"
//		// + ChoiceParameterCalibrator4.DEFAULT_PARAMETER_UPDATE_INTERVAL);
//		// }
//		// }
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
//			distanceFilterCenterNodeCoord = network.getNodes()
//					.get(new IdImpl(distFilterCenterNodeStr)).getCoord();
//			distanceFilter = ccg.getDistanceFilter();
//		}
//		// set up volumes analyzer
//		volumes = ctl.getVolumes();
//	}
//
//	@Override
//	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
//		Controler ctl = event.getControler();
//
//		CharyparNagelScoringFunctionFactoryWithLeftTurnPenalty sfFactory = new CharyparNagelScoringFunctionFactoryWithLeftTurnPenalty(
//				ctl.getConfig(), network);
//		ctl.setScoringFunctionFactory(sfFactory);
//		((Events2ScoreWithLeftTurnPenalty4PC) chooser).setSfFactory(sfFactory);
//
//		((PCStrMn) ctl.getStrategyManager()).setChooser(chooser);
//	}
//}
