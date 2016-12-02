/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.negotiation.locationnegotiation;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import playground.thibautd.negotiation.framework.AlternativesGenerator;
import playground.thibautd.negotiation.framework.PropositionUtility;

/**
 * @author thibautd
 */
public class LocationNegotiationModule extends AbstractModule {
	@Override
	protected void configure() {
		bind( new Key<PropositionUtility<LocationProposition>>() {} ).to( LocationUtility.class );
		bind( new Key<AlternativesGenerator<LocationProposition>>() {} ).to( LocationAlternativesGenerator.class );
	}
}
