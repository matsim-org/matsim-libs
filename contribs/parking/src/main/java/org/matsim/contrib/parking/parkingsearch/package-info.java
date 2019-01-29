/**
 * Thoughts related to parking:<ul>
 * <li> The agents to not learn routes any more from day to day: routes are completely computed on the fly.
 * <li> How does this pick up congestion?  At least in theory, the router may have a Travel Time that comes from the past.
 * In the sense of a Nash equilibrium, this seems imperative.
 * <li> It is <i> not </i> possible to learn routes from day to day ... since the car may be parked at different starting points every day.
 * So one thing that <i> could </i> be learned is some kind of private mental map.  Instead the approach now uses a global
 * mental map ... an ok approximation.
 * <li> There was the "Milano" problem: The router may be far off when planning several hours into the future. 
 * Could be addressed by regular (e.g. every 15min) replanning.
 * <li> The agent walk-teleportes from the activity facility to the parking facility.  Should she eventually consider
 * network walk, we need to be clear about access-walk -- network-walk -- access-walk??? -- car.
 * </ul>
 * 
 * 
 * @author jfbischoff, nagel
 *
 */
package org.matsim.contrib.parking.parkingsearch;