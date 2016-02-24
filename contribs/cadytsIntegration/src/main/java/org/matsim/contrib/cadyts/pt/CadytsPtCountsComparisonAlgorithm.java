/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.cadyts.pt;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.counts.Count;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.CountSimComparisonImpl;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.pt.counts.SimpleWriter;

/**
 * This is a modified copy of CountsComparisonAlgorithm, in order to realize the same functionality
 * for pt counts.
 */
class CadytsPtCountsComparisonAlgorithm {
	/**
	 * The StopAttributes of the simulation
	 */
	private final CadytsPtOccupancyAnalyzer oa;
	/**
	 * The counts object
	 */
	Counts <Link>counts;
	// needed in CadytsErrorPlot

	/**
	 * The result list
	 */
	private final List<CountSimComparison> countSimComp;

	private Node distanceFilterNode = null;

	private Double distanceFilter = null;

	private final Network network;

	double countsScaleFactor; // needed in CadytsErrorPlot

	final static Logger log = Logger.getLogger(CadytsPtCountsComparisonAlgorithm.class);

	StringBuffer content = new StringBuffer();

	CadytsPtCountsComparisonAlgorithm(final CadytsPtOccupancyAnalyzer oa, final Counts<Link> counts, final Network network, final double countsScaleFactor) {
		this.oa = oa;
		this.counts = counts;
		this.countSimComp = new ArrayList<CountSimComparison>();
		this.network = network;
		this.countsScaleFactor = countsScaleFactor;
	}

	/**
	 * Creates the List with the counts vs sim values stored in the countAttribute Attribute of this
	 * class.
	 */
	final String STR_NOVOLUMES = "No volumes for stop: ";
	final String STR_STOPID = "StopId :\t";
	final String STR_HEAD = "\nhour\tsimVal\tscaledSimVal\tcountVal\n";
	final char CHR_HT = '\t';
	final char CHR_NL = '\n';

	void calculateComparison() {
		double countValue;
		for (Count count : this.counts.getCounts().values()) {
			Id stopId = count.getLocId();
			if (!isInRange(count.getCoord())) {
				continue;
			}
			int[] volumes = this.getVolumesForStop(stopId);
			if (volumes == null) {
				log.warn(this.STR_NOVOLUMES + stopId);
				continue;
			} else /* volumes!=null */if (volumes.length == 0) {
				log.warn(this.STR_NOVOLUMES + stopId);
				continue;
			}

			this.content.append(this.STR_STOPID);
			this.content.append(stopId.toString());
			this.content.append(this.STR_HEAD);

			for (int hour = 1; hour <= volumes.length; hour++) {
				// real volumes:
				Volume volume = count.getVolume(hour);
				if (volume != null) {

					this.content.append(hour);
					this.content.append(this.CHR_HT);

					countValue = volume.getValue();
					double simValue = volumes[hour - 1];

					this.content.append(simValue);
					this.content.append(this.CHR_HT);

					simValue *= this.countsScaleFactor;

					this.content.append(simValue);
					this.content.append(this.CHR_HT);
					this.content.append(countValue);
					this.content.append(this.CHR_NL);

					this.countSimComp.add(new CountSimComparisonImpl(stopId, hour, countValue, simValue));

				} else {
					countValue = 0.0;
				}

			}
		}
	}

	int[] getVolumesForStop(final Id stopId) {
		return this.oa.getOccupancyVolumesForStop(stopId);
	}

	/**
	 *
	 * @param stopCoord
	 * @return <code>true</true> if the Link with the given Id is not farther away than the
	 * distance specified by the distance filter from the center node of the filter.
	 */
	boolean isInRange(final Coord stopCoord) {
		if ((this.distanceFilterNode == null) || (this.distanceFilter == null)) {
			return true;
		}

		double dist = CoordUtils.calcEuclideanDistance(stopCoord, this.distanceFilterNode.getCoord());
		return dist < this.distanceFilter.doubleValue();
	}

	/**
	 *
	 * @return the result list
	 */
	List<CountSimComparison> getComparison() {
		return this.countSimComp;
	}

	/**
	 * Set a distance filter, dropping everything out which is not in the distance given in meters
	 * around the given Node Id.
	 *
	 * @param distance
	 * @param nodeId
	 */
	void setDistanceFilter(final Double distance, final String nodeId) {
		this.distanceFilter = distance;
		this.distanceFilterNode = this.network.getNodes().get(Id.create(nodeId, Node.class));
	}

	void write(final String outputFilename) {
		final SimpleWriter simpleWriter = new SimpleWriter(outputFilename);
		simpleWriter.write(this.content.toString());
		simpleWriter.close();
	}
}
