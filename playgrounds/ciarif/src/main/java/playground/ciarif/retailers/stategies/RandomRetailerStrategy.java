/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.ciarif.retailers.stategies;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.facilities.ActivityFacilityImpl;

import playground.ciarif.retailers.data.LinkRetailersImpl;
import playground.ciarif.retailers.utils.Utils;

import java.util.Map;
import java.util.TreeMap;

public class RandomRetailerStrategy extends RetailerStrategyImpl
{
	private final static Logger log = Logger.getLogger(RandomRetailerStrategy.class);

	private TreeMap<Id<ActivityFacility>, ActivityFacility> movedFacilities = new TreeMap<>();

	public RandomRetailerStrategy(Controler controler) {
		super(controler);
	}


	@Override
	public Map<Id<ActivityFacility>, ActivityFacility> moveFacilities(Map<Id<ActivityFacility>, ActivityFacility> facilities, Map<Id<Link>, LinkRetailersImpl> freeLinks)
	{
		log.info("available Links are= " + freeLinks);
		log.info("The facilities are= " + facilities);
		this.retailerFacilities=facilities;
		for (ActivityFacility f : this.retailerFacilities.values())
		{
			int rd = MatsimRandom.getRandom().nextInt(freeLinks.size());
			LinkRetailersImpl newLink =(LinkRetailersImpl) freeLinks.values().toArray()[rd];
            LinkRetailersImpl oldLink = new LinkRetailersImpl(this.controler.getScenario().getNetwork().getLinks().get(f.getLinkId()), controler.getScenario().getNetwork(), 0.0, 0.0);
			Utils.moveFacility((ActivityFacilityImpl) f,newLink);
			freeLinks.put(oldLink.getId(),oldLink );
			this.movedFacilities.put(f.getId(),f);
		}
		return this.movedFacilities;
	}
}
