/* *********************************************************************** *
 * project: org.matsim.*
 * CreateToll.java
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

package playground.anhorni.surprice.preprocess;

import org.matsim.api.core.v01.Id;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingWriterXMLv1;

public class CreateToll {
	       
    public void create(String path, Zone tollZone) { 	    	
    	RoadPricingScheme scheme = new RoadPricingScheme();
    	scheme.setType("area");
    	scheme.setName("surprice");
    	
    	double startTime = 8.0 * 3600.0;
    	double endTime = 12.0 * 3600.0;
    	double amount = 10.0;    	
    	
    	scheme.addCost(startTime, endTime, amount);
    	
    	// add links of center area
    	for (Id linkId : tollZone.getlinksInZone()) {
    		scheme.addLink(linkId);
    	}    	    	
    	RoadPricingWriterXMLv1 tollWriter = new RoadPricingWriterXMLv1(scheme);
    	tollWriter.writeFile(path + "tolls.xml");	
    }
}
