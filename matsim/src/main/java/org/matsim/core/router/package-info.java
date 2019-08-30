
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
 * Contains different routing algorithms and {@linkplain org.matsim.core.population.algorithms.PlanAlgorithm PlanAlgorithms}
 * to use the routing algorithms on plans.
 * <br>
 * The routing algorithms, responsible for finding the least-cost-path between two nodes in the network, all
 * implement the interface {@link org.matsim.core.router.util.LeastCostPathCalculator}. Currently implemented are
 * {@linkplain PlansCalcRouteDijkstra Dijkstra's shortest path algorithm} and some optimizing variants of it
 * (e.g. {@linkplain AStarLandmarks A* with Landmarks}).
 * <br>
 * As the routing algorithms are all time-dependent, they need not only weights on the links, but time-dependent
 * weights and additionally the (estimated) travel times on these links. This data is provided by the interfaces
 * {@link org.matsim.core.router.util.TravelTime}, {@link org.matsim.core.router.util.TravelDisutility} and
 * {@link org.matsim.core.router.util.TravelMinDisutility}. A few commonly used implementations of these interfaces can
 * be found in the subpackage {@link org.matsim.core.router.costcalculators costcalculators}.
 * <br>
 * <br>
 * All modes are not necessarily routed on the network; moreover, a trip may consist
 * in a series of stages (movements with one vehicle, reprensented by legs),
 * which may be separated by "dummy" activities ("<i>stage activities</i>").
 * A trip is defined as the longest sequence of consecutive plan elements
 * consisting only of legs and stage activities.
 * For this, the following three layer architecture
 * is provided:
 *
 * <ul>
 * <li> the {@link org.matsim.core.router.RoutingModule}s are responsible for computing trips
 * between individual O/D couples, for a given mode.
 * <li> the {@link org.matsim.core.router.TripRouter} registers {@link org.matsim.core.router.RoutingModule}s for each
 * mode, and allows to route between O/D pairs for any (registered) mode.
 * It does not modify the plan, but provides convenience methods to
 * identify trips and easily insert a trip between two activities in a plan.
 * <br>
 * <li> the {@link org.matsim.core.router.PlanRouter} provides a {@link org.matsim.core.population.algorithms.PlanAlgorithm} to
 * route all trips in a plan.
 * </ul>
 *
 * The behaviour can be modified by implementing custom {@link org.matsim.core.router.RoutingModule}s.
 * <br>
 * The previous behavior was based on legs rather than trips: the corresponding
 * classes are <b>temporarily</b> kept in the {@link org.matsim.core.router.old}
 * package for backward compatibility.
 * <br>
 * Note that the routing algorithms are generally seen as <b>not thread-safe</b>! If threads are used, one
 * must ensure that each thread uses its own instance of a routing algorithm.
 */
package org.matsim.core.router;
