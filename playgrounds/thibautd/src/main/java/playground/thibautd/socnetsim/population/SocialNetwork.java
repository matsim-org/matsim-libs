/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetwork.java
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
package playground.thibautd.socnetsim.population;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Counter;

import playground.thibautd.utils.MapUtils;

/**
 * @author thibautd
 */
public class SocialNetwork {
	public static final String ELEMENT_NAME = "SocialNetwork";
	private final Counter tieCounter = new Counter( "SocialNetwork: (Monodirectional) Tie # " );
	private final Map<Id, Set<Id>> map = new HashMap<Id, Set<Id>>();

	public void addBidirectionalTie(final Id id1, final Id id2) {
		addMonodirectionalTie( id1 , id2 );
		addMonodirectionalTie( id2 , id1 );
	}

	public void addMonodirectionalTie(
			final Id ego,
			final Id alter) {
		final Set<Id> alters = MapUtils.getSet( ego , map );
		final boolean added = alters.add( alter );
		if ( added ) tieCounter.incCounter();
	}

	public Set<Id> getAlters(final Id ego) {
		final Set<Id> alters = map.get( ego );
		return alters == null ? Collections.<Id>emptySet() : alters;
	}
}

