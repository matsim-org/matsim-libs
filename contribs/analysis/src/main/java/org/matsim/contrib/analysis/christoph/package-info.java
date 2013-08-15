/* *********************************************************************** *
 * project: analysis
 * package-info.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
 * This package contains some classes that can be plugged into a MATSim
 * Controler. They produce additional outputs that one might use to analyse
 * the outcomes of a MATSim run.
 *  <ul>
 *  	<li>ActivitiesAnalyzer: analyzes the number of agents performing activities over the simulation period.
 *  		Results are produced for each iteration and for all types of performed activites.</li>
 *  	<li>TravelTimesWriter: analyzes the average link travel times and writes them to files (absolute and
 *  		relative values; txt and shp files).</li>
 *  	<li>TripsAnalyzer: analyzes the average leg travel times and the number of trips per mode and iteration.</li>
 *  </ul>
 * 
 * @author cdobler
 */
package org.matsim.contrib.analysis.christoph;