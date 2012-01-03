/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripPossibilitiesImpl.java
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
package playground.thibautd.jointtrips.population.jointtrippossibilities;

import java.util.Collections;
import java.util.List;

/**
 * Immutable default implementation of a JointTripPossibilities container.
 * It provides no public constructor: instances are obtained by factories and
 * builders.
 *
 * @author thibautd
 */
public class JointTripPossibilitiesImpl implements JointTripPossibilities {
	private final List<JointTripPossibility> possibilities;

	JointTripPossibilitiesImpl(
			final List<JointTripPossibility> possibilities) {
		this.possibilities = Collections.unmodifiableList( possibilities );
	}

	@Override
	public List<JointTripPossibility> getJointTripPossibilities() {
		return Collections.unmodifiableList( possibilities );
	}
}

