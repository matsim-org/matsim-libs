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

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.roadpricing.RoadPricingSchemeImpl.Cost;

/**
 * @author nagel, mrieser, michaz
 *
 */
public interface RoadPricingScheme {

	/**
	 *  The type to be returned by getType() if this is a distance toll,
	 *  i.e. getLinkCostInfo() returns how much must be paid for each m.
	 *
	 */
	public static final String TOLL_TYPE_DISTANCE = "distance";
	
	/** The type to be used for cordon tolls. */
	public static final String TOLL_TYPE_CORDON = "cordon";
	
	/** The type to be used for area tolls. */
	public static final String TOLL_TYPE_AREA = "area";
	
	/** The type to be used for link toll (the toll scheme gives a toll per link). */
	public static final String TOLL_TYPE_LINK = "link";

	public String getName();

	/**
	 * @return Must return one of the four constants specified above.
	 */
	public String getType();

	public String getDescription();

	public Set<Id> getTolledLinkIds();

	/**
	 * Returns the Cost object that contains the active costs for the given link
	 * at the specified time for a given person.  There is no need that the toll scheme indeed
	 * uses the person information (or the time), and indeed the default implementation of this
	 * interface does not use it.
	 * 
	 * Note: Implementations of this interface are expected to be pure, stateless data containers. Do not,
	 * for example, implement a toll which a user must only pay once per day by giving a different cost
	 * depending on how often his method is called. The interpretation ("when do you pay this amount") is
	 * done by the classes which use this interface, depending on the value of getType().
	 * 
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
	public Cost getLinkCostInfo(final Id linkId, final double time, Id personId);

}