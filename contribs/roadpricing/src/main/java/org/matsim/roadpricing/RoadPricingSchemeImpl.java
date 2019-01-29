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
	
	private static Logger log = Logger.getLogger( RoadPricingSchemeImpl.class ) ;

	private Map<Id<Link>, List<Cost>> linkIds = null;

	private String name = null;
	private String type = null;
	private String description = null;
	private final ArrayList<Cost> costs ;

	private boolean cacheIsInvalid = true;
	private Cost[] costCache = null;

	public RoadPricingSchemeImpl() {
		this.linkIds = new HashMap<Id<Link>, List<Cost>>();
		this.costs = new ArrayList<Cost>();
	}

	public void addLink(final Id<Link> linkId) {
		this.linkIds.put(linkId, null);
	}

	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	public void setType(final String type) {
		this.type = type.intern();
	}

	@Override
	public String getType() {
		return this.type;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	private static int wrnCnt = 0 ;
	
	/**
	 * This is (if I am right) adding a possible toll for <i>all</i> links.  kai, oct'14
	 */
	public Cost createAndAddCost(final double startTime, final double endTime, final double amount) {
		if ( startTime==0. && endTime == 24.*3600. ) {
			if (wrnCnt < 1) {
				wrnCnt++ ;
				log.warn("startTime=0:00 and endTime=24:00 means NO toll after 24h (no wrap-around); make sure this is what you want" ) ;
				if ( wrnCnt==1 ) {
					log.warn( Gbl.ONLYONCE ) ;
				}
			}
		}
		Cost cost = new Cost(startTime, endTime, amount);
		this.costs.add(cost);
		this.cacheIsInvalid = true;
		return cost;
	}

	public void addLinkCost(Id<Link> linkId, double startTime, double endTime, double amount) {
		if ( startTime==0. && endTime == 24.*3600. ) {
			if (wrnCnt < 1) {
				wrnCnt++ ;
				log.warn("startTime=0:00 and endTime=24:00 means NO toll after 24h (no wrap-around); make sure this is what you want" ) ;
				if ( wrnCnt==1 ) {
					log.warn( Gbl.ONLYONCE ) ;
				}
			}
		}
		Cost cost = new Cost(startTime, endTime, amount);
		List<Cost> cs = this.linkIds.get(linkId);
		if (cs == null) {
			cs = new ArrayList<Cost>();
			this.linkIds.put(linkId, cs);
		}
		cs.add(cost);
	}

	public boolean removeCost(final Cost cost) {
		this.cacheIsInvalid = true; // added this without testing it.  kai, nov'13
		return this.costs.remove(cost);
	}

	public boolean removeLinkCost(final Id<Link> linkId, final Cost cost){
		List<Cost> c = this.linkIds.get(linkId);
		return (c != null) ? c.remove(cost) : false;
	}

	@Override
	public Set<Id<Link>> getTolledLinkIds() {
		return this.linkIds.keySet();
	}
	@Override
	public Map<Id<Link>, List<Cost>> getTypicalCostsForLink(){
		return this.linkIds;
	}
	@Override
	public Iterable<Cost> getTypicalCosts() {
		return this.costs;
	}

	/** @return all Cost objects as an array for faster iteration. */
	public Cost[] getCostArray() {
		if (this.cacheIsInvalid) buildCache();
		return this.costCache.clone();
	}

	@Override
	public Cost getLinkCostInfo(final Id<Link> linkId, final double time, Id<Person> personId, Id<Vehicle> vehicleId) {
		// this is the default road pricing scheme, which ignores the person.  kai, mar'12
		// Now also added vehicleId as an argument, which is also ignored at the default level. kai, apr'14

		if (this.cacheIsInvalid) buildCache(); //(*)

		if (this.linkIds.containsKey(linkId)) {
			// (linkId is in contained in list of tolled links)
			
			List<Cost> linkSpecificCosts = this.linkIds.get(linkId);
			if (linkSpecificCosts == null) {
				// (It is expected to have links in the map with null as "value")
				// no link specific info found, apply "general" cost (which is in costCache after (*)):
				for (Cost cost : this.costCache) {
					if ((time >= cost.startTime) && (time < cost.endTime)) {
						return cost;
					}
				}
			}
			else {
				for (Cost cost : linkSpecificCosts){
					if ((time >= cost.startTime) && (time < cost.endTime)) {
						return cost;
					}
				}
			}
		}
		return null;
	}
	
	@Override
	public Cost getTypicalLinkCostInfo( Id<Link> linkId, double time ) {
		return this.getLinkCostInfo( linkId, time, null, null ) ;
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
		
		@Override
		public String toString() {
			return "startTime: " + this.startTime + " endTime: " + this.endTime + " amount: " + this.amount ;
		}
	}
}
