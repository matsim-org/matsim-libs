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
package playground.ivt.maxess.nestedlogitaccessibility.framework;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores results per person. Adding person results is thread-safe
 * @author thibautd
 */
public class AccessibilityComputationResult {
	private final Map<Id<Person>, PersonAccessibilityComputationResult> personResults = new ConcurrentHashMap<>();
	private final Set<String> types = new TreeSet<>(  );

	public Set<String> getTypes() {
		return Collections.unmodifiableSet( types );
	}

	public void addResults(
			final Id<Person> id,
			final PersonAccessibilityComputationResult result ) {
		// This should be thread safe
		types.addAll( result.getAccessibilities().keySet() );
		this.personResults.put( id , result );
	}

	public Map<Id<Person>, PersonAccessibilityComputationResult> getResultsPerPerson() {
		return Collections.unmodifiableMap( personResults );
	}

	public static class PersonAccessibilityComputationResult {
		private final Map<String, Double> accessibilities = new LinkedHashMap<>(  );

		public final void addAccessibility( final String type , final double value ) {
			accessibilities.put( type , value );
		}

		public Map<String, Double> getAccessibilities() {
			return accessibilities;
		}
	}
}
