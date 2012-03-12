/* *********************************************************************** *
 * project: org.matsim.*
 * package-info.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
 * This package contains classes useful to compute time-dependent travel times
 * in best-response modules.
 *
 * The idea is that special "replacement" {@link RoutingModule}s are provided,
 * which are designed so as to be faster than the "core" ones, for example by using
 * fixed route for given O/D pairs, and only updating travel time for new departure
 * times.
 *
 * @author thibautd
 */
package playground.thibautd.tsplanoptimizer.timemodechooser.traveltimeestimation;
