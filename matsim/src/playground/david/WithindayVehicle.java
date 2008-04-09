/* *********************************************************************** *
 * project: org.matsim.*
 * WithindayVehicle.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.david;

import java.io.Serializable;

import org.matsim.mobsim.Vehicle;
import org.matsim.network.Link;
import org.matsim.utils.vis.netvis.DrawableAgentI;


public class WithindayVehicle extends Vehicle implements Serializable, DrawableAgentI {

	// private Agent agent; //hold Johannes agent representation

	@Override
	public Link chooseNextLink() {
		// Give the agent the opportunity to replan, if applicable and
		// Ask agent for next link
		// I dont know if we can include this easily here?
		return null;
	}
}
