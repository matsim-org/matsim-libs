/* *********************************************************************** *
 * project: org.matsim.*
 * PathSizeFromPlanChoiceSet.java
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
package playground.yu.choiceSetGeneration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.yu.utils.container.CollectionSum;
import playground.yu.utils.math.SimpleStatistics;

/**
 * @author yu
 * 
 */
public class PathSizeFromPlanChoiceSet {
	public static class Path {
		private List<List<Id>> legs;
		private List<String> legModeChain = new ArrayList<String>();
		private List<Double> legLinearDistanceChain = new ArrayList<Double>();
		public Id personId;

		/**
		 * converts {@code Plan} to a {@code Path}
		 * 
		 * @param plan
		 */
		public Path(Plan plan) {
			legs = new ArrayList<List<Id>>();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					PlanImpl planImpl = (PlanImpl) plan;
					legLinearDistanceChain.add(CoordUtils.calcDistance(planImpl
							.getPreviousActivity(leg).getCoord(), planImpl
							.getNextActivity(leg).getCoord()));
					addLeg(leg);
					legModeChain.add(leg.getMode());
				}
			}
			personId = plan.getPerson().getId();
		}

		public void addLeg(Leg leg) {
			if (leg.getMode().equals(TransportMode.car)) {
				legs.add(((NetworkRoute) leg.getRoute()).getLinkIds());
			} else/* non-car */{
				legs.add(new ArrayList<Id>());
			}
		}

		public boolean containLinkId(Id linkId, int LegIndex) {
			return legs.get(LegIndex).contains(linkId);
		}

		public int getLegsSize() {
			return legs.size();
		}

		public List<Id> getLegLinkIds(int legIndex) {
			return legs.get(legIndex);
		}

		public List<String> getLegModeChain() {
			return legModeChain;
		}

		public List<Double> getLegLinearDistanceChain() {
			return legLinearDistanceChain;
		}
	}

	private final Network network;
	private final List<? extends Plan> planChoiceSet;
	private double[] pathSizes;
	private final List<Path> paths;

	public PathSizeFromPlanChoiceSet(Network network, List<? extends Plan> plans) {
		this.network = network;
		planChoiceSet = plans;
		pathSizes = new double[planChoiceSet.size()];
		paths = new ArrayList<Path>();
		init();
		calculatePathSizes();
	}

	/** prepares linkId List for Plans and Legs */
	protected void init() {
		for (Plan plan : planChoiceSet) {
			paths.add(new Path(plan));
		}
	}

	protected void calculatePathSizes() {
		int idx = 0;
		for (Path path : paths) {
			pathSizes[idx++] = calculatePathSize(path);
		}
		recalculatePathSizes();
	}

	private void recalculatePathSizes() {
		Map<Integer/* Plan-idx in choice set */, List<String>/* legModeChain */> legModeChains = new HashMap<Integer, List<String>>();
		Map<Integer/* Plan-idx in choice set */, List<Double>/*
															 * leg linear
															 * distance
															 */> legLinearDistChains = new HashMap<Integer, List<Double>>();

		for (int i = 0; i < pathSizes.length; i++) {
			if (pathSizes[i] < 0) {
				Path path = paths.get(i);
				legModeChains.put(i, path.getLegModeChain());
				legLinearDistChains.put(i, path.getLegLinearDistanceChain());
			}
		}

		int legModeChainsSize = legModeChains.size();
		if (legModeChainsSize == 1) {/* only 1 Plan with path-size = -1 */
			pathSizes[legModeChains.keySet().iterator().next()] = 1d;
		} else if (legModeChainsSize > 1) {/* more Plans with path-size = -1 */
			// double legLinearDistance = CollectionSum
			// .getSum(legLinearDistChains);
			for (Entry<Integer, List<String>> legModeChainEntry : legModeChains
					.entrySet()) {
				List<String> legModeChain = legModeChainEntry.getValue();
				double pathSize = 0;
				Integer planIdx = legModeChainEntry.getKey();

				List<Double> legLinearDistChain = legLinearDistChains
						.get(planIdx);

				for (int i = 0; i < legModeChain.size(); i++) {
					String mode = legModeChain.get(i);

					int modeCnt = 0;
					for (List<String> otherLegModeChain : legModeChains
							.values()) {
						if (otherLegModeChain.size() == legModeChain.size()) {
							if (otherLegModeChain.get(i).equals(mode)) {
								modeCnt++;
							}
						}
					}

					if (modeCnt == 0) {
						throw new RuntimeException(
								"modeCnt should >= 1. ???One mode of a Leg was even NOT used by the Leg itself.");
					}

					pathSize += legLinearDistChain.get(i) / modeCnt;
				}

				pathSizes[planIdx] = pathSize
						/ CollectionSum.getSum(legLinearDistChain);
				if (Double.isNaN(pathSizes[planIdx])) {
					System.out.println("Person:\t"
							+ paths.get(planIdx).personId
							+ "\nlegLinearDistChain:\t" + legLinearDistChain
							+ "\nplan-index:\t" + planIdx + "\nlegModeChain:\t"
							+ legModeChain);
					throw new RuntimeException("path-size NaN!!!");
				}
			}
		}
	}

	protected double calculatePathSize(Path path) {
		double result = 0d;
		double pathLength = 0d;
		int pathLegsSize = path.getLegsSize();

		for (int legIdx = 0; legIdx < pathLegsSize; legIdx++) {
			for (Id linkId : path.getLegLinkIds(legIdx)) {
				double linkLength = network.getLinks().get(linkId).getLength();
				result += linkLength
						/ getUsingLinkCount(linkId, legIdx, pathLegsSize);
				pathLength += linkLength;
			}
		}

		return pathLength > 0 ? result / pathLength : -1d;
	}

	protected int getUsingLinkCount(Id linkId, int legIndex, int pathLegsSize) {
		int usingCnt = 0;
		for (Path otherPath : paths) {
			if (otherPath.getLegsSize() == pathLegsSize) {
				if (otherPath.containLinkId(linkId, legIndex)) {
					usingCnt++;
				}
			}
		}
		return usingCnt;

	}

	public double getPlanPathSize(Plan plan) {
		if (!planChoiceSet.contains(plan)) {
			throw new RuntimeException(
					"This plan does NOT exist in choice set!!!");
		}
		return pathSizes[planChoiceSet.indexOf(plan)];
	}

	public double getMaxPathSize() {
		return SimpleStatistics.max(pathSizes);
	}

	public double getMinPathSize() {
		return SimpleStatistics.min(pathSizes);
	}

	public double getAvgPathSize() {
		return SimpleStatistics.average(pathSizes);
	}

	public List<Double> getAllPathSizes() {
		List<Double> allPathSizes = new ArrayList<Double>(pathSizes.length);
		for (double ps : pathSizes) {
			allPathSizes.add(ps);
		}
		return allPathSizes;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

}
