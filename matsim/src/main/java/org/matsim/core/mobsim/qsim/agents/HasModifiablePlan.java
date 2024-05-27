
/* *********************************************************************** *
 * project: org.matsim.*
 * HasModifiablePlan.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.agents;

import org.matsim.api.core.v01.population.Plan;

/**
 * @author kainagel
 *
 */
public interface HasModifiablePlan {

	Plan getModifiablePlan();

	void resetCaches() ;

	int getCurrentLinkIndex();
	// not totally obvious that this should be _here_, but it really only makes sense together with the modifiable plan/within-da replanning
	// capability.  Maybe should find a different name for the interface. kai, nov'17

}
