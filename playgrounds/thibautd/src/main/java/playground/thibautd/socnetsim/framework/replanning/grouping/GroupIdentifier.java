/* *********************************************************************** *
 * project: org.matsim.*
 * GroupIdentifier.java
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
package playground.thibautd.socnetsim.framework.replanning.grouping;

import java.util.Collection;

import org.matsim.api.core.v01.population.Population;

/**
 * Identifies groups of agents to replan together.
 * Various approaches are valid:
 * <ul>
 * <li> fixed groups (e.g. households)
 * <li> dynamically determined based on the joint plans
 * <li>...
 * </ul>
 *
 * Note that new joint plans can only be created for agents belonging
 * to the same group.
 * @author thibautd
 */
public interface GroupIdentifier {
	public Collection<ReplanningGroup> identifyGroups(final Population population);
}
