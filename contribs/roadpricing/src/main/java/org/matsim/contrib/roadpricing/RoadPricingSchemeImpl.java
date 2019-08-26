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

package org.matsim.contrib.roadpricing;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.vehicles.Vehicle;

/**
 * A road pricing scheme (sometimes also called toll scheme) contains the type of the toll, a list of the
 * tolled links and the (time-dependent) toll amount agents have to pay.
 *
 * @author mrieser
 */
public final class RoadPricingSchemeImpl implements RoadPricingScheme {
	// currently needs to be public. kai, sep'14

	private static Logger log = Logger.getLogger(RoadPricingSchemeImpl.class);

	private Map<Id<Link>, List<RoadPricingCost>> linkIds;

	private String name = null;
	private String type = null;
	private String description = null;
	private final ArrayList<RoadPricingCost> costs;

	private boolean cacheIsInvalid = true;
	private RoadPricingCost[] costCache = null;

	RoadPricingSchemeImpl() {
		this.linkIds = new HashMap<>();
		this.costs = new ArrayList<>();
	}

	void addLink(final Id<Link> linkId) {
		this.linkIds.put(linkId, null);
	}

	void setName(final String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	void setType(final String type) {
		this.type = type.intern();
	}

	@Override
	public String getType() {
		return this.type;
	}

	void setDescription(final String description) {
		this.description = description;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	private static int wrnCnt = 0;

	/**
	 * This is (if I am right) adding a possible toll for <i>all</i> links.  kai, oct'14
	 */
	RoadPricingCost createAndAddCost( final double startTime, final double endTime, final double amount ) {
		warnAboutNoToll(startTime, endTime);
		RoadPricingCost cost = new RoadPricingCost(startTime, endTime, amount);
		this.costs.add(cost);
		this.cacheIsInvalid = true;
		return cost;
	}

	void addLinkCost(Id<Link> linkId, double startTime, double endTime, double amount) {
		warnAboutNoToll(startTime, endTime);
		RoadPricingCost cost = new RoadPricingCost(startTime, endTime, amount);
		List<RoadPricingCost> cs = this.linkIds.computeIfAbsent(linkId, k -> new ArrayList<>() );
		cs.add(cost);
		Collections.sort(cs);
	}

	private void warnAboutNoToll(double startTime, double endTime) {
		if (startTime == 0. && endTime == 24. * 3600.) {
			if (wrnCnt < 1) {
				wrnCnt++;
				log.warn("startTime=0:00 and endTime=24:00 means NO toll after 24h (no wrap-around); make sure this is what you want");
				if (wrnCnt == 1) {
					log.warn(Gbl.ONLYONCE);
				}
			}
		}
	}

	void removeCost(final RoadPricingCost cost ) {
		this.cacheIsInvalid = true; // added this without testing it.  kai, nov'13
		this.costs.remove(cost);
	}

	@SuppressWarnings("SimplifiableConditionalExpression")
	@Deprecated
	boolean removeLinkCost(final Id<Link> linkId, final RoadPricingCost cost ) {
		List<RoadPricingCost> c = this.linkIds.get(linkId );
		return (c != null) ? c.remove(cost) : false;
	}

	@Override
	public final Set<Id<Link>> getTolledLinkIds() {
		return this.linkIds.keySet();
	}

	@Override
	public final Map<Id<Link>, List<RoadPricingCost>> getTypicalCostsForLink() {
		return this.linkIds;
	}

	@Override
	public final Iterable<RoadPricingCost> getTypicalCosts() {
		return this.costs;
	}

	/**
	 * @return all Cost objects as an array for faster iteration.
	 */
	RoadPricingCost[] getCostArray() {
		if (this.cacheIsInvalid) buildCache();
		return this.costCache.clone();
	}

	@Override
	public RoadPricingCost getLinkCostInfo( final Id<Link> linkId, final double time, Id<Person> personId, Id<Vehicle> vehicleId ) {
		// this is the default road pricing scheme, which ignores the person.  kai, mar'12
		// Now also added vehicleId as an argument, which is also ignored at the default level. kai, apr'14

		if (this.cacheIsInvalid) buildCache(); //(*)

		if (this.linkIds.containsKey(linkId)) {
			// (linkId is contained in list of tolled links)

			List<RoadPricingCost> linkSpecificCosts = this.linkIds.get(linkId );
			if (linkSpecificCosts == null) {
				// (It is expected to have links in the map with null as "value")
				// no link specific info found, apply "general" cost (which is in costCache after (*)):
				for ( RoadPricingCost cost : this.costCache) {
					if ((time >= cost.startTime) && (time < cost.endTime)) {
						return cost;
					}
				}
			} else {
				for ( RoadPricingCost cost : linkSpecificCosts) {
					if ((time >= cost.startTime) && (time < cost.endTime)) {
						return cost;
					}
				}
			}
		}
		return null;
	}

	@Override
	public RoadPricingCost getTypicalLinkCostInfo( Id<Link> linkId, double time ) {
		return this.getLinkCostInfo(linkId, time, null, null);
	}

	private void buildCache() {
		this.costCache = new RoadPricingCost[this.costs.size()];
		this.costCache = this.costs.toArray(this.costCache);
		this.cacheIsInvalid = false;
	}

}
