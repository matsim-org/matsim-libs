/**
 * This package contains some code for simulation of electric vehicles. It consists of the following parts:
 * <ul>
 *    <li>ERunner.java : startup code for the simulation, running a default MATSim simulation run with some additional persons.</li>
 *    <li>EPostProcessor.java: Postprocessing code, enabling the modelling of electric vehicles.</li>
 *    <li>package trafficstate: Code that reads and writes information about traffic state on each link</li>
 *    <li>package poi: Manages charging locations, so called "pois" in the postprocessing</li>
 *    <li>package eVehicles: Models the electric vehicles in the postprocessing</li>
 *    <li>package ePlans: specifies planned charging locations and times for electric vehicles</li>
 *    <li>package energy: models charging and discharging functionality</li>
 * </ul>
 * 
 * 
 *  @author droeder
 *  @author dgrether
 */
package playground.vsp.energy;