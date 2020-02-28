/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
 * marker interface
 * <p></p>
 * Design comments:<ul>
 * <li>For the time being, factories should try to have as little state as possible.  Optimally, they have no state at all, i.e. everything
 * is done in the "create" method.
 * <li> (The above statement does not apply to anonymous implementations of factory interfaces ... since they can be considered ad-hoc
 * scripts where the sequence of actions is, in fact, determined by the script.)
 * </ul>
 * 
 * @author nagel
 *
 */
public interface MatsimFactory {

}
