/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.vis.snapshotwriters.VisMobsim;

/**
 * This is a combination of capabilities that is needed to that the otfvis live mode can connect with the mobsim.  Historically,
 * this used to be the MobsimFeature.
 * 
 * @author nagel
 */
public interface VisMobsimFeature {
	
	VisMobsim getVisMobsim();

	Plan findPlan(Id agentId);

	void addTrackedAgent(Id agentId);

	void removeTrackedAgent(Id agentId);
	
}
