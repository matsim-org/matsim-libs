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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
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
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * This algorithm can be used to obtain a List of CountSimComparison objects from the
 * LinkAttributes of a run.
 * A time filter can be specified.
 *
 * @author dgrether
 */
public class CountsComparisonAlgorithm {

	public static interface VolumesForId {
		double[] getVolumesForStop(Id<TransitStopFacility> locationId);
	}
	
	public static interface DistanceFilter {
		boolean isInRange(Count count);
	}

	private final VolumesForId volumesPerLinkPerHour;

	private final Counts<Link> counts;

	private final List<CountSimComparison> result;

	private DistanceFilter distanceFilter = new DistanceFilter() {

		@Override
		public boolean isInRange(Count count) {
			return true;
		}
		
	};

	private final Network network;

	private double countsScaleFactor;

	private final static Logger log = Logger.getLogger(CountsComparisonAlgorithm.class);

	public CountsComparisonAlgorithm(final VolumesAnalyzer volumes, final Counts counts, final Network network, final double countsScaleFactor) {
		this.counts = counts;
		this.result = new ArrayList<CountSimComparison>();
		this.network = network;
		this.countsScaleFactor = countsScaleFactor;
		this.volumesPerLinkPerHour = new VolumesForId() {

			@Override
			public double[] getVolumesForStop(Id<TransitStopFacility> locationId) {
				return volumes.getVolumesPerHourForLink(Id.create(locationId, Link.class));
			}

		};
	}

	public CountsComparisonAlgorithm(final Map<Id<Link>, double[]> volumesPerLinkPerHour, final Counts counts, final Network network, final double countsScaleFactor) {
		this.volumesPerLinkPerHour = new VolumesForId() {

			@Override
			public double[] getVolumesForStop(Id<TransitStopFacility> locationId) {
				return volumesPerLinkPerHour.get(Id.create(locationId, Link.class));
			}

		};
		this.counts = counts;
		this.result = new ArrayList<CountSimComparison>();
		this.network = network;
		this.countsScaleFactor = countsScaleFactor;
	}

	public CountsComparisonAlgorithm(VolumesForId volumesPerLinkPerHour, final Counts counts, final Network network, final double countsScaleFactor) {
		this.volumesPerLinkPerHour = volumesPerLinkPerHour;
		this.counts = counts;
		this.result = new ArrayList<CountSimComparison>();
		this.network = network;
		this.countsScaleFactor = countsScaleFactor;
	}

	/**
	 * Creates the List with the counts vs sim values stored in the
	 * countAttribute Attribute of this class.
	 */
	private void compare() {
		for (Count count : this.counts.getCounts().values()) {
			if (!distanceFilter.isInRange(count)) {
				continue;
			}
			double[] volumes = this.volumesPerLinkPerHour.getVolumesForStop(Id.create(count.getLocId(), TransitStopFacility.class));
			if (volumes == null || volumes.length == 0) {
				log.warn("No volumes for count location: " + count.getLocId().toString());
				continue;
			}
			for (int hour = 1; hour <= 24; hour++) {
				Volume volume = count.getVolume(hour);
				if (volume != null) {
					double countValue = volume.getValue();
					double simValue=volumes[hour-1];
					simValue *= this.countsScaleFactor;
					this.result.add(new CountSimComparisonImpl(count.getLocId(), hour, countValue, simValue));
				}
			}
		}
	}

	/**
	 *
	 * @return the result list
	 */
	public List<CountSimComparison> getComparison() {
		return this.result;
	}

	public void run() {
		this.compare();
	}

	/**
	 * Set a distance filter, dropping everything out which is not in the
	 * distance given in meters around the given Node Id.
	 * @param distance
	 * @param nodeId
	 */
	public void setDistanceFilter(final Double distance, final String nodeId) {
		final Coord centerCoord = network.getNodes().get(Id.create(nodeId, Node.class)).getCoord();
		this.distanceFilter = new DistanceFilter() {

			@Override
			public boolean isInRange(Count count) {
				Link l = network.getLinks().get(count.getLocId());
				if (l == null) {
					log.warn("Cannot find requested link: " + count.getLocId().toString());
					return false;
				}
				double dist = CoordUtils.calcEuclideanDistance(l.getCoord(), centerCoord);
				return dist < distance;
			}
			
		};
	}
	
	public void setCountCoordUsingDistanceFilter(final Double distance, final String nodeId) {
		final Coord centerCoord = network.getNodes().get(Id.create(nodeId, Node.class)).getCoord();
		this.distanceFilter = new CountsComparisonAlgorithm.DistanceFilter() {
			
			@Override
			public boolean isInRange(Count count) {
				double dist = CoordUtils.calcEuclideanDistance(count.getCoord(), centerCoord);
				return dist < distance;
			}
		};
	}
	
	public void setDistanceFilter(DistanceFilter distanceFilter) {
		this.distanceFilter = distanceFilter;
	}

	public void setCountsScaleFactor(final double countsScaleFactor) {
		this.countsScaleFactor = countsScaleFactor;
	}
	
}
