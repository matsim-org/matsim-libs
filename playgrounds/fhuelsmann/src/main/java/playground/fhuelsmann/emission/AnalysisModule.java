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


import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.fhuelsmann.emission.objects.HbefaObject;



public interface AnalysisModule {
	
//	public void calculateEmissionsPerLink(final double travelTime, final Id linkId, 
//			final Id personId, final double averageSpeed, final int roadType, 
//			final String fuelSizeAge, final double freeVelocity, final double distance,HbefaObject[][] hbefaTable,HbefaObject[][] hbefaHdvTable, EventsManager eventsManager);
//	
//	public void calculateEmissionsPerPerson(final double travelTime, final Id personId, 
//			final double averageSpeed, final int roadType, final String fuelSizeAge,  
//			final double freeVelocity, final double distance,HbefaObject[][] hbefaTable,HbefaObject[][] hbefaHdvTable, EventsManager eventsManager);
//	
//	
//	public void calculateEmissionsPerLinkForComHdvPecWithoutVeh (final double travelTime, final Id linkId, 
//			final Id personId, final double averageSpeed, final int roadType, 
//			final double freeVelocity, final double distance,HbefaObject[][] hbefaTable,HbefaObject[][] hbefaHdvTable );
//	
//	public void calculateEmissionsPerCommuterHdvPcWithoutVeh (final double travelTime, final Id personId, 
//			final double averageSpeed, final int roadType,  
//			final double freeVelocity, final double distance,HbefaObject[][] hbefaTable,HbefaObject[][] hbefaHdvTable);
//	
//	public void calculatePerLinkPtBikeWalk(final Id linkId, final Id personId);
//	
//	public void calculatePerPersonPtBikeWalk(final Id personId,final Id linkId);

	public void calculateWarmEmissions(Id linkId, Id personId,
			Integer roadType, Double freeVelocity, Double linkLength,
			Double enterTime, Double travelTime, String fuelSizeAge,
			HbefaObject[][] hbefaTable, HbefaObject[][] hbefaHdvTable,
			EventsManager eventsManager);

}
