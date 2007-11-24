/* *********************************************************************** *
 * project: org.matsim.*
 * RouterNode.java
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

package teach.multiagent07.router;

import java.util.Comparator;

import teach.multiagent07.net.CANode;

public class RouterNode extends CANode {

	public RouterNode(String id) {
		super(id);
		// TODO Auto-generated constructor stub
	}

	public static class CostComparator implements Comparator<RouterNode> {

		public int compare(RouterNode n1, RouterNode n2) {
			if (n1.cost_ < n2.cost_) {
				return -1;
			} else if (n1.cost_ == n2.cost_) {
				return 0;
			} else {
				return +1;
			}
		}
	};

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private boolean visited_ = false;
	private RouterNode prev_ = null;
	private double cost_ = 0;
	private double time_ = 0;
	public boolean isHandled = false;

	/**
	 * @param comingFrom
	 * @param cost
	 * @param time
	 * @return true if the visited-flag or the arrival time changed, false otherwise
	 */
	public boolean visit(RouterNode comingFrom, double cost, double time) {
//		if (!visited_) {
			visited_ = true;
			prev_ = comingFrom;
			cost_ = cost;
			time_ = time;
			return true;
/*		} else if (cost < cost_) {
			prev_ = comingFrom;
			cost_ = cost;
			time_ = time;
			return true;
		} else {
			return false;
		}
*/	}

	public void resetVisited() {
		visited_ = false;
		prev_ = null;
		isHandled = false;
	}

	//////////////////////////////////////////////////////////////////////
	// query methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public boolean isVisited() {
		return visited_;
	}

	public double getCost() {
		return cost_;
	}

	public double getTime() {
		return time_;
	}

	public RouterNode getPrevNode() {
		return prev_;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return super.toString() +
				"[time=" + this.time_ + "]" +
				"[cost=" + this.cost_ + "]" +
				"[visited=" + this.visited_ + "]";
	}

	//////////////////////////////////////////////////////////////////////
	// other methods
	//////////////////////////////////////////////////////////////////////
	public static Comparator<RouterNode> getCostComparator() {
		return new CostComparator();
	}

	public static Comparator<RouterNode> getTimeComparator() {
		return new Comparator<RouterNode>() {
			public int compare(RouterNode n1, RouterNode n2) {
				if (n1.time_ < n2.time_) {
					return -1;
				} else if (n1.time_ == n2.time_) {
					return 0;
				} else {
					return +1;
				}
			}
		};
	}

}
