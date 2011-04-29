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
package playground.yu.counts.scaleFactor.fromLinkStats;

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
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.counts.Count;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Volume;

/**
 * calculates countsScaleFactor meeting different criteria (e.g. min MRE).
 *
 * @author yu
 *
 */
public class CountScaleFactorCalculator {
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

		// /**
		// * Creates the List with the counts vs sim values stored in the
		// * countAttribute Attribute of this class.
		// */
		// private void compare() {
		// double countValue;
		//
		// for (Count count : counts.getCounts().values()) {
		// if (!isInRange(count.getLocId())) {
		// continue;
		// }
		// double[] volumes = linkStats
		// .getAvgLinkVolumes(count.getLocId());
		// if (volumes.length == 0) {
		// log.warn("No volumes for link: "
		// + count.getLocId().toString());
		// volumes = new double[24];
		// // continue;
		// }
		// for (int hour = 1; hour <= 24; hour++) {
		// // real volumes:
		// Volume volume = count.getVolume(hour);
		// if (volume != null) {
		// countValue = volume.getValue();
		// double simValue = volumes[hour - 1];
		// simValue *= countsScaleFactor;
		// countSimComp.add(new CountSimComparisonImpl(count
		// .getLocId(), hour, countValue, simValue));
		// } else {
		// countValue = 0.0;
		// }
		// }
		// }
		// }

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

		// public void run() {
		// compare();
		// }

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

		/**
		 * Creates the List with the counts vs sim values stored in the
		 * countAttribute Attribute of this class.
		 */
		public void getBestScaleFactors(int startHour, int endHour) {
			double simValSquareSum = 0d, countSimProductSum = 0d;// 4 MSB
			double simValSum = 0d, countValSum = 0d;// 4 MB
			double simCntQuotientSum = 0d, simCntQuotientSquareSum = 0d;// 4
																		// MSRE

			if (startHour < 1 || startHour > endHour || endHour > 24) {
				throw new RuntimeException(
						"Illegal start- and endHour for count comparison! [1-24]");
			}

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
				}
				for (int hour = startHour; hour <= endHour; hour++) {
					// real volumes:
					Volume volume = count.getVolume(hour);
					if (volume != null) {
						countValue = volume.getValue();
						double simValue = volumes[hour - 1];
						simValue *= countsScaleFactor;

						// 4 MSB
						simValSquareSum += simValue * simValue;
						countSimProductSum += countValue * simValue;

						// 4 MB
						simValSum += simValue;
						countValSum += countValue;

						// 4 MSRE
						double simCntQuotient = countValue > 0d ? simValue
								/ countValue : 11d;
						simCntQuotientSum += simCntQuotient;
						// see also CountSimComparisonImpl.java
						simCntQuotientSquareSum += simCntQuotient
								* simCntQuotient;
					} else {
						countValue = 0.0;
					}
				}
			}

			if (simValSquareSum <= 0) {
				throw new RuntimeException(
						"sigma(sim^2)==0, no agents were simulated?");
			}

			double a = countsScaleFactor * countSimProductSum / simValSquareSum, b = countsScaleFactor
					* simCntQuotientSum / simCntQuotientSquareSum, c = countsScaleFactor
					* countValSum / simValSum;
			System.out
					.println("best scaleFactor for minimal MSB (mean squred bias) (  sigma[(sim-cnt)^2]  ) could be\t"
							+ a + "\t!");

			System.out
					.println("best scaleFactor for minimal MSRE (mean squred relative errors) ( sigma[(sim/cnt-1)^2] ) could be\t"
							+ b + "\t!");

			System.out
					.println("best scaleFactor for minimal MB (mean bias) (  [sigma(sim-cnt)]^2  ) could be\t"
							+ c + "\t!");

			System.out.println("simSum =\t" + simValSum + "\tcntSum=\t"
					+ countValSum);
			for (double csf = 4d; csf < 7d; csf += 0.05) {
				System.out.println("countScaleFactor =\t" + csf
						+ "\tdifference=\t"
						+ (simValSum * csf / countsScaleFactor - countValSum));
			}
		}
	}

	private final String linkStatsFilename;

	private final Logger log = Logger
			.getLogger(CountScaleFactorCalculator.class);

	private final Scenario scenario;
	private Config config;
	private Counts counts;

	private final int startHour, endHour;

	/**
	 * @param endHour
	 * @param startHour
	 * @param config
	 */
	public CountScaleFactorCalculator(String configFilename,
			String linkStatsFilename, int startHour, int endHour) {
		scenario = ScenarioUtils.loadScenario(ConfigUtils
				.loadConfig(configFilename));
		this.linkStatsFilename = linkStatsFilename;
		this.startHour = startHour;
		this.endHour = endHour;
	}

	public void run() {
		config = scenario.getConfig();

		counts = new Counts();
		CountsConfigGroup countsCG = config.counts();
		new MatsimCountsReader(counts).readFile(countsCG.getCountsFileName());

		double scaleFactor = countsCG.getCountsScaleFactor();
		log.info("compare with counts, scaleFactor =\t" + scaleFactor);

		Network network = scenario.getNetwork();

		CalcLinkStats calcLinkStats = new CalcLinkStats(network, scaleFactor
				/ countsCG.getCountsScaleFactor());
		calcLinkStats.readFile(linkStatsFilename);

		CountsComparisonAlgorithm cca = new CountsComparisonAlgorithm(
				calcLinkStats, counts, network, scaleFactor);

		if (countsCG.getDistanceFilter() != null
				&& countsCG.getDistanceFilterCenterNode() != null) {
			cca.setDistanceFilter(countsCG.getDistanceFilter(),
					countsCG.getDistanceFilterCenterNode());
		}
		cca.setCountsScaleFactor(scaleFactor);

		cca.getBestScaleFactors(startHour, endHour);
	}

	/**
	 * @param args
	 *            - args[0] configFilename; args[1] linkstatsFilename, args[2]
	 *            and args[3] time range of calculating countScaleFactor
	 */
	public static void run(String[] args) {
		CountScaleFactorCalculator rccfls = new CountScaleFactorCalculator(
				args[0], args[1], Integer.parseInt(args[2]),
				Integer.parseInt(args[3]));
		rccfls.run();
	}

	/**
	 * @param args
	 *            - args[0] configFilename; args[1] linkstatsFilename, args[2]
	 *            and args[3] time range of calculating countScaleFactor
	 */
	public static void main(String[] args) {
		// run1(args);
		run(args);
	}
}
