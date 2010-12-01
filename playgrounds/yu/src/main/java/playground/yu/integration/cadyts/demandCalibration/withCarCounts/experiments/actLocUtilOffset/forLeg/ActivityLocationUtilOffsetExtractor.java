/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityLocationUtilOffsetExtractor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.actLocUtilOffset.forLeg;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.ActivityEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.counts.Counts;

import playground.yu.utils.io.SimpleWriter;
import cadyts.utilities.misc.DynamicData;

/**
 * @author yu
 * 
 */
public class ActivityLocationUtilOffsetExtractor implements
		LinkEnterEventHandler {
	protected Network net;
	private Counts counts;
	private DynamicData<Link> linkUtilOffsets;
	protected int caliStartTime, caliEndTime;
	protected Map<Integer/* time */, Map<Coord/* actLoc */, Tuple<Integer/* cnt */, Double/* sum */>>> gridUtilOffsets = new HashMap<Integer, Map<Coord, Tuple<Integer, Double>>>();
	protected Map<Id/* agentId */, Double/* legUtilOffset */> tmpAgentLegUtilOffsets = new HashMap<Id, Double>();
	// time is not needed, because it should be determined by time of activity
	// beginning or ending
	protected Id tmpActLinkId = null;
	private int lowerLimit;
	protected double gridLength;
	protected boolean involveZeroUtilOffset = true;

	public ActivityLocationUtilOffsetExtractor(Network net, Counts counts,
			DynamicData<Link> linkUtilOffsets, int caliStartTime,
			int caliEndTime, int lowerLimit, double gridLength) {
		this.net = net;
		this.counts = counts;
		this.linkUtilOffsets = linkUtilOffsets;
		this.caliStartTime = caliStartTime;
		this.caliEndTime = caliEndTime;
		this.lowerLimit = lowerLimit;
		this.gridLength = gridLength;
	}

	protected double getLinkUtilOffset(Id linkId, int time) {
		if (counts.getCounts().containsKey(linkId)) {
			if (isInRange(linkId, net)) {
				return linkUtilOffsets.getSum(net.getLinks().get(linkId),
						(time - 1) * 3600, time * 3600 - 1);
			}
		}
		return 0d;
	}

	public double getGridLength() {
		return gridLength;
	}

	protected Coord getActLocCoord(Id linkId) {
		return net.getLinks().get(linkId).getCoord();
	}

	protected Coord getGridCenterCoord(Coord coord) {
		Coord center = new CoordImpl((int) coord.getX() / (int) gridLength
				* gridLength + gridLength / 2d, (int) coord.getY()
				/ (int) gridLength * gridLength + gridLength / 2d);
		return center;
	}

	protected Coord getGridCenterCoord(Id linkId) {
		return this.getGridCenterCoord(net.getLinks().get(linkId).getCoord());
	}

	public void setInvolveZeroUtilOffset(boolean involveZeroUtilOffset) {
		this.involveZeroUtilOffset = involveZeroUtilOffset;
	}

	public void output(String outputFilenameBase) {
		for (Entry<Integer, Map<Coord, Tuple<Integer, Double>>> entry : getRetrenchedGridUtilOffsets()
				.entrySet()) {
			// System.out.println("entry:\t" + entry.getKey()
			// + "\tentryValueSize:\t" + entry.getValue().size());
			if (entry.getValue().size() > 0) {
				SimpleWriter writer = new SimpleWriter(outputFilenameBase
						+ entry.getKey() + ".grid.log");
				writer.writeln("x\ty\tavg. locationUtilityOffset\tno. of legs");
				for (Entry<Coord, Tuple<Integer, Double>> guo : entry
						.getValue().entrySet()) {
					Coord center = guo.getKey();
					Tuple<Integer, Double> countSum = guo.getValue();
					int count = countSum.getFirst();
					if (count >= lowerLimit) {
						writer.writeln(center.getX() + "\t" + center.getY()
								+ "\t" + countSum.getSecond() / count + "\t"
								+ count);
						writer.flush();
					}
				}
				writer.close();
			}
		}
	}

	private static boolean isInRange(final Id linkId, final Network net) {
		Coord distanceFilterCenterNodeCoord = net.getNodes().get(
				new IdImpl("2531")).getCoord();
		double distanceFilter = 30000;
		Link l = net.getLinks().get(linkId);
		if (l == null) {
			System.out.println("Cannot find requested link: "
					+ linkId.toString());
			return false;
		}
		return ((LinkImpl) l).calcDistance(distanceFilterCenterNodeCoord) < distanceFilter;
	}

	public void handleEvent(LinkEnterEvent event) {
		Id linkId = event.getLinkId();
		Id agentId = event.getPersonId();
		int timeStep = getTimeStep(event.getTime());

//		if (timeStep >= caliStartTime && timeStep <= caliEndTime) {
			double linkUtilOffset = getLinkUtilOffset(linkId, timeStep);

			Double legUtilOffset = tmpAgentLegUtilOffsets.get(agentId);
			if (legUtilOffset == null) {
				tmpAgentLegUtilOffsets.put(agentId, linkUtilOffset);
			} else {
				tmpAgentLegUtilOffsets.put(agentId, legUtilOffset
						+ linkUtilOffset);
			}
//		}
	}

	protected static int getTimeStep(double time) {
		return (int) time / 3600 + 1;
	}

	public Map<Integer, Map<Coord, Tuple<Integer, Double>>> getGridUtilOffsets() {
		return gridUtilOffsets;
	}

	public void reset(int iteration) {

	}

	/**
	 * save information from event into gridCenterUtilOffsets container
	 * 
	 * @param event
	 */
	protected void internalHandleEvent(ActivityEvent event) {
		if (event != null) {
			Id agentId = event.getPersonId();
			Double legUtilOffset = tmpAgentLegUtilOffsets./**/remove(agentId)/**/;
			if (legUtilOffset != null) {
				int time = getTimeStep(event.getTime());
				Coord actLoc = this.getGridCenterCoord(event.getLinkId());
				// //////////////////////
				if (involveZeroUtilOffset || legUtilOffset != 0d) {
					addGridUtilOffset(time, actLoc, legUtilOffset);
					// /////////////////////////
				}
			}
		}
	}

	protected void addGridUtilOffset(int timeStep, Coord actLoc,
			double legUtilOffset) {
		Map<Coord, Tuple<Integer, Double>> gridUtilOffsetMap = gridUtilOffsets
				.get(timeStep);
		if (gridUtilOffsetMap == null) {
			gridUtilOffsetMap = new HashMap<Coord, Tuple<Integer, Double>>();
			gridUtilOffsets.put(timeStep, gridUtilOffsetMap);
		}

		Tuple<Integer, Double> guoCntSum = gridUtilOffsetMap.get(actLoc);
		if (guoCntSum == null) {
			guoCntSum = new Tuple<Integer, Double>(1, legUtilOffset);
		} else {
			guoCntSum = new Tuple<Integer, Double>(guoCntSum.getFirst() + 1,
					guoCntSum.getSecond() + legUtilOffset);
		}
		gridUtilOffsetMap.put(actLoc, guoCntSum);
	}

	public Map<Integer, Map<Coord, Tuple<Integer, Double>>> getRetrenchedGridUtilOffsets() {
		Map<Integer, Map<Coord, Tuple<Integer, Double>>> tmpGridUtilOffsets = new HashMap<Integer, Map<Coord, Tuple<Integer, Double>>>();
		// tmpGridUtilOffsets.putAll(gridUtilOffsets);
		for (Entry<Integer, Map<Coord, Tuple<Integer, Double>>> gridUtilOffsetsEntry : gridUtilOffsets
				.entrySet()) {
			Map<Coord, Tuple<Integer, Double>> gridUtilOffsetsMap = new HashMap<Coord, Tuple<Integer, Double>>();
			tmpGridUtilOffsets.put(gridUtilOffsetsEntry.getKey(),
					gridUtilOffsetsMap);
			for (Entry<Coord, Tuple<Integer, Double>> coordCntUtilOffsetPair : gridUtilOffsetsEntry
					.getValue().entrySet()) {
				if (coordCntUtilOffsetPair.getValue().getFirst() >= lowerLimit) {
					gridUtilOffsetsMap.put(coordCntUtilOffsetPair.getKey(),
							coordCntUtilOffsetPair.getValue());
				}
			}
		}
		gridUtilOffsets = tmpGridUtilOffsets;
		return tmpGridUtilOffsets;
	}
}
