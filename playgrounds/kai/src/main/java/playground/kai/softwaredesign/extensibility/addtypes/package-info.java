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
 * 
 * These are behavioral classes, behind an interface.
 * <p></p>
 * Additional types can be added by local programming: Just add Xxx implements Shape, implement it, and there you are.
 * <p></p>
 * In contrast, adding new methods needs a shotgun approach: changes in every class which implements the interface.
 * <p></p>
 * In MATSim, at agent level (e.g. DriverAgent), we probably want to be able to create additional behavioral types.
 * 
 * 
 * @author nagel
 */
package playground.kai.softwaredesign.extensibility.addtypes;