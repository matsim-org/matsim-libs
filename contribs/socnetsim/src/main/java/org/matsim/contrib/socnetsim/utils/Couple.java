/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.socnetsim.utils;

import org.matsim.api.core.v01.Id;

public final class Couple {
	private final int hash;
	private final Id id1, id2;

	public Couple(
			final Id id1,
			final Id id2 ) {
		this.id1 = id1;
		this.id2 = id2;
		this.hash = id1.hashCode() + id2.hashCode();
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals( final Object o ) {
		final Couple other = (Couple) o;
		return ( other.id1.equals( id1 ) && other.id2.equals( id2 ) ) ||
			( other.id2.equals( id1 ) && other.id1.equals( id2 ) );
	}
}
