/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.kai.ptproject.qsim.interfaces;

import java.util.Queue;

import org.matsim.core.mobsim.jdeqsim.Vehicle;

/**Essentially the "driveway".  I think this is _directly_ a priority queue.
 * If this is so, then it might just delegate the collections container (may be slow!!).
 * 
 * 
 * @author nagel
 *
 */
@Deprecated // do not yet use
public interface WaitQueue extends Queue<Vehicle>{
	// The "Vehicle" import (from jdqsim) does not make sense.

}
