/* *********************************************************************** *
 * project: org.matsim.*
 * RebuildCountComparisonFromLinkStatsFile.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
/**
 *
 */
package playground.yu.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.counts.Count;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.CountSimComparisonImpl;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Volume;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;

/**
 * enables {@code Counts} comparison and writing .kmz file to work for each
 * iteration, and tests the counts comparison effects with different
 * countsScaleFactor
 *
 * @author yu
 *
 */
public class RebuildCountComparisonFromLinkStatsFile {
	protected class CountsComparisonAlgorithm {
		/**
		 * The LinkAttributes of the simulation
		 */
		private final CalcLinkStats linkStats;
		/**
		 * The counts object
		 */
		private final Counts counts;
		/**
		 * The result list
		 */
		private final List<CountSimComparison> countSimComp;

		private Node distanceFilterNode = null;

		private Double distanceFilter = null;

		private final Network network;

		private double countsScaleFactor;

		private final Logger log = Logger
				.getLogger(CountsComparisonAlgorithm.class);

		public CountsComparisonAlgorithm(final CalcLinkStats linkStats,
				final Counts counts, final Network network,
				final double countsScaleFactor) {
			this.linkStats = linkStats;
			this.counts = counts;
			countSimComp = new ArrayList<CountSimComparison>();
			this.network = network;
			this.countsScaleFactor = countsScaleFactor;
		}

		/**
		 * Creates the List with the counts vs sim values stored in the
		 * countAttribute Attribute of this class.
		 */
		private void compare() {
			double countValue;

			for (Count count : counts.getCounts().values()) {
				if (!isInRange(count.getLocId())) {
					continue;
				}
				double[] volumes = linkStats
						.getAvgLinkVolumes(count.getLocId());
				if (volumes.length == 0) {
					log.warn("No volumes for link: "
							+ count.getLocId().toString());
					volumes = new double[24];
					// continue;
				}
				for (int hour = 1; hour <= 24; hour++) {
					// real volumes:
					Volume volume = count.getVolume(hour);
					if (volume != null) {
						countValue = volume.getValue();
						double simValue = volumes[hour - 1];
						simValue *= countsScaleFactor;
						countSimComp.add(new CountSimComparisonImpl(count
								.getLocId(), hour, countValue, simValue));
					} else {
						countValue = 0.0;
					}
				}
			}
		}

		/**
		 *
		 * @return the result list
		 */
		public List<CountSimComparison> getComparison() {
			return countSimComp;
		}

		/**
		 *
		 * @param linkid
		 * @return <code>true</true> if the Link with the given Id is not farther away than the
		 * distance specified by the distance filter from the center node of the filter.
		 */
		private boolean isInRange(final Id linkid) {
			if (distanceFilterNode == null || distanceFilter == null) {
				return true;
			}
			Link l = network.getLinks().get(linkid);
			if (l == null) {
				log.warn("Cannot find requested link: " + linkid.toString());
				return false;
			}
			double dist = CoordUtils.calcDistance(l.getCoord(),
					distanceFilterNode.getCoord());
			return dist < distanceFilter.doubleValue();
		}

		public void run() {
			compare();
		}

		public void setCountsScaleFactor(final double countsScaleFactor) {
			this.countsScaleFactor = countsScaleFactor;
		}

		/**
		 * Set a distance filter, dropping everything out which is not in the
		 * distance given in meters around the given Node Id.
		 *
		 * @param distance
		 * @param nodeId
		 */
		public void setDistanceFilter(final Double distance, final String nodeId) {
			distanceFilter = distance;
			distanceFilterNode = network.getNodes().get(new IdImpl(nodeId));
		}
	}

	private final String linkStatsFilename;

	private final double minScaleFactor, maxScaleFactor, scaleFactorInterval;

	private final Logger log = Logger
			.getLogger(RebuildCountComparisonFromLinkStatsFile.class);

	private final Scenario scenario;
	private Config config;
	private Counts counts;

