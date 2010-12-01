/* *********************************************************************** *
 * project: org.matsim.*
 * MarginalTravelCostCalculatorIII.java
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
package playground.gregor.sims.socialcostII;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.misc.IntegerCache;

public class MarginalTravelCostCalculatorIII implements PersonalizableTravelCost {
	
	
	private final TravelCost sc;
	private final TravelTimeCalculator tc;
	private final int binSize;

	public MarginalTravelCostCalculatorIII(final TravelTimeCalculator tc, final TravelCost sc, final int binSize) {
		this.tc = tc;
		this.sc = sc;
		this.binSize = binSize;
	}

	public double getLinkGeneralizedTravelCost(final Link link, double time) {
//		double t2, t1;
//		Integer k1;
//		double diff = time % this.binSize;
//		if (diff > this.binSize/2) {
//			k1 = getTimeBin(time);
//		} else {
//			k1 = getTimeBin(time - this.binSize);
//		}
//
//		t1 = k1 * this.binSize + this.binSize/2;
////		k2 = k1 +1;
//		t2 = t1 + this.binSize;
//		double w = (t2 - time) / (double)this.binSize;
//		
//		double tt1 = this.tc.getLinkTravelTime(link, t1);
//		double tt2 = this.tc.getLinkTravelTime(link, t2);
//		double t = w * tt1 + (1-w) * tt2;
//		double s = w * this.sc.getSocialCost(link, t1) + (1-w) * this.sc.getSocialCost(link, t2);
		double t = this.tc.getLinkTravelTime(link, time);
		double s = 0; //this.sc.getSocialCost(link, time);
		double cost = t+s;
//		if (cost < 0) {
//			System.err.println("negative cost:" + cost);
//		} else if (Double.isNaN(cost)) {
//			System.err.println("nan cost:" + cost);
//		} else if (Double.isInfinite(cost)) {
//			System.err.println("infinite cost:" + cost);
//		} else if (cost > 10000) {
//			System.out.println("verry high cost:" + cost);
//		}
		return cost;
	}
	private Integer getTimeBin(double time) {
		int slice = ((int) time)/this.binSize;
		return IntegerCache.getInteger(slice);
	}

	@Override
	public void setPerson(Person person) {
		// TODO Auto-generated method stub
		
	}
	
	
	
}
