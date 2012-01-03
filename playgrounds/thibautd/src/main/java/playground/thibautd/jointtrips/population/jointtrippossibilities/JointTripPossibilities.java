/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripPossibilities.java
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

import java.util.List;

/**
 * gives access to information on possible joint trips for a clique.
 *
 * @author thibautd
 */
public interface JointTripPossibilities {
	/**
	 * @return a list of {@link JointTripPossibility}
	 */
	public List<JointTripPossibility> getJointTripPossibilities();
}

