/* *********************************************************************** *
 * project: org.matsim.*
 * CountsComparisonAlgorithm.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.yu.counts.pt;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.counts.Count;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.CountSimComparisonImpl;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;

import playground.yu.analysis.pt.OccupancyAnalyzer;

/**
 * This algorithm can be used to obtain a List of CountSimComparison objects
 * from the LinkAttributes of a run. A time filter can be specified.
 * 
 * @author dgrether
 */
public class PtBoardCountsComparisonAlgorithm {
	/**
	 * The LinkAttributes of the simulation
	 */
	private final OccupancyAnalyzer oa;
	/**
	 * The counts object
	 */
	private Counts counts;
	/**
	 * The result list
	 */
	private final List<CountSimComparison> countSimComp;

	private Node distanceFilterNode = null;

	private Double distanceFilter = null;

	private final Network network;

	private double countsScaleFactor;

	private final static Logger log = Logger
			.getLogger(PtBoardCountsComparisonAlgorithm.class);

	public PtBoardCountsComparisonAlgorithm(final OccupancyAnalyzer oa,
			final Counts counts, final Network network) {
		this.oa = oa;
		this.counts = counts;
		this.countSimComp = new ArrayList<CountSimComparison>();
		this.network = network;
		if (Gbl.getConfig() != null) {
			this.countsScaleFactor = Gbl.getConfig().counts()
					.getCountsScaleFactor();
		} else {
			this.countsScaleFactor = 1.0;
		}
	}

	/**
	 * Creates the List with the counts vs sim values stored in the
	 * countAttribute Attribute of this class.
	 */
	private void compare() {
		double countValue;

		for (Count count : this.counts.getCounts().values()) {
			if (!isInRange(count.getLocId())) {
				continue;
			}
			int[] volumes = this.oa.getBoardVolumesForStop(count.getLocId());
			if (volumes.length == 0) {
				log.warn("No volumes for link: " + count.getLocId().toString());
				continue;
			}
			for (int hour = 1; hour <= 24; hour++) {
				// real volumes:
				Volume volume = count.getVolume(hour);
				if (volume != null) {
					countValue = volume.getValue();
					double simValue = volumes[hour - 1];
					simValue *= this.countsScaleFactor;
					this.countSimComp.add(new CountSimComparisonImpl(count
							.getLocId(), hour, countValue, simValue));
				} else {
					countValue = 0.0;
				}
			}
		}
	}

	/**
	 * 
	 * @param linkid
	 * @return
	 *         <code>true</true> if the Link with the given Id is not farther away than the
	 * distance specified by the distance filter from the center node of the filter.
	 */
	private boolean isInRange(final Id linkid) {
		if ((this.distanceFilterNode == null) || (this.distanceFilter == null)) {
			return true;
		}
		Link l = this.network.getLinks().get(linkid);
		if (l == null) {
			log.warn("Cannot find requested link: " + linkid.toString());
			return false;
		}
		double dist = CoordUtils.calcDistance(l.getCoord(),
				this.distanceFilterNode.getCoord());
		return dist < this.distanceFilter.doubleValue();
	}

	/**
	 * 
	 * @return the result list
	 */
	public List<CountSimComparison> getComparison() {
		return this.countSimComp;
	}

	public void run() {
		this.compare();
	}

	/**
	 * Set a distance filter, dropping everything out which is not in the
	 * distance given in meters around the given Node Id.
	 * 
	 * @param distance
	 * @param nodeId
	 */
	public void setDistanceFilter(final Double distance, final String nodeId) {
		this.distanceFilter = distance;
		this.distanceFilterNode = this.network.getNodes().get(
				new IdImpl(nodeId));
	}

	public void setCountsScaleFactor(final double countsScaleFactor) {
		this.countsScaleFactor = countsScaleFactor;
	}
}
