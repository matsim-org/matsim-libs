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

package playground.southafrica.gauteng.roadpricingscheme;

import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.roadpricing.RoadPricingSchemeImpl.Cost;

/**
 * @author nagel
 *
 */
public class GautengRoadPricingScheme implements RoadPricingScheme {
	private RoadPricingScheme delegate = null ;
	private Network network;
	private Population population ;
	private final TollFactorI tollFactor ;
	
	public GautengRoadPricingScheme( String tollLinksFileName, Network network, Population population, TollFactorI tollFactor ) {
		this.network = network ;
		this.population = population ; 
		Logger.getLogger(this.getClass()).warn("for me, using this as cordon toll did not work; using it as new scheme `link' " +
				"toll for the time being.  needs to be debugged?!?!  kai, mar'12") ;
		
		// read the road pricing scheme from file
		RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(scheme);
		try {
			rpReader.parse( tollLinksFileName  );
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		this.delegate = scheme ;
		this.tollFactor = tollFactor ;
	}

	@Override
	public String getDescription() {
		return delegate.getDescription();
	}

	@Override
	public Cost getLinkCostInfo(Id linkId, double time, Id personId) {
		Cost baseToll = delegate.getLinkCostInfo(linkId, time, personId );
		if (baseToll == null) {
//			return new Cost(0.,24*3600.,0.0) ;
			// yyyyyy this is what causes the cordon setting for the Gauteng scenario to fail: It always
			// returns a cost object, and so the algo thinks that the agent is always "inside" the area.
			// I can't say why I programmed it this way; I seem to recall that I copied it from somewhere 
			// but I cannot find it.  kai, apr'12
			// Throws NullPointerException. Changed back to original... just to get SANRAL runs going . jwj, Apr 24 '12/
			return null ;
		}
		Link link = network.getLinks().get(linkId) ;
		Person person = population.getPersons().get(personId) ;
		final double tollFactorVal = tollFactor.getTollFactor(person, link.getId(), time);
		return new Cost( baseToll.startTime, baseToll.endTime, baseToll.amount * tollFactorVal );
	}

	@Override
	public Set<Id> getTolledLinkIds() {
		return delegate.getTolledLinkIds();
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public String getType() {
		return delegate.getType();
//		return "link" ;
	}

}
