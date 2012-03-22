/* *********************************************************************** *
 * project: kai
 * GautengRoadPricingScheme.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.kai.gauteng.roadpricingscheme;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.roadpricing.RoadPricingSchemeI;
import org.matsim.roadpricing.RoadPricingScheme.Cost;

/**
 * @author nagel
 *
 */
public class GautengRoadPricingScheme implements RoadPricingSchemeI {
	private RoadPricingSchemeI delegate = null ;
	private Network network;
	
	public GautengRoadPricingScheme( RoadPricingSchemeI inputRoadPricingScheme, Network network ) {
		this.delegate = inputRoadPricingScheme ;
		this.network = network ;
	}

	public String getDescription() {
		return delegate.getDescription();
	}

	public Cost getLinkCostInfo(Id linkId, double time, Person person) {
		Cost baseToll = delegate.getLinkCostInfo(linkId, time, null);
		if (baseToll == null) {
			return new Cost(0.,24*3600.,0.0) ;
		}
		Link link = network.getLinks().get(linkId) ;
		return new Cost( baseToll.startTime, baseToll.endTime, 
				baseToll.amount * SanralTollFactor.getTollFactor(person.getId(), link.getId(), time)
						);
	}

	public Map<Id, List<Cost>> getLinkIds() {
		return delegate.getLinkIds();
	}

	public Set<Id> getLinkIdSet() {
		return delegate.getLinkIdSet();
	}

	public String getName() {
		return delegate.getName();
	}

	public String getType() {
		return delegate.getType();
	}


}
