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

/**
 * Identifier for joint legs.
 * The idea behind the re-implementation of id types is allow easy check of the
 * identified object type.
 *
 * @author thibautd
 */
public class IdLeg implements Id {

	private final long id;

	public IdLeg(long idValue) {
		this.id = idValue;
	}

	public int compareTo(Id arg) {
		try {
			return (int) (this.id - ((IdLeg) arg).id);
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("can only compare IdLeg with IdLeg instances");
		}
	}

	/**
	 * {@inheritDoc}
	 * @see Object#equals(Object)
	 */
	@Override
	public boolean equals(Object obj) {
		try {
			return this.id == ((IdLeg) obj).id;
		} catch (ClassCastException e) {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		return String.valueOf(this.id);
	}
}

