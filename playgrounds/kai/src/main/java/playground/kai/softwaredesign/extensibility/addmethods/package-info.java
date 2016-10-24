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
 * These are pure data classes, with static methods to operate on them.
 * <p></p>
 * Methods can be added by local programming: Just add a new static method, e.g. computeDiameter( Shape shape ) ;
 * <p></p>
 * In contrast, adding a new Shape means a shotgun approach: every existing static behavioral method needs to be adapted. 
 * <p></p>
 * At the level of the MATSim data classes, we probably want this: we have reasonably stable data clases, plus static
 * methods to operate on them.
 * 
 * @author nagel
 */
package playground.kai.softwaredesign.extensibility.addmethods;