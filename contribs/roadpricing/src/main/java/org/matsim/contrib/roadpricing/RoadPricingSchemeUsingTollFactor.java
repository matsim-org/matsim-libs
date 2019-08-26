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

package org.matsim.contrib.roadpricing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author nagel
 */
public final class RoadPricingSchemeUsingTollFactor implements RoadPricingScheme {
	// needs to be public. kai, sep'14

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(RoadPricingSchemeUsingTollFactor.class);

	private RoadPricingScheme delegate;
	private final TollFactor tollFactor;

	public RoadPricingSchemeUsingTollFactor(RoadPricingScheme scheme, TollFactor tollFactor) {
		this.delegate = scheme;
		this.tollFactor = tollFactor;

	}

	/**
	 *  @param pricingSchemeFileName the absolute path to the road pricing filename.
	 *                              It is important that this must be <i>absolute</i>
	 *                              as we do not have the {@link org.matsim.core.config.Config}
	 *                              to provide context.
	 * @param tollFactor the implementation instance of toll factors.
	 * @param scenario
	 */
	private RoadPricingSchemeUsingTollFactor(URL pricingSchemeFileName, TollFactor tollFactor, Scenario scenario ) {

		// read the road pricing scheme from file
		RoadPricingSchemeImpl scheme = RoadPricingUtils.createAndRegisterMutableScheme(scenario );
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(scheme);
		try {
			rpReader.readURL(pricingSchemeFileName);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		this.delegate = scheme;
		this.tollFactor = tollFactor;

	}

	public static void createAndRegisterRoadPricingSchemeUsingTollFactor(URL pricingSchemeFileName, TollFactor tollFactor,
																																			 Scenario scenario ){
		new RoadPricingSchemeUsingTollFactor( pricingSchemeFileName, tollFactor, scenario );
		// yy todo: inline constructor. kai, jul'19
	}

	@Override
	public String getDescription() {
		return delegate.getDescription();
	}

	@Override
	public RoadPricingCost getLinkCostInfo( Id<Link> linkId, double time, Id<Person> personId, Id<Vehicle> vehicleId ) {
		RoadPricingCost baseToll = delegate.getLinkCostInfo(linkId, time, personId, vehicleId );
		if (baseToll == null) {
			return null;
		}
		final double tollFactorVal = tollFactor.getTollFactor(personId, vehicleId, linkId, time);
		return new RoadPricingCost(baseToll.startTime, baseToll.endTime, baseToll.amount * tollFactorVal);
	}

	@Override
	public RoadPricingCost getTypicalLinkCostInfo( Id<Link> linkId, double time ) {
		return delegate.getTypicalLinkCostInfo(linkId, time);
	}

	@Override
	public Set<Id<Link>> getTolledLinkIds() {
		return delegate.getTolledLinkIds();
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public String getType() {
		return delegate.getType();
	}

	@Override
	public Iterable<RoadPricingCost> getTypicalCosts() {
		return delegate.getTypicalCosts();
	}

	@Override
	public Map<Id<Link>, List<RoadPricingCost>> getTypicalCostsForLink() {
		return delegate.getTypicalCostsForLink();
	}

}
