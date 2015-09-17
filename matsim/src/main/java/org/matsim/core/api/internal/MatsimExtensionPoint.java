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
package org.matsim.core.api.internal;

/**
 * Marker interface to mark MATSim extension points.  As a first approximation, they should eventually correspond
 * to those mentioned in the "own extensions" chapter of the MATSim book.  The marker thus serves as a reminder
 * for the following things:<ul>
 * <li> Everything marked by this marker interface should not be renamed.
 * <li> Everything marked by this marker interface should, in its javadoc, point to example code how
 * it can be put to use.  In most cases, corresponding code should already exist in the tutorial section of
 * the matsim repository.  (Please write to us if such a pointer to example code does not exist.)
 * </ul>
 * 
 * @author nagel
 *
 */
public interface MatsimExtensionPoint {

}
