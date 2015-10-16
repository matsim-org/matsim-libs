/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.utils;

import java.util.ArrayList;

import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;

import playground.jbischoff.taxibus.passenger.TaxibusRequest;
import playground.jbischoff.taxibus.vehreqpath.TaxibusVehicleRequestPath;

/**
 * @author  jbischoff
 *
 */
public class TaxibusUtils {
    public static final String TAXIBUS_MODE = "taxibus";



public static double calcPathCost(ArrayList<VrpPathWithTravelData> newPath ){
	double cost = 0.0;
	
	for (VrpPathWithTravelData path : newPath){
		cost += path.getTravelCost();
	}
	
	return cost;
}



public static void printRequestPath(TaxibusVehicleRequestPath best) {
	System.out.println("RequestPath for vehicle : "+best.vehicle.getId());
	for (TaxibusRequest r : best.requests){
	System.out.println(r.toString() + "\tfrom\t"+ r.getFromLink().getId().toString() + "\tto:\t"+ r.getToLink().getId().toString());
	}
	for (VrpPathWithTravelData p : best.path){
		System.out.println("Path from\t" + p.getFromLink().getId().toString()+ "\tto\t"+ p.getToLink().getId().toString());
		
	}
	
}
}
