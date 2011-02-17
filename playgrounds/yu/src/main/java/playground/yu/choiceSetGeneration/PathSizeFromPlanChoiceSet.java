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
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.routes.NetworkRoute;

import playground.yu.utils.math.SimpleStatistics;

/**
 * @author yu
 * 
 */
public class PathSizeFromPlanChoiceSet {
	public static class Path {
		private List<List<Id>> legs;

		/**
		 * converts {@code Plan} to a {@code Path}
		 * 
		 * @param plan
		 */
		public Path(Plan plan) {
			legs = new ArrayList<List<Id>>();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					addLeg((Leg) pe);
				}
			}
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
	}

	private final Network network;
	private final List<? extends Plan> plans;
	private double[] pathSizes;
	private final List<Path> paths;

	public PathSizeFromPlanChoiceSet(Network network, List<? extends Plan> plans) {
		this.network = network;
		this.plans = plans;
		pathSizes = new double[plans.size()];
		paths = new ArrayList<Path>();
		init();
		calculatePathSizes();
	}

	/** prepares linkId List for Plans and Legs */
	protected void init() {
		for (Plan plan : plans) {
			paths.add(new Path(plan));
		}
	}

	protected void calculatePathSizes() {
		int idx = 0;
		for (Path path : paths) {
			pathSizes[idx++] = calculatePathSize(path);
		}
	}

	protected double calculatePathSize(Path path) {
		double result = 0d;
		double pathLength = 0d;

		for (int legIdx = 0; legIdx < path.getLegsSize(); legIdx++) {
			for (Id linkId : path.getLegLinkIds(legIdx)) {
				double linkLength = network.getLinks().get(linkId).getLength();
				result += linkLength / getUsingLinkCount(linkId, legIdx);
				pathLength += linkLength;
			}
		}

		return pathLength > 0 ? result / pathLength : -1d;
	}

	protected int getUsingLinkCount(Id linkId, int legIndex) {
		int usingCnt = 0;
		for (Path otherPath : paths) {
			if (otherPath.containLinkId(linkId, legIndex)) {
				usingCnt++;
			}
		}
		return usingCnt;

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
