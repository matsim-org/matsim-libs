/* *********************************************************************** *
 * project: org.matsim																																							 *
 *                               																			                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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


package org.matsim.ptproject.qsim.interfaces;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.PersonAgent;


public interface MobsimFeature {

	void afterPrepareSim();

	void beforeCleanupSim();

	/**
	 * @deprecated as it seems this method is not used by any QSimFeature, also it is not clear what 
	 * semantics  are provided by this method. QSim won't necessarily support the method handleAgentArrival(...). It is rather
	 * possible that there is a ArrivalHandler similar to the DepartureHandler concept
	 */
	@Deprecated
	void beforeHandleAgentArrival(PersonAgent agent);

	@Deprecated // do we need to pass the time?  to be discussed ...  kai, may'10
	void afterAfterSimStep(double time);

	@Deprecated // do we need to pass the time?  to be discussed ...  kai, may'10
	void beforeHandleUnknownLegMode(double now, PersonAgent agent, Link link);

	void afterActivityBegins(PersonAgent agent);

	@Deprecated // do we need to pass the time?  to be discussed ...  kai, may'10
	void afterActivityEnds(PersonAgent agent, double time);
	
	/**
	 * @deprecated as long as the agent representation of the QSim is not implemented this method provides
	 * not reliable functionality.
	 */
	@Deprecated
	void agentCreated(PersonAgent agent);

}
