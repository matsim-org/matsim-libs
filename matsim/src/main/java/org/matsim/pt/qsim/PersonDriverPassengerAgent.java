/* *********************************************************************** *
 * project: matsim
 * PersonDriverPassengerAgent.java
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

package org.matsim.pt.qsim;

import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.PlanDriverAgent;

/**
 * @author nagel
 *
 */
public interface PersonDriverPassengerAgent extends PlanAgent, DriverAgent, PassengerAgent, PlanDriverAgent {
	// note: this needs the combined interfaces (currently: PersonDriverAgent) in addition to the atomistic interfaces
	// because of "instanceof" conditions.  kai, nov'10

}
