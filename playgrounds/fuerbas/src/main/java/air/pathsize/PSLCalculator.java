/* *********************************************************************** *
 * project: org.matsim.*
 * PSLCalculator
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
package air.pathsize;

import java.util.List;


/**
 * Interface for user specific calculators of overlap and plan weight to be used in PathSizeLogitSelector
 * @author dgrether
 *
 */
public interface PSLCalculator {

	/**
	 * Should calculate and set three values for all elements of the List:
	 * <ul>
	 * 	<li>The overall length of the plan that serves as norm for the length of each leg.</li>
	 * <li>The weight, see default implementations for examples.</li>
	 * </ul>
	 * 
	 * @param planData List containing PlanData instances, MainMode and Legs are already calculated. 
	 */
	public void calculatePSLValues(List<PSLPlanData> planData);
	
}
