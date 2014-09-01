/* *********************************************************************** *
 * project: org.matsim.*
 * TagPenetration.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.jjoubert.projects.gautengPopulation;

/**
 * Currently the eTag penetration is set arbitrarily (January 2014):
 * <ul>
 * 		<li> Private car: 40%;
 * 		<li> Commercial vehicles (intra and inter-Gauteng): 40%;
 * 		<li> Bus: 50%;
 * 		<li> Taxi: 40%; and
 * 		<li> External vehicles: 25%
 * </ul>
 *
 * @author jwjoubert
 */
abstract class TagPenetration {
	final static double CAR = 0.40;
	final static double COMMERCIAL = 0.40;
	final static double BUS = 0.50;
	final static double TAXI = 0.40;
	final static double EXTERNAL = 0.25;

}

