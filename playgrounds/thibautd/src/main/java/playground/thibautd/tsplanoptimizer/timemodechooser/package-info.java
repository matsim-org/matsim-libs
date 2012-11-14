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
 * Best-response module based on Matthias Feil "TimeModeChoicer" algorithm.
 * It optimizes a plan activity durations and modes given the travel times
 * observed in the previous iterations using tabu search.
 * <br>
 * The package contains a framework, which can be used to build Tabu Search
 * based replaning modules, and a replaning module using this framework.
 * @author thibautd
 */
package playground.thibautd.tsplanoptimizer.timemodechooser;

