/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.ev;

/**
 * All values used in this package use SI base and derived units. In particular:
 * <ul>
 * <li>distance - meter [m]</li>
 * <li>time - second [s]</li>
 * <li>energy - joule [J]</li>
 * <li>power - watt [W]</li>
 * </ul>
 * <p>
 * In particular, the use of [kWh] and [s] generates confusion and leads to bugs, as 1 kWh = 1,000 J * 3,600 s
 * <p>
 * Consequently, energy consumption is measured in [J/m], instead of [kWh/100km] or [Wh/km], as typically done in
 * transport.
 */
