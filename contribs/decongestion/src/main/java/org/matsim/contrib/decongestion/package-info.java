/**
 * 
 * A package which provides some tools to compute delays per link and time interval and set tolls accordingly in order to reduce delays.
 * Tolls per link and time interval are adjusted from iteration to iteration or every specified number of iterations.
 * 
 * There are different implementations to compute the tolls:
 * <ul>
 * <li> using a proportional-integral-derivative controller which treats the average delay as the error term; requires tuning of the parameters K_p, K_i and K_d
 * <li> using a proportional controller which treats the average delay as the error term and where K_p = value-of-travel-times-savings * number of delayed agents
 * <li> using a 'bang-bang' approach which either increases the toll or decreases the toll by a certain amount every specified number of iterations
 * </ul>
 * 
 * All relevant parameters such as the toll computation approach, the tuning parameters are specified in {@link org.matsim.contrib.decongestion.DecongestionConfigGroup}.
 * <p>
 * 
 * @author ikaddoura
 *
 */
package org.matsim.contrib.decongestion;