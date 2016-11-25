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
package playground.thibautd.negotiation.framework;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import playground.thibautd.negotiation.locationnegotiation.LocationHelper;
import playground.thibautd.negotiation.locationnegotiation.RandomSeedHelper;

/**
 * @author thibautd
 */
public class NegotiationModule<P extends Proposition> extends AbstractModule {
	@Override
	protected void configure() {
		bind( Negotiator.class );
		bind( new Key<NegotiatingAgents<P>>() {} );
		bind( LocationHelper.class );
		bind( RandomSeedHelper.class );
	}
}
