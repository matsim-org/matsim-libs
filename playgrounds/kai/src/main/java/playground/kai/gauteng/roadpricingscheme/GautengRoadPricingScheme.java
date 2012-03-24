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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.roadpricing.RoadPricingSchemeI;
import org.matsim.roadpricing.RoadPricingScheme.Cost;

/**
 * @author nagel
 *
 */
public class GautengRoadPricingScheme implements RoadPricingSchemeI {
	private RoadPricingSchemeI delegate = null ;
	private Network network;
	private Population population ;
	private final double FACTOR = 100. ;
	
	public GautengRoadPricingScheme( RoadPricingSchemeI inputRoadPricingScheme, Network network, Population population ) {
		this.delegate = inputRoadPricingScheme ;
		this.network = network ;
		this.population = population ; 
		if ( FACTOR != 1. ) { 
			Logger.getLogger(this.getClass()).error("artificially inflating toll by: " + FACTOR ) ;
		}			
	}

	public String getDescription() {
		return delegate.getDescription();
	}

	@Override
	public Cost getLinkCostInfo(Id linkId, double time, Id personId) {
		Cost baseToll = delegate.getLinkCostInfo(linkId, time, personId );
		if (baseToll == null) {
			return new Cost(0.,24*3600.,0.0) ;
		}
		Link link = network.getLinks().get(linkId) ;
//		System.err.println( " link: " + link ) ;
		Person person = population.getPersons().get(personId) ;
//		System.err.println( " person: " + person ) ;
		final double tollFactor = SanralTollFactor.getTollFactor(person, link.getId(), time);
//		System.err.println( " toll factor: " + tollFactor ) ;
		return new Cost( baseToll.startTime, baseToll.endTime, FACTOR * baseToll.amount * tollFactor );
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
