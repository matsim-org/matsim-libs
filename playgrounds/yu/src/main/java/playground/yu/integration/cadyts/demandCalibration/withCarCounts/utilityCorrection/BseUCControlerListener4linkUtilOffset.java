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

package playground.yu.integration.cadyts.demandCalibration.withCarCounts.utilityCorrection;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;

import playground.yu.integration.cadyts.demandCalibration.withCarCounts.BseControlerListener;
import playground.yu.integration.cadyts.demandCalibration.withCarCounts.BseLinkCostOffsetsXMLFileIO;
import playground.yu.integration.cadyts.demandCalibration.withCarCounts.utils.qgis.LinkCostOffsets2QGIS;
import playground.yu.linkUtilOffset.hourVersion.PopLinksTimeBinsMatrixCreator;
import playground.yu.utils.io.SimpleWriter;
import playground.yu.utils.math.MatrixUtils;
import playground.yu.utils.qgis.LinkUtilityOffset2QGIS;
import playground.yu.utils.qgis.MATSimNet2QGIS;
import playground.yu.utils.qgis.X2QGIS;
import Jama.Matrix;
import cadyts.calibrators.Calibrator;
import cadyts.interfaces.matsim.MATSimUtilityModificationCalibrator;
import cadyts.measurements.SingleLinkMeasurement.TYPE;
import cadyts.supply.SimResults;
import cadyts.utilities.misc.DynamicData;

