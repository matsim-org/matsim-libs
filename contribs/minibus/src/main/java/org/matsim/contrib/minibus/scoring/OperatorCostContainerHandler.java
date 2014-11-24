/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.scoring;

/**
 * 
 * Handles {@link OperatorCostContainer} and does something meaningful with them. 
 * 
 * @author aneumann
 *
 */
interface OperatorCostContainerHandler {

	public void handleOperatorCostContainer(OperatorCostContainer operatorCostContainer);

	/**
	 * Reset everything
	 *
     */
	public void reset();
	
}
