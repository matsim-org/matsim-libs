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

package playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.events2PlanStep;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
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
import cadyts.calibrators.Calibrator;
import cadyts.interfaces.matsim.MATSimUtilityModificationCalibrator;
import cadyts.measurements.SingleLinkMeasurement.TYPE;
import cadyts.supply.SimResults;
import cadyts.utilities.misc.DynamicData;

public class BseUCControlerListener implements StartupListener,
		AfterMobsimListener, BeforeMobsimListener, BseControlerListener {
	private MATSimUtilityModificationCalibrator<Link> calibrator = null;
	private SimResults<Link> resultsContainer = null;
	/* default */VolumesAnalyzer volumes = null;
	/* default */double countsScaleFactor = 1.0;
	private Coord distanceFilterCenterNodeCoord;
	private double distanceFilter;
	private int arStartTime, arEndTime;
	private boolean writeQGISFile = false;
	private EventsToPlanSteps events2PlanStep = null;

	private static List<Link> links = new ArrayList<Link>();
	private static Set<Id> linkIds = new HashSet<Id>();

	private boolean isInRange(final Id linkid, final Network net) {
		Link l = net.getLinks().get(linkid);
		if (l == null) {
			System.out.println("Cannot find requested link: "
					+ linkid.toString());
			return false;
		}
		return ((LinkImpl) l).calcDistance(distanceFilterCenterNodeCoord) < distanceFilter;
	}

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
		EventsManager events = ctl.getEvents();
		events.addHandler(volumes);

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
		calibrator = new MATSimUtilityModificationCalibrator<Link>(ctl
				.getControlerIO().getOutputFilename("calibration.log"),
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
		// SET MAX DRAWS
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
		// SET linkNeighbourhood
		// {
		// String linkNeighbourhoodStr = config.findParam("bse",
		// "linkNeighbourhood");
		// if (linkNeighbourhoodStr != null) {
		// int linkNeighbourhood = Integer.parseInt(linkNeighbourhoodStr);
		// System.out.println("BSE:\tlinkNeighbourhood\t= "
		// + linkNeighbourhood);
		// calibrator.setLinkNeighbourhood(linkNeighbourhood);
		// } else {
		// System.out.println("BSE:\tlinkNeighbourhood\t= default value\t"
		// + Calibrator.DEFAULT_LINK_NEIGHBOURHOOD);
		// }
		// }
		// SET proportionalAssignment
		{
			String proportionalAssignmentStr = config.findParam("bse",
					"proportionalAssignment");
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
		// SET pcaCoverage
		// {
		// String pcaCoverageStr = config.findParam("bse", "pcaCoverage");
		// if (pcaCoverageStr != null) {
		// double pcaCoverage = Double.parseDouble(pcaCoverageStr);
		// System.out.println("BSE:\tpcaCoverage\t= " + pcaCoverage);
		// calibrator.setPcaCoverage(pcaCoverage);
		// } else
		// System.out.println("BSE:\tpcaCoverage\t= default value\t"
		// + Calibrator.DEFAULT_PCA_COVERAGE);
		// }
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

		// prepare events2planStep
		events2PlanStep = new EventsToPlanSteps(network, ctl.getPopulation());

		// set up a/r-strategy
		((BseUCStrategyManager) ctl.getStrategyManager()).init(calibrator, ctl
				.getTravelTimeCalculator(), events2PlanStep);

		// events addhandler e2p
		events.addHandler(events2PlanStep);
		// prepare resultsContainer
		resultsContainer = new SimResultsContainerImpl();

	}

	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		events2PlanStep.reset(event.getIteration());
	}

	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		events2PlanStep.clearUpAfterMobsim();
		calibrator.afterNetworkLoading(resultsContainer);
		int iter = event.getIteration();
		if (iter % 10 == 0) {
			Controler ctl = event.getControler();
			ControlerIO io = ctl.getControlerIO();
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
	}

	public void setWriteQGISFile(final boolean writeQGISFile) {
		this.writeQGISFile = writeQGISFile;
	}

	// INNER CLASS
	/* package */class SimResultsContainerImpl implements SimResults<Link> {

		/***/
		private static final long serialVersionUID = 1L;

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
