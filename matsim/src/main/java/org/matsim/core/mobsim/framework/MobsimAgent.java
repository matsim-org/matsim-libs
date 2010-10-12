/* *********************************************************************** *
 * project: matsim
 * MatsimAgent.java
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

package org.matsim.core.mobsim.framework;

import org.matsim.api.core.v01.Identifiable;

/**An interface for all matsim mobsim agents.  mobsim agents are entities capable of autonomous behavior.  This could, in my view, 
 * include signals.  "PersonAgent" as the joint super-interface is almost certainly too tight, since we want to include automatic
 * drivers.  Possibly, it could be "MobileAgents".  kai, aug'10
 * 
 * @author nagel
 *
 */
public interface MobsimAgent {
	
	void resetCaches() ;

}
