/* *********************************************************************** *
 * project: org.matsim.*
 * QBufferItem
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
package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;


/**
 * Essentially an interface, but since I do not want the methods public for the time being, it is incarnated as an abstract class.  
 * <p/>
 * Contains the logic which is joint between QLane and QLink.  Some of this may eventually go ...
 * 
 * @author dgrether, nagel
 */
abstract class QInternalI {
	
	/**
	 * Seems ok as public interface function. kai, aug'15
	 */
	abstract boolean doSimStep(final double now) ;
	
	/**
	 * Seems ok as public interface function. kai, aug'15 
	 */
	abstract void clearVehicles() ;
	
	abstract Collection<MobsimVehicle> getAllVehicles() ;
	
	/**
	 * <br>
	 * seems ok as public interface function. kai, aug'15
	 */
	abstract void addFromUpstream(final QVehicle veh);
	
	abstract boolean isNotOfferingVehicle();

	abstract QVehicle popFirstVehicle();

	abstract QVehicle getFirstVehicle();

	abstract double getLastMovementTimeOfFirstVehicle();

	abstract boolean hasGreenForToLink(final Id<Link> toLinkId);
	
	abstract boolean isAcceptingFromUpstream();
	
}
