/* *********************************************************************** *
 * project: org.matsim.*
 * RoadPricingScheme.java
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

package org.matsim.roadpricing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;

/**
 * A road pricing scheme (sometimes also called toll scheme) contains the type of the toll, a list of the
 * tolled links and the (time-dependent) toll amount agents have to pay.
 *
 * @author mrieser
 */
public class RoadPricingScheme {

	/** The type to be used for distance tolls. */
	public static final String TOLL_TYPE_DISTANCE = "distance";

	/** The type to be used for cordon tolls. */
	public static final String TOLL_TYPE_CORDON = "cordon";

	/** The type to be used for area tolls. */
	public static final String TOLL_TYPE_AREA = "area";

	private Map<Id, List<Cost>> linkIds = null;

	private String name = null;
	private String type = null;
	private String description = null;
	private ArrayList<Cost> costs = null;

	private boolean cacheIsInvalid = true;
	private Cost[] costCache = null;

	public RoadPricingScheme() {
		this.linkIds = new HashMap<Id, List<Cost>>();
		this.costs = new ArrayList<Cost>();
	}

	public void addLink(final Id linkId) {
		this.linkIds.put(linkId, null);
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setType(final String type) {
		this.type = type.intern();
	}

	public String getType() {
		return this.type;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public String getDescription() {
		return this.description;
	}

	public Cost addCost(final double startTime, final double endTime, final double amount) {
		Cost cost = new Cost(startTime, endTime, amount);
		this.costs.add(cost);
		this.cacheIsInvalid = true;
		return cost;
	}

  public void addLinkCost(Id linkId, double startTime, double endTime,
      double amount) {
    Cost cost = new Cost(startTime, endTime, amount);
    List<Cost> cs = this.linkIds.get(linkId);
    if (cs == null) {
      cs = new ArrayList<Cost>();
      this.linkIds.put(linkId, cs);
    }
    cs.add(cost);
  }
	
	public boolean removeCost(final Cost cost) {
		return this.costs.remove(cost);
	}
	
	public boolean removeLinkCost(final Id linkId, final Cost cost){
	  List<Cost> c = this.linkIds.get(linkId);
	  return (c != null) ? c.remove(cost) : false;
	}

	public Set<Id> getLinkIdSet() {
		return this.linkIds.keySet();
	}

	public Map<Id, List<Cost>> getLinkIds(){
	  return this.linkIds;
	}
	
	public Iterable<Cost> getCosts() {
		return this.costs;
	}

	/** @return all Cost objects as an array for faster iteration. */
	public Cost[] getCostArray() {
		if (this.cacheIsInvalid) buildCache();
		return this.costCache.clone();
	}

	/**
	 * Returns the Cost object that contains the active costs for the given link
	 * at the specified time.
	 *
	 * @param linkId
	 * @param time
	 * @return The cost object for the given link at the specified time,
	 * <code>null</code> if the link is either not part of the tolling scheme
	 * or there is no toll at the specified time for the link.
	 */
	public Cost getLinkCost(final Id linkId, final double time) {
		if (this.cacheIsInvalid) buildCache();
		if (this.linkIds.containsKey(linkId)) {
		  List<Cost> costs = this.linkIds.get(linkId);
		  if (costs == null) {
	      for (Cost cost : this.costCache) {
	        if ((time >= cost.startTime) && (time < cost.endTime)) {
	          return cost;
	        }
	      }
		  }
		  else {
		    for (Cost cost : costs){
          if ((time >= cost.startTime) && (time < cost.endTime)) {
            return cost;
          }
		    }
		  }
		}
		return null;
	}
	

	private void buildCache() {
		this.costCache = new Cost[this.costs.size()];
		this.costCache = this.costs.toArray(this.costCache);
		this.cacheIsInvalid = false;
	}

	/**
	 * A single, time-dependent toll-amount for a roadpricing scheme.
	 *
	 * @author mrieser
	 */
	static public class Cost {
		public final double startTime;
		public final double endTime;
		public final double amount;

		public Cost(final double startTime, final double endTime, final double amount) {
			this.startTime = startTime;
			this.endTime = endTime;
			this.amount = amount;
		}
	}
}
