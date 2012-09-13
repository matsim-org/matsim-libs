///* *********************************************************************** *
// * project: org.matsim.*
// * SimCntLogLikelihoodCtlListener.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
//package playground.yu.tests.parameterCalibration.naiveWithoutUC;
//
//import java.util.Map;
//
//import org.matsim.analysis.VolumesAnalyzer;
//import org.matsim.api.core.v01.Coord;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.core.basic.v01.IdImpl;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.controler.Controler;
//import org.matsim.core.controler.events.AfterMobsimEvent;
//import org.matsim.core.controler.events.ShutdownEvent;
//import org.matsim.core.controler.events.StartupEvent;
//import org.matsim.core.controler.listener.AfterMobsimListener;
//import org.matsim.core.controler.listener.ShutdownListener;
//import org.matsim.core.controler.listener.StartupListener;
//import org.matsim.core.network.LinkImpl;
//import org.matsim.counts.Count;
//import org.matsim.counts.Volume;
//
//import playground.yu.integration.cadyts.CalibrationConfig;
//import playground.yu.scoring.withAttrRecorder.leftTurn.LeftTurnPenaltyControler;
//import playground.yu.utils.io.SimpleWriter;
//import cadyts.calibrators.Calibrator;
//
//public class SimCntLogLikelihoodCtlListenerWithUnitiveVar implements
//		StartupListener, AfterMobsimListener, ShutdownListener {
//	private double minStdDev, llhSum = 0d, distanceFilter,
//			countsScaleFactor = 1d, varianceScale = 1d, avgLlh;
//
//	private int avgLlhOverIters = 0, writeLlhInterval = 0, caliStartTime = 1,
//			caliEndTime = 24;
//
//	private Coord distanceFilterCenterNodeCoord = null;
//
//	private SimpleWriter writer = null, writer2 = null;
//
//	private int n = 0;
//
//	public int getAvgLlhOverIters() {
//		return avgLlhOverIters;
//	}
//
//	public int getWriteLlhInterval() {
//		return writeLlhInterval;
//	}
//
//	public double getAverageLoglikelihood() {
//		return avgLlh;
//	}
//
//	@Override
//	public void notifyAfterMobsim(AfterMobsimEvent event) {
//		int iter = event.getIteration();
//		Controler ctl = event.getControler();
//		// TESTS: calculate log-likelihood -(q-y)^2/(2sigma^2)
//		if (writeLlhInterval > 0 && avgLlhOverIters > 0
//				&& iter > ctl.getFirstIteration()) {
//			int nextWriteLlhInterval = writeLlhInterval
//					* (iter / writeLlhInterval + 1);
//
//			if (iter <= nextWriteLlhInterval
//					&& iter > nextWriteLlhInterval - avgLlhOverIters
//					|| iter % writeLlhInterval == 0) {
//				n++;
//			}
//
//			VolumesAnalyzer volumes = ctl.getVolumes();
//			Network network = ctl.getNetwork();
//
//			double localLlhSum = 0;
//			for (Map.Entry<Id, Count> entry : ctl.getCounts().getCounts()
//					.entrySet()) {
//				Id countId = entry.getKey();
//				Link link = network.getLinks().get(countId);
//				if (link == null) {
//					System.err.println("could not find link "
//							+ countId.toString());
//				} else if (isInRange(countId, network)) {
//					// for ...2QGIS
//					// links.add(network.getLinks().get(entry.getKey()));
//					// linkIds.add(entry.getKey());
//
//					int[] linkVols = volumes.getVolumesForLink(countId);
//					// ---------GUNNAR'S CODES---------------------
//					for (Volume volume : entry.getValue().getVolumes().values()) {
//						int hour = volume.getHourOfDayStartingWithOne();
//						if (hour >= caliStartTime && hour <= caliEndTime) {
//							double cntVal = volume.getValue();
//							double simVal = 0d;
//							if (linkVols != null) {
//								simVal = linkVols[hour - 1] * countsScaleFactor;
//							}
//							// **********************************************
//							double var = minStdDev * minStdDev;
//							// **********************************************
//							double absLlh = (simVal - cntVal)
//									* (simVal - cntVal) / 2d / var;
//
//							localLlhSum -= absLlh;
//							if (iter <= nextWriteLlhInterval
//									&& iter > nextWriteLlhInterval
//											- avgLlhOverIters
//									|| iter % writeLlhInterval == 0) {
//								llhSum -= absLlh;
//								writer.writeln("ITER\t" + iter
//										+ "\tAccumulated Llh over " + n
//										+ " iterations =\t" + llhSum
//										+ "\tadded llh =\t-" + absLlh);
//								writer.flush();
//							}
//						}
//					}
//				}
//
//			}
//			writer2.writeln("BSE:\tllh of ITER\t" + iter + "\t=\t"
//					+ localLlhSum);
//			writer2.flush();
//
//			if (iter % writeLlhInterval == 0) {
//				// calculate avg. value of llh
//				avgLlh = llhSum / n;
//				System.out.println("ITER\t" + iter + "\tavgLlh over " + n
//						+ " iterations =\t" + avgLlh + "\t;\tsum of Llh =\t"
//						+ llhSum);
//				llhSum = 0d;// refresh
//				n = 0;
//			}
//		}
//	}
//
//	@Override
//	public void notifyStartup(StartupEvent event) {
//		Controler ctl = event.getControler();
//		Config config = ctl.getConfig();
//
//		// SETTING DISTANCE_FILTER_CENTER_NODE_COORD
//		String distFilterCenterNodeStr = config.counts()
//				.getDistanceFilterCenterNode();
//		if (distFilterCenterNodeStr != null) {
//			distanceFilterCenterNodeCoord = ctl.getNetwork().getNodes()
//					.get(new IdImpl(distFilterCenterNodeStr)).getCoord();
//			distanceFilter = config.counts().getDistanceFilter();
//		}
//
//		// SETTING MINSTDDEV
//		String minStdDevStr = config.findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME, "minFlowStddevVehH");
//		if (minStdDevStr != null) {
//			minStdDev = Double.parseDouble(minStdDevStr);
//			System.out.println("BSE:\tminStdDev\t= " + minStdDev);
//		} else {
//			minStdDev = Calibrator.DEFAULT_MIN_FLOW_STDDEV_VEH_H;
//			System.out.println("BSE:\tminStdDev\t= default value\t"
//					+ Calibrator.DEFAULT_MIN_FLOW_STDDEV_VEH_H);
//		}
//
//		// SETTING AVG_LLH_OVER_ITERS
//		String avgLlhOverItersStr = config.findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME,
//				"averageLogLikelihoodOverIterations");
//		if (avgLlhOverItersStr != null) {
//			avgLlhOverIters = Integer.parseInt(avgLlhOverItersStr);
//		}
//
//		// SETTING WRITE_LOG-LIKELIHOOD_ITERVAL
//		String writeLlhItervalStr = config.findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME,
//				"writeLogLikelihoodInterval");
//		if (writeLlhItervalStr != null) {
//			writeLlhInterval = Integer.parseInt(writeLlhItervalStr);
//		}
//
//		// SETTING CALIBRATION_START_TIME
//		String caliStartTimeStr = config.findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME, "startTime");
//		if (caliStartTimeStr != null) {
//			caliStartTime = Integer.parseInt(caliStartTimeStr);
//		}
//
//		// SETTING CALIBRATION_END_TIME
//		String caliEndTimeStr = config.findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME, "endTime");
//		if (caliEndTimeStr != null) {
//			caliEndTime = Integer.parseInt(caliEndTimeStr);
//		}
//
//		// SETTING VARIANCE_SCALE
//		final String varianceScaleStr = config.findParam(
//				CalibrationConfig.BSE_CONFIG_MODULE_NAME, "varianceScale");
//		if (varianceScaleStr != null) {
//			varianceScale = Double.parseDouble(varianceScaleStr);
//			System.out.println("BSE:\tvarianceScale\t= " + varianceScale);
//		} else {
//			System.out.println("BSE:\tvarianceScale\t= default value\t"
//					+ Calibrator.DEFAULT_VARIANCE_SCALE);
//		}
//
//		// SETTING COUNTS_SCALE_FACTOR
//		countsScaleFactor = config.counts().getCountsScaleFactor();
//
//		// INITIALIZING WRITER
//		writer = new SimpleWriter(ctl.getControlerIO().getOutputFilename(
//				"log-likelihood.detailed.log.gz"));
//		writer2 = new SimpleWriter(ctl.getControlerIO().getOutputFilename(
//				"log-likelihoodStats.log"));
//	}
//
//	private boolean isInRange(final Id linkid, final Network net) {
//		if (distanceFilterCenterNodeCoord == null) {
//			return true;
//		}
//
//		Link l = net.getLinks().get(linkid);
//		if (l == null) {
//			System.out.println("Cannot find requested link: "
//					+ linkid.toString());
//			return false;
//		}
//		return ((LinkImpl) l).calcDistance(distanceFilterCenterNodeCoord) < distanceFilter;
//	}
//
//	public static void main(String[] args) {
//		Config cfg = ConfigUtils.loadConfig(args[0]);
//		LeftTurnPenaltyControler ctl = new LeftTurnPenaltyControler(cfg);
//		ctl.addControlerListener(new SimCntLogLikelihoodCtlListenerWithUnitiveVar());
//		ctl.setCreateGraphs(false);
//		ctl.setOverwriteFiles(true);
//		ctl.run();
//	}
//
//	@Override
//	public void notifyShutdown(ShutdownEvent event) {
//		writer.close();
//		writer2.close();
//	}
//}
