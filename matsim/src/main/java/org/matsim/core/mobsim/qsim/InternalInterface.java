/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.mobsim.qsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;

import java.util.List;

/**
 * Design thoughts:<ul>
 * <li> The main functionality of this interface is arrangeNextAgentState.
 * <li> getMobsim is provided as a convenience.
 * </ul>
 *
 * @author nagel
 *
 */
public interface InternalInterface {
	QSim getMobsim();
	void arrangeNextAgentState( MobsimAgent agent );
	void registerAdditionalAgentOnLink(MobsimAgent agent);
	MobsimAgent unregisterAdditionalAgentOnLink(Id<Person> agentId, Id<Link> linkId);

	List<DepartureHandler> getDepartureHandlers();
}
