
/* *********************************************************************** *
 * project: org.matsim.*
 * QNodeI.java
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

 /**
 * 
 */
package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.interfaces.NetsimNode;

import java.util.*;

/**
 * @author kainagel
 *
 */
interface QNodeI extends NetsimNode {

	boolean doSimStep(double now) ;

	List<AcceptedVehiclesDto> acceptVehicles(List<MoveVehicleDto> moveVehicleDtos, boolean local);

	void init() ;

	Map<Id<Link>, List<AcceptedVehiclesDto>> acceptVehiclesLocal(List<MoveVehicleDto> moveVehicleDtos);

	void handleAccepted(List<AcceptedVehiclesDto> accepted);
}
