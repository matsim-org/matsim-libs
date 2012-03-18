/* *********************************************************************** *
 * project: matsim
 * RoadPricingSchemeI.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.roadpricing;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.roadpricing.RoadPricingScheme.Cost;

/**
 * @author nagel
 *
 */
public interface RoadPricingSchemeI {

	public String getName();

	public String getType();

	public String getDescription();

	public Set<Id> getLinkIdSet();

	public Map<Id, List<Cost>> getLinkIds();

	/**
	 * Returns the Cost object that contains the active costs for the given link
	 * at the specified time for a given person.  There is no need that the toll scheme indeed
	 * uses the person information.
	 * <br/>
	 * Design issues:<ul>
	 * <li>	I have decided to put the person into the method call rather than the setPerson construction in TravelDisutility etc.
	 * Reason: A big advantage of agent-based simulation over traditional methods is heterogeneity of agent population.
	 * But if we make this hard to use, the advantage shrinks.  kai, mar'12
	 * <li> It should truly be based on the vehicle (type).  But since vehicles have not yet universally annealed to a 
	 * robust state, I am sticking with persons.  kai, mar'12
	 * </ul>
	 *
	 * @param linkId
	 * @param time
	 * @param person TODO
	 * @return The cost object for the given link at the specified time,
	 * <code>null</code> if the link is either not part of the tolling scheme
	 * or there is no toll at the specified time for the link.
	 */
	public Cost getLinkCostInfo(final Id linkId, final double time, Person person);

}