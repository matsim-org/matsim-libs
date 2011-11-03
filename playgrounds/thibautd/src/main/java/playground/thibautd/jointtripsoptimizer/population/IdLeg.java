/* *********************************************************************** *
 * project: org.matsim.*
 * IdLeg.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.population;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

/**
 * Identifier for joint legs.
 * The idea behind the re-implementation of id types is allow easy check of the
 * identified object type.
 *
 * @author thibautd
 */
public class IdLeg implements Id {

	private final IdImpl id;

	public IdLeg(final long value) {
		this.id = new IdImpl( value );
	}

	@Override
	public boolean equals(final Object other) {
		return id.equals(other);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public int compareTo(final Id other) {
		return id.compareTo( other );
	}

	@Override
	public String toString() {
		return id.toString();
	}

}

