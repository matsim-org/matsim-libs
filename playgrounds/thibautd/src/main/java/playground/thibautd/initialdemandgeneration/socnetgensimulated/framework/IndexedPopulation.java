/* *********************************************************************** *
 * project: org.matsim.*
 * IndexedPopulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.framework;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 * @author thibautd
 */
public abstract class IndexedPopulation {
	private final Id[] ids;

	protected IndexedPopulation(final Id[] ids) {
		this.ids = ids;
	}

	public Id<Person> getId( final int index ) {
		return ids[ index ];
	}

	public int size() {
		return ids.length;
	}
}

