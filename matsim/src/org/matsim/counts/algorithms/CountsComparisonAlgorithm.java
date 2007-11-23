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

package org.matsim.counts.algorithms;

import java.util.List;
import java.util.Vector;

import org.matsim.analysis.CalcLinkStats;
import org.matsim.counts.Count;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.CountSimComparisonImpl;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.identifiers.IdI;

/**
 * This algorithm can be used to obtain a List of CountSimComparison objects from the
 * LinkAttributes of a run.
 * A time filter can be specified.
 *
 * @author dgrether
 */
public class CountsComparisonAlgorithm extends CountsAlgorithm {
	/**
	 * The LinkAttributes of the simulation
	 */
	private final CalcLinkStats linkStats;
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

	private final NetworkLayer network;

	private double countsScaleFactor;

	public CountsComparisonAlgorithm(final CalcLinkStats linkStats, final Counts counts, final NetworkLayer network) {
		this.linkStats = linkStats;
		this.counts = counts;
		this.countSimComp = new Vector<CountSimComparison>();
		this.network = network;
		this.countsScaleFactor = Gbl.getConfig().counts().getCountsScaleFactor();
	}
	/**
	 * Creates the List with the counts vs sim values stored in the
	 * countAttribute Attribute of this class.
	 *
	 */
	private void compare() {
		double countValue;

		for (Count count : this.counts.getCounts().values()) {
			if (!isInRange(count.getLocId())) {
				continue;
			}
			double[] volumes = this.linkStats.getAvgLinkVolumes(count.getLocId().toString());
			if (volumes== null) {
				Gbl.warningMsg(CountsComparisonAlgorithm.class, "compare()", "No volumes for link: " + count.getLocId().toString());
				continue;
			}
			for (int hour = 1; hour <= 24; hour++) {
				// real volumes:
				Volume volume = count.getVolume(hour);
				if (volume != null) {
					countValue = volume.getValue();
				} else {
					countValue = 0.0;
				}
				double simValue=volumes[hour-1];
				simValue *= this.countsScaleFactor;
				this.countSimComp.add(new CountSimComparisonImpl(count.getLocId(), hour, countValue, simValue));
			}
		}
	}

	/**
	 *
	 * @param linkid
	 * @return true if the Link with the given Id is not farther away than the
	 * distance specified by the distance filter from the center node of the filter.
	 */
	private boolean isInRange(final IdI linkid) {
		if (this.distanceFilterNode == null || this.distanceFilter == null) {
			return true;
		}
		Link l = this.network.getLink(linkid);
		if (l == null) {
			Gbl.warningMsg(this.getClass(), "isInRange", "Cannot find requested link: " + linkid.toString());
			return false;
		}
		double dist = l.getCenter().calcDistance(this.distanceFilterNode.getCoord());
		return dist < this.distanceFilter.doubleValue();
	}

	/**
	 *
	 * @return the result list
	 */
	public List<CountSimComparison> getComparison() {
		return this.countSimComp;
	}

	@Override
	public void run(final Counts counts) {
		this.counts = counts;
		this.compare();
	}
	/**
	 * Set a distance filter, dropping everything out which is not in the
	 * distance given in meters around the given Node Id.
	 * @param distance
	 * @param nodeId
	 */
	public void setDistanceFilter(final Double distance, final String nodeId) {
		this.distanceFilter = distance;
	  this.distanceFilterNode = this.network.getNode(nodeId);
	}

	public void setCountsScaleFactor(final double countsScaleFactor) {
		this.countsScaleFactor = countsScaleFactor;
	}
}
