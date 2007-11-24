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
import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.identifiers.IdI;

public class RoadPricingScheme {

	private NetworkLayer network = null;
	private TreeMap<IdI, Link> links = null;

	private String name = null;
	private String type = null;
	private boolean active = false;
	private String description = null;
	private ArrayList<Cost> costs = null;

	private boolean cacheIsInvalid = true;
	private Cost[] costCache = null;

	public RoadPricingScheme(NetworkLayer network) {
		this.network = network;
		this.links = new TreeMap<IdI, Link>();
		this.costs = new ArrayList<Cost>();
	}

	public void addLink(String linkId) {
		Link link = this.network.getLink(linkId);
		if (link == null) {
			Gbl.errorMsg("Link " + linkId + " for road pricing scheme cannot be found in associated network.");
		}
		this.links.put(link.getId(), link);
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setType(String type) {
		this.type = type.intern();
	}

	public String getType() {
		return this.type;
	}

	public void isActive(final boolean active) {
		this.active = active;
	}

	public boolean isActive() {
		return this.active;
	}

	public void setDescription(String description) {
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

	public boolean removeCost(final Cost cost) {
		return this.costs.remove(cost);
	}

	public Collection<Link> getLinks() {
		return this.links.values();
	}

	public Set<IdI> getLinkIds() {
		return this.links.keySet();
	}

	public Iterable<Cost> getCosts() {
		return this.costs;
	}

	/** @return Returns all Cost objects as an array for faster iteration. */
	public Cost[] getCostArray() {
		if (this.cacheIsInvalid) buildCache();
		return this.costCache;
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
	public Cost getLinkCost(IdI linkId, double time) {
		if (this.cacheIsInvalid) buildCache();
		if (this.links.keySet().contains(linkId)) {
			for (Cost cost : this.costCache) {
				if ((time >= cost.startTime) && (time < cost.endTime)) {
					return cost;
				}
			}
			return null;
		}
		return null;
	}

	private void buildCache() {
		this.costCache = new Cost[this.costs.size()];
		this.costCache = this.costs.toArray(this.costCache);
		this.cacheIsInvalid = false;
	}

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
