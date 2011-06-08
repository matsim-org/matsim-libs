/* *********************************************************************** *
 * project: org.matsim.*
 * FhEmissions.java
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
 *                                                                         
 * *********************************************************************** */
package playground.fhuelsmann.emission;

import java.util.ArrayList;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import playground.fhuelsmann.emission.objects.HbefaObject;
import playground.fhuelsmann.emission.objects.HotValue;


public interface AnalysisModule {
	
	public void calculateEmissionsPerPerson(final double travelTime, final Id personId, 
			final double averageSpeed, final int roadType, //final String fuelSizeAge,  
			final double freeVelocity, final double distance,HbefaObject[][] hbefaTable,HbefaObject[][] hbefaHdvTable, Map<String,HotValue> HbefaHot,ArrayList<String> listOfPollutant);
	
	public void calculateEmissionsPerLink(final double travelTime, final Id linkId, 
			final Id personId, final double averageSpeed, final int roadType, 
			//final String fuelSizeAge,
			final double freeVelocity, final double distance,HbefaObject[][] hbefaTable,HbefaObject[][] hbefaHdvTable);
	

}
