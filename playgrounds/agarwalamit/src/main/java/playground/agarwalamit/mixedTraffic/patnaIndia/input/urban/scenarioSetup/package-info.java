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
/**
 * It contains the data calibration codes and explanations. 
 * Therefore, most of things are not reusable for other scenarios.
 * 
 * <li>
 * First run calibrator which will calibrate the data for nonslum between zones 27 to 42. 
 * This data is not available in trip diaries.
 * </li>
 * 
 * <li>
 * Then run the cleaner, this will clean the files and put mostly meaningful data, if some data 
 * is not available, it will randomly produce based on the given distribution in the Patna CMP.
 * <b> The important thing is, the distribution is adjusted for unknown data so that the distribution for all (known+unknown) is
 * same as given distribution.</b>
 * </li>
 * 
 * <li>
 * All generated distribution are taken from tables/figures of Patna CMP and an reference is 
 * made to the code.
 * </li>
 * @author amit
 *
 */
package playground.agarwalamit.mixedTraffic.patnaIndia.input.urban.scenarioSetup;