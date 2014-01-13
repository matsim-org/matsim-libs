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
package playground.thibautd.initialdemandgeneration.socnetgen.framework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;

/**
 * @author thibautd
 */
public class SocialNetwork {
	private final Collection<Tie> ties = new ArrayList<Tie>();
	private final Collection<Tie> unmodifiableTies = Collections.unmodifiableCollection( ties );
	private final Map<Id, Set<Id>> alterEgoMap = new HashMap<Id, Set<Id>>();

	public void addTie(final Tie tie) {
		ties.add( tie );
		
		addAlter( tie.getFirstId() , tie.getSecondId() );
		addAlter( tie.getSecondId() , tie.getFirstId() );
	}

	private void addAlter(
			final Id ego,
			final Id alter) {
		Set<Id> alters = alterEgoMap.get( ego );

		if ( alters == null ) {
			alters = new HashSet<Id>();
			alterEgoMap.put( ego , alters );
		}

		alters.add( alter );
	}

	public Collection<Tie> getTies() {
		return unmodifiableTies;
	}

	public Set<Id> getAlters(final Id ego) {
		final Set<Id> alters = alterEgoMap.get( ego );

		return alters != null ?
			Collections.unmodifiableSet( alters ) :
			Collections.<Id>emptySet();
	}

	public Set<Id> getEgos() {
		return Collections.unmodifiableSet( alterEgoMap.keySet() );
	}
}

