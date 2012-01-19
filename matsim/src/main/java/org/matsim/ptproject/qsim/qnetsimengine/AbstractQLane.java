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
package org.matsim.ptproject.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;


/**
 * Interface representing the buffer functionality common for all Queue-Logic Links and Lanes, i.e.
 * providing selected, decorated methods for Buffer access and additional methods needed for
 * the buffer logic implemented.
 * <p/>
 * Thoughts/comments:<ul>
 * <li> There is the "lane" functionality (e.g. "addToLane", "popFirst"), and the "link" 
 * functionality (e.g. "addToParking").  For the normal qsim, they are combined into
 * the QLinkImpl.  For the qsim with lanes, those are split into QLane and QLinkLanesImpl.
 * This has led to a lot of code replication, and some of the code has diverged 
 * (QLinkImpl is more modern with respect to pt and with respect to vehicle conservation).
 * kai, nov'11
 * </ul>
 * Please read the docu of QBufferItem, QLane, QLinkInternalI (arguably to be renamed
 * into something like AbstractQLink) and QLinkImpl jointly. kai, nov'11
 * 
 * @author dgrether
 */
abstract class AbstractQLane extends VisLane {
	/**
	 * equivalent to a Buffer.isEmpty() operation
	 */
	abstract boolean bufferIsEmpty();

	/**
	 * equivalent to a Buffer.pop() operation
	 */
	abstract QVehicle popFirstFromBuffer();
	/**
	 * equivalent to a Buffer.peek() operation
	 */
	abstract QVehicle getFirstFromBuffer();

	abstract double getBufferLastMovedTime();

	abstract boolean hasGreenForToLink(Id toLinkId);
	
	abstract boolean hasSpace();
	
}
