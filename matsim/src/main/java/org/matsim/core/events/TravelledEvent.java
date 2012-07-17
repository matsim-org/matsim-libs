/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.events;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;

/**
 * A TravelEvent replaces, for teleported trips, the
 * LinkLeave-LinkEnter-Event-Chain. It contains a field for the distance, which
 * may be necessary for scoring.
 * <p/>
 * Some more info:
 * <p/>
 * I have a question concerning the "travelled" event.  As far as I understand,
 * this was added in order to get the distance information into the scoring
 * function. I am wondering why this was not just attached to the arrival event.
 *  "arrival" means, in my understanding, the end (i.e. a point in time) of some
 * travelling process (which is a section of a line in time). Was it just
 * because of some hesitance to touch established code?  Or was there anything
 * deeper behind it? kai
 * <p/>
 * Nothing deep, I think, but it was about symmetry/orthogonality. For network
 * modes, an event listener which wants to know the travelled distance listens
 * to a chain of LinkLeave-LinkEnter events. For non-network modes, an event
 * listener which wants to know the travelled distance now listens to a new
 * Event which is thrown in place of the LinkLeave-LinkEnter chain.
 * 
 * It was just a hunch. It doesn't need to stay that way... 
 * 
 * michael z.
 * 
 * @author zilske
 * 
 */
public interface TravelledEvent extends Event {

	public Id getPersonId();

	public double getDistance();

}
