/* *********************************************************************** *
 * project: org.matsim.*
 * SubpopulationFilter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.ivt.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author thibautd
 */
public class SubpopulationFilter implements Filter<Id> {
	private final String attName;
	private final String subpop;
	private final ObjectAttributes personAtts;


	public SubpopulationFilter(
			final ObjectAttributes personAtts,
			final String subpop ) {
		this.personAtts = personAtts;
		this.attName = new PlansConfigGroup().getSubpopulationAttributeName();
		this.subpop = subpop;
	}

	public SubpopulationFilter(
			final ObjectAttributes personAtts,
			final String attName,
			final String subpop ) {
		this.personAtts = personAtts;
		this.attName = attName;
		this.subpop = subpop;
	}

	@Override
	public boolean accept(final Id id) {
		final String subpopPerson = (String) personAtts.getAttribute( id.toString() , attName );
		return subpop == null ? subpopPerson == null : subpop.equals( subpopPerson );
	}

	public Filter<Person> getPersonVersion() {
		return new Filter<Person>() {
			@Override
			public boolean accept(final Person o) {
				return SubpopulationFilter.this.accept( o.getId() );
			}
		};
	}
}

