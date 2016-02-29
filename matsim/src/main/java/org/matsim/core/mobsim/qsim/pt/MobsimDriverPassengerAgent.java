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

package org.matsim.core.mobsim.qsim.pt;

import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

/**
 * @author nagel
 *
 */
public interface MobsimDriverPassengerAgent extends PTPassengerAgent, MobsimDriverAgent, MobsimPassengerAgent {
	// note: this needs the combined interfaces (currently: MobsimDriverAgent) in addition to the atomistic interfaces
	// because of "instanceof" conditions.  kai, nov'10
	
	// yy One should consider renaming this into MobsimPTPassengerAgent and make it extend PTPassengerAgent, MobsimAgent only
	// (modular interface for PT Passengers).  kai, nov'14
	
}
