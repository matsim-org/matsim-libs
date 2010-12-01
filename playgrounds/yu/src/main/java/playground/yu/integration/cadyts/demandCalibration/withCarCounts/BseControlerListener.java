/* *********************************************************************** *
 * project: org.matsim.*
 * BseControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
 * 
 */
package playground.yu.integration.cadyts.demandCalibration.withCarCounts;

/**
 * @author yu
 * 
 */
public interface BseControlerListener {

	/**
	 * to set, whether to write QGIS file during simulations
	 * 
	 * @param writeQGISFile
	 */
	public void setWriteQGISFile(final boolean writeQGISFile);

}
