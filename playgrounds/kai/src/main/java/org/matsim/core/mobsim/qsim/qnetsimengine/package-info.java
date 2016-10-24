/* *********************************************************************** *
 * project: org.matsim.*												   *
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
/**
 * <h3> Use case for hybrid netsim engine </h3>
 * 
 * not documented
 * 
 * <h3> Emulating BVWP assignment </h3>
 * 
 * Trying to emulate some version of traditional assignment.  My current "reference case" is the road 
 * assignment used in the BVWP (German national assessment exercise).  I am not sure if we have
 * a full description; I speculate as follows:<ul>
 * <li> The assignment proceeds hour by hour.
 * <li> There may be leftover traffic from the previous hour; they are assignment first.
 * <li> All other OD pairs are assigned to routes.
 * <li> Link speeds are computed according to V/C functions.
 * <li> Vehicles are moved forward according to link speeds.
 * <li> Vehicles which have not arrived during the hour are kept for the next hour. Some things are unclear here, e.g.:<ul>
 * <li> Do all vehicles start at the beginning of the hour, or are they smeared out?
 * <li> Are "leftover vehicles" re-routed at the beginning of the next time step, or do they somehow memorize their route 
 * (which might be difficult given their current approach)?
 * </ul>
 * </ul>
 * <p></p>
 * How translate into the matsim q model?  Some thoughts:<ul>
 * <li> Vehicles are moved into the buffer when their time is up, without physical queuing constraints.  This means (at least?) the following:<ul>
 * <li> Buffer has infinite size.
 * <li> Either "lost time = zero" or infinite storage capacity.
 * <li> Infinite flow capacity.
 * </ul>
 * <li> The link travel time comes from the V/C function.  Needs to be programmed, as function of $x_a$.
 * <li> <i> What is $x_a$? </i> Given the time-dynamic assignment without physical queues outlined above, it seems that $x_a$ is (simply) the
 * number of vehicles traversing the link in a given hour.<ul>
 * <li> A small problem is how to count this.  
 * <li> However, it seems to me that also the BVWP method cannot do a perfect job here: Only
 * when the last vehicle is assigned to a link then they know the true travel time but I don't think they will use this for the time-dependency of
 * the vehicle movements.
 * (They will, however, use it for the performance measure in the end, I would assume.)
 * <li> So I would say we try to figure out how many vehicles were on the link during the last hour ... and we can even make that hour "rolling".
 * </ul>
 * </ul>
 * 
 * @author nagel
 */
package org.matsim.core.mobsim.qsim.qnetsimengine;