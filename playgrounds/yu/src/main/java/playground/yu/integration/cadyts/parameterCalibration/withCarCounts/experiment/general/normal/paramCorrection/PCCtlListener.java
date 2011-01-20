/* *********************************************************************** *
 * project: org.matsim.*
 * BseControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.general.normal.paramCorrection;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.general.normal.scoring.Events2Score4PC;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.general.normal.scoring.Events2Score4PC_mnl;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.general.normal.withLegModeASC.CharyparNagelScoringFunctionFactory4PC;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.mnlValidation.MultinomialLogitChoice;
import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.scoring.ScoringConfigGetValue;
import playground.yu.utils.io.SimpleWriter;
import cadyts.calibrators.Calibrator;
import cadyts.calibrators.analytical.ChoiceParameterCalibrator;
import cadyts.interfaces.matsim.MATSimChoiceParameterCalibrator;
import cadyts.measurements.SingleLinkMeasurement.TYPE;
import cadyts.utilities.math.MultinomialLogit;
import cadyts.utilities.math.Vector;

public class PCCtlListener extends BseParamCalibrationControlerListener
		implements StartupListener, ShutdownListener, IterationEndsListener {
	// private Config config;
	private int cycleIdx = 0, cycle;
	private SimpleWriter writer = null, writerCV = null;
	// private static List<Link> links = new ArrayList<Link>();
	// private static Set<Id> linkIds = new HashSet<Id>();
	private XYLineChart chart;// paramChart

	private int paramDim;

	static final String PARAM_NAME_INDEX = "parameterName_",
			PARAM_STDDEV_INDEX = "paramStddev_";
	static String[] paramNames/* in configfile */;

	private double[][] paramArrays/* performing, traveling and so on */;

	// private ChoiceParameterCalibrator2<Link> calibrator = null;
	private void setMatsimParameters(Controler ctl) {
		final Network network = ctl.getNetwork();
		Config config = ctl.getConfig();

		// set up center and radius of counts stations locations
		distanceFilterCenterNodeCoord = network.getNodes().get(
				new IdImpl(config.counts().getDistanceFilterCenterNode()))
				.getCoord();
		distanceFilter = config.counts().getDistanceFilter();

		// set up volumes analyzer
		volumes = new VolumesAnalyzer(3600, 30 * 3600, network);
		ctl.getEvents().addHandler(volumes);
	}

	private void initializeCalibrator(Controler ctl) {
		Config config = ctl.getConfig();
		ControlerIO ctlIO = ctl.getControlerIO();

		// SETTING "parameter calibration" parameters
		watching = Boolean.parseBoolean(config.findParam(
				BSE_CONFIG_MODULE_NAME, "watching"));
		String parameterDimensionStr = config.findParam(BSE_CONFIG_MODULE_NAME,
				"parameterDimension");
		if (parameterDimensionStr != null) {
			paramDim = Integer.parseInt(parameterDimensionStr);// e.g.
			// =2
			// [traveling,performing]
		} else {
			throw new RuntimeException("bse.parameterDimension muss be filled!");
		}

		paramNames = new String[paramDim];
		for (int i = 0; i < paramNames.length; i++) {
			paramNames[i] = config.findParam(BSE_CONFIG_MODULE_NAME,
					PARAM_NAME_INDEX + i);
		}

		// int timeBinSize_s = Integer.parseInt(config.findParam(
		// BSE_CONFIG_MODULE_NAME, "timeBinSize_s"));

		// SETTING REGRESSIONINERTIA
		String regressionInertiaValue = config.findParam(
				BSE_CONFIG_MODULE_NAME, "regressionInertia");
		double regressionInertia = 0;
		if (regressionInertiaValue == null) {
			System.out.println("BSE:\tregressionInertia\t= default value\t"
					+ Calibrator.DEFAULT_REGRESSION_INERTIA);
			regressionInertia = Calibrator.DEFAULT_REGRESSION_INERTIA;
		} else {
			regressionInertia = Double.parseDouble(regressionInertiaValue);
			System.out.println("BSE:\tregressionInertia\t=\t"
					+ regressionInertia);
		}

		// INITIALIZING "Calibrator"
		calibrator = new MATSimChoiceParameterCalibrator<Link>(ctlIO
				.getOutputFilename("calibration.log"), MatsimRandom
				.getLocalInstance(), regressionInertia, paramDim);

		// SETTING staticsFile
		calibrator.setStatisticsFile(ctlIO
				.getOutputFilename("calibration-stats.txt"));
	}

	private void setInitialParametersInCalibrator(Config config) {
		ScoringConfigGetValue.setConfig(config);
		PlanCalcScoreConfigGroup scoringCfg = config
				.charyparNagelScoring();

		Vector initialParams = new Vector(paramDim);
		for (int i = 0; i < initialParams.size(); i++) {
			double paramScaleFactor = Events2Score4PC.paramScaleFactorList
					.get(Events2Score4PC.attrNameList.indexOf(paramNames[i]));

			if (scoringCfg.getParams().containsKey(paramNames[i])) {

				initialParams.set(i, Double.parseDouble(ScoringConfigGetValue
						.getValue(paramNames[i]))
						* paramScaleFactor);

			} else/* bse */{
				initialParams.set(i, Double.parseDouble(config.findParam(
						BSE_CONFIG_MODULE_NAME, paramNames[i]))
						* paramScaleFactor);
			}
		}

		((ChoiceParameterCalibrator<Link>) calibrator)
				.setInitialParameters(initialParams);// without ASC
	}

	private void setInitialParameterVariancesInCalbrator(Config config) {
		String setInitialParamVarStr = config.findParam(BSE_CONFIG_MODULE_NAME,
				"setInitialParameterVariances");
		if (setInitialParamVarStr != null) {
			if (Boolean.parseBoolean(setInitialParamVarStr)) {
				Vector initialParamVars = new Vector(paramDim);
				// =2 without ASC
				for (int i = 0; i < paramDim; i++) {
					initialParamVars.set(i, Double.parseDouble(config
							.findParam(BSE_CONFIG_MODULE_NAME,
									PARAM_STDDEV_INDEX + i)));// [traveling,performing]
				}

				((ChoiceParameterCalibrator<Link>) calibrator)
						.setInitialParameterVariances(initialParamVars);
			}
		} else {
			Logger
					.getLogger("BSE:\tNo setting of setInitialParameterVariances, so it is accepted as FALSE");
		}
	}

	private void readCounts(Controler ctl) {
		final Counts counts = ctl.getCounts();
		final Network network = ctl.getNetwork();
		final Config config = ctl.getConfig();

		if (counts == null) {
			throw new RuntimeException("BSE requires counts-data.");
		}

		int caliStartTime = Integer.parseInt(config.findParam(
				BSE_CONFIG_MODULE_NAME, "startTime"));
		int caliEndTime = Integer.parseInt(config.findParam(
				BSE_CONFIG_MODULE_NAME, "endTime"));

		for (Map.Entry<Id, Count> entry : counts.getCounts().entrySet()) {
			Link link = network.getLinks().get(entry.getKey());
			if (link == null) {
				System.err.println("could not find link "
						+ entry.getKey().toString());
			} else if (isInRange(entry.getKey(), network)) {
				// for ...2QGIS
				// links.add(network.getLinks().get(entry.getKey()));
				// linkIds.add(entry.getKey());
				// ---------GUNNAR'S CODES---------------------
				for (Volume volume : entry.getValue().getVolumes().values()) {
					if (volume.getHour() >= caliStartTime
							&& volume.getHour() <= caliEndTime) {
						int start_s = (volume.getHour() - 1) * 3600;
						int end_s = volume.getHour() * 3600 - 1;
						double val_veh_h = volume.getValue();
						calibrator.addMeasurement(link, start_s, end_s,
								val_veh_h, TYPE.FLOW_VEH_H);
					}
				}
			}
		}
	}

	private void setCalibratorParameters(Config config) {
		// SETTING setUseApproximateNewton
		{
			String useApproximateNewtonStr = config.findParam(
					BSE_CONFIG_MODULE_NAME, "useApproximateNewton");
			if (useApproximateNewtonStr != null) {
				boolean useApproximateNewton = Boolean
						.parseBoolean(useApproximateNewtonStr);
				((ChoiceParameterCalibrator<Link>) calibrator)
						.setUseApproximateNetwton(useApproximateNewton);
				System.out.println("BSE:\tuseApproximateNetwton\t=\t"
						+ useApproximateNewton);
			} else {
				System.out
						.println("BSE:\tuseApproximateNetwton\t= default value\t"
								+ ChoiceParameterCalibrator.DEFAULT_USE_APPROXIMATE_NEWTON);
			}
		}

		// SETTING proportionalAssignment
		{
			String proportionalAssignmentStr = config.findParam(
					BSE_CONFIG_MODULE_NAME, "proportionalAssignment");
			if (proportionalAssignmentStr != null) {
				boolean proportionalAssignment = Boolean
						.parseBoolean(proportionalAssignmentStr);
				System.out.println("BSE:\tproportionalAssignment\t= "
						+ proportionalAssignment);
				calibrator.setProportionalAssignment(proportionalAssignment);
			} else {
				System.out
						.println("BSE:\tproportionalAssignment\t= default value\t"
								+ Calibrator.DEFAULT_PROPORTIONAL_ASSIGNMENT);
			}
		}

		// SETTING FREEZE ITERATION
		// {
		// final String freezeIterationStr =
		// config.findParam(BSE_CONFIG_MODULE_NAME,
		// "freezeIteration");
		// if (freezeIterationStr != null) {
		// final int freezeIteration = Integer
		// .parseInt(freezeIterationStr);
		// System.out.println("BSE:\tfreezeIteration\t= "
		// + freezeIteration);
		// calibrator.setFreezeIteration(freezeIteration);
		// } else
		// System.out.println("BSE:\tfreezeIteration\t= default value\t"
		// + Calibrator.DEFAULT_FREEZE_ITERATION);
		// }

		// SETTING MINSTDDEV
		{
			String minStdDevStr = config.findParam(BSE_CONFIG_MODULE_NAME,
					"minFlowStddevVehH");
			if (minStdDevStr != null) {
				double minStdDev = Double.parseDouble(minStdDevStr);
				calibrator.setMinStddev(minStdDev, TYPE.FLOW_VEH_H);
				System.out.println("BSE:\tminStdDev\t= " + minStdDev);
			} else {
				System.out.println("BSE:\tminStdDev\t= default value\t"
						+ Calibrator.DEFAULT_MIN_FLOW_STDDEV_VEH_H);
			}
		}
		// SETTING Preparatory Iterations
		{
			final String preparatoryIterationsStr = config.findParam(
					BSE_CONFIG_MODULE_NAME, "preparatoryIterations");
			if (preparatoryIterationsStr != null) {
				final int preparatoryIterations = Integer
						.parseInt(preparatoryIterationsStr);
				System.out.println("BSE:\tpreparatoryIterations\t= "
						+ preparatoryIterations);
				calibrator.setPreparatoryIterations(preparatoryIterations);
			} else {
				System.out
						.println("BSE:\tpreparatoryIterations\t= default value\t"
								+ Calibrator.DEFAULT_PREPARATORY_ITERATIONS);
			}
		}
		// SETTING varianceScale
		{
			final String varianceScaleStr = config.findParam(
					BSE_CONFIG_MODULE_NAME, "varianceScale");
			if (varianceScaleStr != null) {
				final double varianceScale = Double
						.parseDouble(varianceScaleStr);
				System.out.println("BSE:\tvarianceScale\t= " + varianceScale);
				calibrator.setVarianceScale(varianceScale);
			} else {
				System.out.println("BSE:\tvarianceScale\t= default value\t"
						+ Calibrator.DEFAULT_VARIANCE_SCALE);
			}
		}
		// SETTING parameterStepSize
		{
			String initialStepSizeStr = config.findParam(
					BSE_CONFIG_MODULE_NAME, "initialStepSize");
			System.out.print("BSE:\tparameterStepSize\t= ");
			if (initialStepSizeStr != null) {
				double initialStepSize = Double.parseDouble(initialStepSizeStr);
				System.out.println(initialStepSize);
				((ChoiceParameterCalibrator<Link>) calibrator)
						.setInitialStepSize(initialStepSize);
			} else {
				System.out.println("default value\t"
						+ ChoiceParameterCalibrator.DEFAULT_INITIAL_STEP_SIZE);
			}
		}
		// SETTING msaExponent
		{
			/*
			 * initialStepSize * 1.0 /(iteration ^ msaExponent), if msaExponent
			 * == 0.5, the step won't so quickly smaller, default value: 1.0
			 */
			String msaExponentStr = config.findParam(BSE_CONFIG_MODULE_NAME,
					"msaExponent");
			if (msaExponentStr != null) {
				double msaExponent = Double.parseDouble(msaExponentStr);
				((ChoiceParameterCalibrator<Link>) calibrator)
						.setMsaExponent(msaExponent);
				System.out.println("BSE:\tmsaExponent\t=" + msaExponent);
			} else {
				System.out.println("BSE:\tmsaExponent\t= default value\t"
						+ ChoiceParameterCalibrator.DEFAULT_MSA_EXPONENT);
			}
		}
	}

	private void initializeStrategyManager(Controler ctl) {
		String deltaStr = ctl.getConfig().findParam(BSE_CONFIG_MODULE_NAME,
				"delta");
		double delta;
		if (deltaStr == null) {
			delta = 1e-6;
			System.out.println("BSE:\tdelta\t= default value\t" + delta);
		} else {
			delta = Double.parseDouble(deltaStr);
			System.out.println("BSE:\tdelta\t=\t" + delta);
		}
		((PCStrMn) ctl.getStrategyManager()).init(
				(ChoiceParameterCalibrator<Link>) calibrator, ctl
						.getTravelTimeCalculator(),
				(MultinomialLogitChoice) chooser, delta);
	}

	private void initializeOutput(Controler ctl) {
		ControlerIO ctlIO = ctl.getControlerIO();
		{
			writer = new SimpleWriter(ctlIO.getOutputFilename("parameters.log"));
			StringBuffer sb = new StringBuffer("iter");
			for (int i = 0; i < paramNames.length; i++) {
				sb.append("\t");
				sb.append(paramNames[i]);

			}
			writer.writeln(sb);
		}
		{
			writerCV = new SimpleWriter(ctlIO
					.getOutputFilename("parameterCovariance.log"));
			StringBuffer sb = new StringBuffer("iter\tmatrix/Covariance ["
					+ paramNames[0]);
			for (int i = 1; i < paramNames.length; i++) {
				sb.append(", ");
				sb.append(paramNames[i]);
			}
			sb.append("]");
			writerCV.writeln(sb);
		}
		{
			StringBuffer sb = new StringBuffer(paramNames[0]);
			for (int i = 1; i < paramNames.length; i++) {
				sb.append(" & ");
				sb.append(paramNames[i]);
			}
			chart = new XYLineChart(sb.toString(), "iter", "value of parameter");
		}
		int arraySize = ctl.getLastIteration() - ctl.getFirstIteration() + 1;
		paramArrays = new double[paramDim][];
		for (int i = 0; i < paramNames.length; i++) {
			paramArrays[i] = new double[arraySize];// e.g. traveling,
			// performing
		}
	}

	public void notifyStartup(final StartupEvent event) {
		final PCCtl ctl = (PCCtl) event.getControler();
		Config config = ctl.getConfig();

		setMatsimParameters(ctl);
		initializeCalibrator(ctl);

		// *********SETTING PARAMETERS FOR "PARAMETER CALIBRATION"****
		setInitialParametersInCalibrator(config);
		setInitialParameterVariancesInCalbrator(config);

		// *******************SETTING CALIBRATOR********************
		setCalibratorParameters(config);

		// ***************SETTING PARAMETERS FOR INTEGRATION***********
		// SETTING countsScale
		{
			countsScaleFactor = config.counts().getCountsScaleFactor();
			System.out.println("BSE:\tusing the countsScaleFactor of "
					+ countsScaleFactor + " as packetSize from config.");
		}
		cycle = Integer.parseInt(config.findParam(BSE_CONFIG_MODULE_NAME,
				"cycle"));

		// READING countsdata
		readCounts(ctl);

		// INITIALIZING chooser
		chooser = ctl.getPlansScoring4PC().getPlanScorer();

		// INITIALIZING StrategyManager
		initializeStrategyManager(ctl);
		// ******************************************************************

		// INITIALIZING resultContainer
		resultsContainer = new SimResultsContainerImpl();

		// INITIALIZING OUTPUT
		initializeOutput(ctl);
	}

	public void notifyShutdown(ShutdownEvent event) {
		writer.close();
		writerCV.close();

		Controler ctl = event.getControler();
		ControlerIO ctlIO = ctl.getControlerIO();
		int firstIter = ctl.getFirstIteration(), lastIter = ctl
				.getLastIteration();

		double[] xs = new double[lastIter - firstIter + 1];
		for (int i = firstIter; i <= lastIter; i++) {
			xs[i - firstIter] = i;
		}

		String chartFilename = "parameters_";
		for (int i = 0; i < paramNames.length; i++) {
			chart.addSeries(paramNames[i], xs, paramArrays[i]);
			chartFilename += paramNames[i] + ".";
		}

		chart.saveAsPng(ctlIO.getOutputFilename(chartFilename + "png"), 1024,
				768);

		// this.travPerfChart.saveAsPng(ctlIO
		// .getOutputFilename("statistics-travPerf.png"), 1024, 768);
	}

	public void setWriteQGISFile(boolean writeQGISFile) {
		// dummy
	}

	private void outputHalfway(Controler ctl, int outputIterInterval) {
		int iter = ctl.getIterationNumber();
		if (iter % outputIterInterval == 0) {
			ControlerIO ctlIO = ctl.getControlerIO();
			int firstIter = ctl.getFirstIteration();

			double[] xs = new double[iter - firstIter + 1];
			for (int i = firstIter; i <= iter; i++) {
				xs[i - firstIter] = i;
			}

			String chartTitle = "Calibrated Parameter(s):";
			for (String paramName : paramNames) {
				chartTitle += " " + paramName;
			}

			XYLineChart chart = new XYLineChart(chartTitle, "iter",
					"value of parameters") // ,travPerf = new
			// XYLineChart("traveling & performing - avg. and sqrt(var)",
			// "iteration","[h]")
			;
			String chartFilename = "";
			for (int i = 0; i < paramNames.length; i++) {
				chart.addSeries(paramNames[i], xs, paramArrays[i]);
				chartFilename += paramNames[i] + ".";
			}

			chart.saveAsPng(ctlIO.getIterationFilename(iter, chartFilename
					+ "png"), 1024, 768);

			// travPerf.saveAsPng(ctlIO.getIterationFilename(iter,
			// "travTravPt.png"), 1024, 768);
		}
	}

	public void notifyIterationEnds(IterationEndsEvent event) {
		PCCtl ctl = (PCCtl) event.getControler();
		Config config = ctl.getConfig();
		int iter = event.getIteration();
		int firstIter = ctl.getFirstIteration();
		// this.chooser.finish();-->called in notifyScoring()
		PCStrMn strategyManager = (PCStrMn) ctl.getStrategyManager();

		if (iter - firstIter > strategyManager.getMaxPlansPerAgent()) {
			// ***************************************************
			calibrator.afterNetworkLoading(resultsContainer);
			// ************************************************

			writerCV.writeln(iter
					+ ":\n"
					+ ((ChoiceParameterCalibrator<Link>) calibrator)
							.getParameterCovariance());
			writerCV.flush();

			// ****SET CALIBRATED PARAMETERS FOR SCORE CALCULATION AGAIN!!!***
			Vector params = ((ChoiceParameterCalibrator<Link>) calibrator)
					.getParameters();
			PlanCalcScoreConfigGroup scoringCfg = config
					.charyparNagelScoring();

			// VERY IMPORTANT #########################################

			if (!watching && cycleIdx == 0) {
				MultinomialLogit mnl = ((MultinomialLogitChoice) chooser)
						.getMultinomialLogit();

				// *******should after Scoring Listener!!!*******

				if (((ChoiceParameterCalibrator<Link>) calibrator)
						.getInitialStepSize() != 0d) {

					StringBuffer sb = new StringBuffer(Integer.toString(iter));

					for (int i = 0; i < paramNames.length; i++) {
						int paramNameIndex = Events2Score4PC.attrNameList
								.indexOf(paramNames[i]/*
													 * pos. of param in
													 * Parameters in Cadyts
													 */);
						double paramScaleFactor = Events2Score4PC.paramScaleFactorList
								.get(paramNameIndex);

						double value = params.get(i);
						if (scoringCfg.getParams().containsKey(paramNames[i])) {
							scoringCfg.addParam(paramNames[i], Double
									.toString(value / paramScaleFactor));
						} else/* bse */{
							config.setParam(BSE_CONFIG_MODULE_NAME,
									paramNames[i], Double.toString(value
											/ paramScaleFactor));
						}

						mnl.setParameter(paramNameIndex, value);

						// text output
						paramArrays[i][iter - firstIter] = value
								/ paramScaleFactor;
						sb.append("\t");
						sb.append(value / paramScaleFactor);
					}

					writer.writeln(sb);
					writer.flush();
				}/* initialStepSize==0, no parameters are changed */

				((Events2Score4PC_mnl) chooser).setMultinomialLogit(mnl);

				CharyparNagelScoringFunctionFactory4PC sfFactory = new CharyparNagelScoringFunctionFactory4PC(
						config);
				ctl.setScoringFunctionFactory(sfFactory);
				((Events2Score4PC_mnl) chooser).setSfFactory(sfFactory);

				strategyManager.setChooser(chooser);

			}
			cycleIdx++;
			if (cycleIdx == cycle) {
				cycleIdx = 0;
			}
		}

		// output - chart etc.
		outputHalfway(ctl, 50);
	}
}