	/**
	 * @param config
	 */
	public RebuildCountComparisonFromLinkStatsFile(String configFilename,
			String linkStatsFilename, double minScaleFactor,
			double maxScaleFactor, double scaleFactorInterval) {
		scenario = ScenarioUtils.loadScenario(ConfigUtils
				.loadConfig(configFilename));
		this.linkStatsFilename = linkStatsFilename;
		this.minScaleFactor = minScaleFactor;
		this.maxScaleFactor = maxScaleFactor;
		this.scaleFactorInterval = scaleFactorInterval;
	}

	public void run() {
		config = scenario.getConfig();

		counts = new Counts();
		new MatsimCountsReader(counts).readFile(config.counts()
				.getCountsFileName());

		// SET COUNTS_SCALE_FACTOR
		for (double scaleFactor = minScaleFactor; scaleFactor <= maxScaleFactor; scaleFactor += scaleFactorInterval) {
			runCountsComparisonAlgorithmAndOutput(scaleFactor);
		}
	}

	private void runCountsComparisonAlgorithmAndOutput(double scaleFactor) {
		CountsConfigGroup countsConfigGroup = config.counts();

		log.info("compare with counts, scaleFactor =\t" + scaleFactor);

		Network network = scenario.getNetwork();

		CalcLinkStats calcLinkStats = new CalcLinkStats(network, scaleFactor
				/ countsConfigGroup.getCountsScaleFactor());
		calcLinkStats.readFile(linkStatsFilename);

		CountsComparisonAlgorithm cca = new CountsComparisonAlgorithm(
				calcLinkStats, counts, network, scaleFactor);

		if (countsConfigGroup.getDistanceFilter() != null
				&& countsConfigGroup.getDistanceFilterCenterNode() != null) {
			cca.setDistanceFilter(countsConfigGroup.getDistanceFilter(),
					countsConfigGroup.getDistanceFilterCenterNode());
		}
		cca.setCountsScaleFactor(scaleFactor);

		cca.run();

		String outputFormat = countsConfigGroup.getOutputFormat();
		if (outputFormat.contains("kml") || outputFormat.contains("all")) {
			ControlerConfigGroup ctlCG = config.controler();

			int iteration = ctlCG.getFirstIteration();
			ControlerIO ctlIO = new ControlerIO(ctlCG.getOutputDirectory(),
					new IdImpl(ctlCG.getRunId()));

			// String filename = ctlIO.getIterationFilename(iteration, "sf"
			// + scaleFactor + "_countscompare" + ".kmz");

			String path = ctlIO.getIterationPath(iteration) + "/sf"
					+ scaleFactor + "/";
			File itDir = new File(path);
			if (!itDir.mkdirs()) {
				if (itDir.exists()) {
					log.info("Iteration directory " + path + " exists already.");
				} else {
					log.info("Could not create iteration directory " + path
							+ ".");
				}
			}
			String filename = path + "/countscompare.kmz";

			CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(
					cca.getComparison(), network,
					TransformationFactory.getCoordinateTransformation(config
							.global().getCoordinateSystem(),
							TransformationFactory.WGS84));
			kmlWriter.setIterationNumber(iteration);
			kmlWriter.writeFile(filename);// biasErrorGraphData.txt will
			// be
			// written here
		}

		log.info("compare with counts, scaleFactor =\t" + scaleFactor);
	}

	/**
	 * @param args
	 *            - args[0] configFilename; args[1] linkstatsFilename, args[2]
	 *            minimum value of countScaleFactor, args[3] maximum vaule of
	 *            countScaleFactor, args[4] incremental interval of
	 *            countScaleFactor
	 */
	public static void main(String[] args) {
		RebuildCountComparisonFromLinkStatsFile rccfls = new RebuildCountComparisonFromLinkStatsFile(
				args[0], args[1], Double.parseDouble(args[2]),
				Double.parseDouble(args[3]), Double.parseDouble(args[4]));
		rccfls.run();

	}
}
