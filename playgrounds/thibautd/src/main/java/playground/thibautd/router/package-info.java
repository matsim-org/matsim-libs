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
 * This package defines interfaces and classes for trip routing.
 * <br>
 * Trips can consist in any sequence of plan elements. Within-trip
 * activities (<i>stage activities</i>) are identified based on their types.
 * A trip is defined as the longest sequence of plan elements containing
 * only legs and stage activities.
 *
 * <br>
 * It is based on a three layers architecture:
 * <ul>
 * <li> the {@link org.matsim.core.router.RoutingModule}s are responsible for computing trips
 * between individual O/D couples, for a given mode. They moreover provide
 * access to an object allowing to identify their stage activities, implementing
 * {@link org.matsim.core.router.StageActivityTypes}.
 * <li> the {@link org.matsim.core.router.TripRouter} registers {@link org.matsim.core.router.RoutingModule}s for each
 * mode, and allows to route between O/D pairs for any mode. It does not modify the plan.
 * It moreover provides access to a {@link org.matsim.core.router.StageActivityTypes} instance allowing
 * to identify all possible stage activities, for all modes.
 * <li> the {@link org.matsim.core.router.PlanRouter} provides a {@link org.matsim.population.algorithms.PlanAlgorithm} to
 * route all trips in a plan.
 * </ul>
 *
 * The behaviour can be modified by implementing custom {@link org.matsim.core.router.RoutingModule}s.
 *
 *
 * @author thibautd
 */
package playground.thibautd.router;
