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

import java.util.Arrays;
import java.util.Collection;

/**
 * if other use cases, might be transformed to an interface
 */
public class NestedChoiceSet<N extends Enum<N>> {
	private final Collection<Nest<N>> nests;

	public NestedChoiceSet( final Nest<N>... nests ) {
		this.nests = Arrays.asList( nests );
	}

	public Collection<Nest<N>> getNests() {
		return nests;
	}
}
