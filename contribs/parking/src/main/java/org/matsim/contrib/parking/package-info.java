/**
 * The parking contrib is split into several substantially different parts<ul>
 *
 * <li> Parking Choice, based on work of Rashid Waraich. Parts of the contrib are used in the carsharing contrib.
 * <p> Its main goal, as far as we understand it, is the simulation of parking choice (e.g. between garages).
 * There is no modelling of circulating traffic searching for parking or walking of agents to/from parking. Currently (2022) unmaintained.
 *
 * <li> Parking Search, by Joschka Bischoff. This contrib is currently developed at TU Berlin.
 * <p> The main goal is to model parking search, including walk legs of agents and vehicles searching for parking spaces.
 *
 *  <li>Parking Proxy by Tobias Kohl (Senozon)
 * 		<p>This was designed for large scenarios where it's not feasable to fully simulate parking agents.
 * 		Rather, the additional time needed for parking is estimated</p>
 *
 * <li>Parking Costs, developed by Marcel Rieser and Joschka Bischoff at SBB.
 * 		<p> This modules allows the integration of parking costs based on link attribute data.</p>
 *
 * </ul>
 *
 * @author of this doc: jfbischoff, nagel
 */
package org.matsim.contrib.parking;
