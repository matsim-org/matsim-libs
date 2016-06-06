/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.input.urban.calibration;

/**
* @author amit
*/

public enum PatnaDemandLabels {
	ward, // residence
	member, // household size
	sex, age, occupation, //pretty obvious
	monthlyInc, // current monthly income, splitted into several intervals 
	dailyCost, // current daily transport cost, splitted into several intervals
	originWard, // origin
	destiWard, //destination
	purpose,  // HBW, HBE, HBS, HBO, see PatnaUrbanActivityTypes
	mode, // travel mode
	freq // trip fequency
}


	