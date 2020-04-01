
/* *********************************************************************** *
 * project: org.matsim.*
 * package-info.java
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
 * The "Simulation Listeners/Events" are in addition to "matsim events".  "matsim events" concern traffic-related events, 
 * "simulation events" concern events related to simulation structure (such as "initialization is finished").
 * 
 * <p></p>
 * 
 * There is some debate if these things are so easy to separate; for example "time step finished" (Simulation Event) is the same as 
 * "clock advances by one second" (event related to the traffic world).  Nevertheless, there are strong opinions to leave these things
 * separate, and so they are separate.
 * 
 * <p></p>
 * 
 * For an example of how to have them in the same channel, see the dissertation of Christian Gloor.
 */
package org.matsim.core.mobsim.framework.listeners;