public class BseUCControlerListener4linkUtilOffset implements StartupListener,
		AfterMobsimListener, BeforeMobsimListener, BseControlerListener {

	private final static Logger log = Logger.getLogger(BseUCControlerListener4linkUtilOffset.class);

	private MATSimUtilityModificationCalibrator<Link> calibrator = null;
	private SimResults<Link> resultsContainer = null;
	/* default */VolumesAnalyzer volumes = null;
	/* default */double countsScaleFactor = 1.0;
	private Coord distanceFilterCenterNodeCoord;
	private double distanceFilter;
	private int arStartTime, arEndTime;
	private boolean writeQGISFile = false;

	private static List<Link> links = new ArrayList<Link>();
	private static Set<Id> linkIds = new HashSet<Id>();
	private int strategyDisableAfter = 0;
	private PopLinksTimeBinsMatrixCreator pltbmc = null;
	private String MATRIX_CALCULATION = "Matrix Calculation";

	private boolean isInRange(final Id linkid, final Network net) {
		Link l = net.getLinks().get(linkid);
		if (l == null) {
			System.out.println("Cannot find requested link: "
					+ linkid.toString());
			return false;
		}
		return ((LinkImpl) l).calcDistance(distanceFilterCenterNodeCoord) < distanceFilter;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		int iter = event.getIteration();
		if (iter % 100 == 0 && iter > strategyDisableAfter) {
			Controler ctl = event.getControler();
			pltbmc.init();
			ctl.getEvents().addHandler(pltbmc);
		}
	}

	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		calibrator.afterNetworkLoading(resultsContainer
		// , null
				);
		int iter = event.getIteration();
		Controler ctl = event.getControler();

		ControlerIO io = ctl.getControlerIO();
		if (iter % 10 == 0) {
			try {
				DynamicData<Link> linkCostOffsets = calibrator
						.getLinkCostOffsets();
				new BseLinkCostOffsetsXMLFileIO(ctl.getNetwork()).write(io
						.getIterationFilename(iter, "linkCostOffsets.xml"),
						linkCostOffsets);
				if (writeQGISFile) {
					for (int i = arStartTime; i <= arEndTime; i++) {
						LinkCostOffsets2QGIS lco2QGSI = new LinkCostOffsets2QGIS(
								ctl.getNetwork(), ctl.getConfig().global()
										.getCoordinateSystem(), i, i);
						lco2QGSI.createLinkCostOffsets(links, linkCostOffsets);
						lco2QGSI.output(linkIds, io.getIterationFilename(iter,
								""));
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// create matrix A
		if (iter % 100 == 0 && iter > strategyDisableAfter) {
			Matrix A = pltbmc.getMatrix();
			printMatrix(A, io.getIterationFilename(iter,
					"popLinkTimeMatrixA.log"), null, 2);

			ctl.getEvents().removeHandler(pltbmc);

			// get matrix (Vector) b
			Map<Id, Double> perUtilityOffsets = ((BseUCStrategyManager) ctl
					.getStrategyManager()).getPersonUtilityOffsets();
			Matrix b = getConstantVector(pltbmc, perUtilityOffsets);
			printMatrix(b,
					io.getIterationFilename(iter, "constantVectorB.log"),
					"0.###E00", 12);

			Matrix A_b = MatrixUtils.getAugmentMatrix(A, b);
			System.out.println("rank[A] =\t" + A.rank() + "\trank[A_b] =\t"
					+ A_b.rank());

			log.info("Minimum Norm Solution BEGAN");
			Matrix x = MatrixUtils.getMinimumNormSolution(A, b);
			log.info("Minimum Norm Solution ENDED");

			// output unknown x
			SimpleWriter writer = new SimpleWriter(io.getIterationFilename(
					iter, "resultVectorX.log"));
			Map<Integer/* timeBin */, Map<Id/* linkId */, Double/* utiliyOffset */>> linkUtilityOffsets = new HashMap<Integer, Map<Id, Double>>();
			for (int rowIdx = 0; rowIdx < x.getRowDimension(); rowIdx++) {
				String linkIdTimeBin = pltbmc.getLinkTimeBinSequence2().get(
						rowIdx);
				StringBuffer sb = new StringBuffer(linkIdTimeBin);
				sb.append("\tutiliyOffset\t");
				double x_rowIdx = x.get(rowIdx, 0);
				sb.append(x_rowIdx);
				writer.writeln(sb);

				String[] row = linkIdTimeBin.split("\t");
				String linkId = row[1];
				int timeBin = Integer.parseInt(row[3]);

				Map<Id/* linkId */, Double/* x */> utilOffsetsPerTimeBin = linkUtilityOffsets
						.get(timeBin);
				if (utilOffsetsPerTimeBin == null) {
					utilOffsetsPerTimeBin = new HashMap<Id, Double>();
					linkUtilityOffsets.put(timeBin, utilOffsetsPerTimeBin);
				}
				utilOffsetsPerTimeBin.put(new IdImpl(linkId), x_rowIdx);
			}
			writer.close();
			// x to QGIG
			MATSimNet2QGIS net2qgis = new MATSimNet2QGIS(ctl.getScenario(),
					X2QGIS.gk4);
			LinkUtilityOffset2QGIS luos2qgis = new LinkUtilityOffset2QGIS(
					linkUtilityOffsets, net2qgis.getNetwork());
			for (Integer timeBin : luos2qgis.getLinkUtilityOffsets().keySet()) {
				net2qgis.addParameter(timeBin + "hour", Double.class, luos2qgis
						.getLinkUtilityOffsets().get(timeBin));
			}
			net2qgis.writeShapeFile(io.getIterationFilename(iter,
					"linkUtilityOffset.shp"));

			// Ax-b
			Matrix Residual = A.times(x).minus(b);
			printMatrix(Residual, io.getIterationFilename(iter,
					"residual_Ax-b.log"), "0.###E00", 12);

			double rnorm = Residual.normInf();
			System.out.println("Matrix\tResidual Infinity norm:\t" + rnorm);
			//
			pltbmc.reset(iter);
		}
	}

	private void printMatrix(Matrix matrix, String outputFilename,
			String pattern, int width) {
		try {
			PrintWriter printWriter = new PrintWriter(outputFilename);
			DecimalFormat format = pattern != null ? new DecimalFormat(pattern)
					: new DecimalFormat();
			matrix.print(printWriter, format, width);
			printWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private Matrix getConstantVector(PopLinksTimeBinsMatrixCreator Acreator,
			Map<Id, Double> personUtilOffsets) {
		Matrix b = new Matrix(Acreator.getPersonSequence().size(), 1);
		for (Entry<Id, Integer> perIdRowIdxPair : pltbmc.getPersonSequence()
				.entrySet()) {
			int rowIdx = perIdRowIdxPair.getValue();
			double utilOffset = personUtilOffsets
					.get(perIdRowIdxPair.getKey()/* personId */);
			b.set(rowIdx, 0, utilOffset);
		}
		return b;
	}

	// @deprecated
	// public void notifyIterationStarts(IterationStartsEvent event) {
	// // outputs of score-modification from StrategyManager
	// Controler ctl = event.getControler();
	// int iter = event.getIteration();
	//
	// if (iter % 100 == 0 && iter > strategyDisableAfter) {
	// ((BseUCStrategyManager) ctl.getStrategyManager())
	// .setScoreModificationFilename(ctl
	// .getControlerIO()
	// .getIterationFilename(iter, "scoreModification.log"));
	// }
	// }

	@Override
	public void setWriteQGISFile(final boolean writeQGISFile) {
		this.writeQGISFile = writeQGISFile;
	}

	@Override
	public void notifyStartup(final StartupEvent event) {
		final Controler ctl = event.getControler();
		final Network network = ctl.getNetwork();
		Config config = ctl.getConfig();

		// set up center and radius of counts stations locations
		distanceFilterCenterNodeCoord = network.getNodes().get(
				new IdImpl(config.findParam("counts",
						"distanceFilterCenterNode"))).getCoord();
		distanceFilter = Double.parseDouble(config.findParam("counts",
				"distanceFilter"));
		arStartTime = Integer.parseInt(config.findParam("bse", "startTime"));
		arEndTime = Integer.parseInt(config.findParam("bse", "endTime"));

		// set up volumes analyzer
		volumes = new VolumesAnalyzer(3600, 30 * 3600, network);
		ctl.getEvents().addHandler(volumes);

		// get default regressionInertia
		String regressionInertiaValue = config.findParam("bse",
				"regressionInertia");
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
		// ----------------------------------------------------------
		calibrator = new MATSimUtilityModificationCalibrator<Link>(null,
				MatsimRandom.getLocalInstance(), regressionInertia);
		// -----------------------------------------------------------
		// this.calibrator.setVerbose(true);

		// Set default standard deviation
		{
			String value = config.findParam("bse", "minFlowStddevVehH");
			if (value == null) {
				System.out.println("BSE:\tminFlowStddevVehH\t= default value\t"
						+ Calibrator.DEFAULT_MIN_FLOW_STDDEV_VEH_H);
			} else {
				double stddev_veh_h = Double.parseDouble(value);
				calibrator.setMinStddev(stddev_veh_h, TYPE.FLOW_VEH_H);
				System.out.println("BSE:\tminFlowStddevVehH\t=\t"
						+ stddev_veh_h);
			}
		}
		// @deprecated SET MAX DRAWS
		// {
		// final String maxDrawStr = config.findParam("bse", "maxDraws");
		// if (maxDrawStr != null) {
		// final int maxDraws = Integer.parseInt(maxDrawStr);
		// System.out.println("BSE:\tmaxDraws=" + maxDraws);
		// calibrator.setMaxDraws(maxDraws);
		// } else {
		// System.out.println("BSE:\tmaxDraws\t= default value\t"
		// + SamplingCalibrator.DEFAULT_MAX_DRAWS);
		// }
		// }

		// SET FREEZE ITERATION
		{
			final String freezeIterationStr = config.findParam("bse",
					"freezeIteration");
			if (freezeIterationStr != null) {
				final int freezeIteration = Integer
						.parseInt(freezeIterationStr);
				System.out.println("BSE:\tfreezeIteration\t= "
						+ freezeIteration);
				calibrator.setFreezeIteration(freezeIteration);
			} else {
				System.out.println("BSE:\tfreezeIteration\t= default value\t"
						+ Calibrator.DEFAULT_FREEZE_ITERATION);
			}
		}
		// SET Preparatory Iterations
		{
			final String preparatoryIterationsStr = config.findParam("bse",
					"preparatoryIterations");
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

		// SET varianceScale
		{
			final String varianceScaleStr = config.findParam("bse",
					"varianceScale");
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
		// SET countsScale
		countsScaleFactor = config.counts().getCountsScaleFactor();
		System.out.println("BSE:\tusing the countsScaleFactor of "
				+ countsScaleFactor + " as packetSize from config.");
		/*
		 * SET rejectProb String rejectProb = config.findParam("bse",
		 * "rejectProbFactor"); double rejectProbFactor = 1.0; if (rejectProb ==
		 * null)
		 * System.out.println("BSE:\tusing the default reject probility of " +
		 * rejectProbFactor); else { rejectProbFactor =
		 * Double.parseDouble(rejectProb);
		 * System.out.println("BSE:\tusing the reject probility of " +
		 * rejectProbFactor + " from configfile."); }
		 */

		// reads countsdata
		final Counts counts = ctl.getCounts();
		if (counts == null) {
			throw new RuntimeException("BSE requires counts-data.");
		}

		for (Map.Entry<Id, Count> entry : counts.getCounts().entrySet()) {
			Link link = network.getLinks().get(entry.getKey());
			if (link == null) {
				System.err.println("could not find link "
						+ entry.getKey().toString());
			} else if (isInRange(entry.getKey(), network)) {
				// for ...2QGIS
				links.add(network.getLinks().get(entry.getKey()));
				linkIds.add(entry.getKey());
				// ---------GUNNAR'S CODES---------------------
				for (Volume volume : entry.getValue().getVolumes().values()) {
					if (volume.getHour() >= arStartTime
							&& volume.getHour() <= arEndTime) {
						int start_s = (volume.getHour() - 1) * 3600;
						int end_s = volume.getHour() * 3600 - 1;
						double val_veh_h = volume.getValue();
						calibrator.addMeasurement(link, start_s, end_s,
								val_veh_h, TYPE.FLOW_VEH_H);
					}
				}
			}
		}

		// set up a/r-strategy
		((BseUCStrategyManager) ctl.getStrategyManager()).init(calibrator, ctl
				.getTravelTimeCalculator(), config.planCalcScore()
				.getBrainExpBeta());
		// set the max Iteration to Disable new plans generating
		for (StrategySettings strategySetting : config.strategy()
				.getStrategySettings()) {
			int tmpStrategyDisableAfter = strategySetting.getDisableAfter();
			if (tmpStrategyDisableAfter > strategyDisableAfter) {
				strategyDisableAfter = tmpStrategyDisableAfter;
			}
		}
		// prepare resultsContainer
		resultsContainer = new SimResultsContainerImpl();
		// prepare matrix [person no. * (link no. * timbBin no.)]
		pltbmc = new PopLinksTimeBinsMatrixCreator(network,
				ctl.getPopulation(), arStartTime, arEndTime);
	}

	// INNER CLASS
	/* package */class SimResultsContainerImpl implements SimResults<Link> {

		/***/
		private static final long serialVersionUID = 1L;

		@Override
		public double getSimValue(final Link link, final int startTime_s,
				final int endTime_s, final TYPE type) {
			int hour = startTime_s / 3600;
			int[] values = volumes.getVolumesForLink(link.getId());
			if (values == null) {
				return 0;
			}
			return values[hour] * countsScaleFactor;
		}
	}

}
