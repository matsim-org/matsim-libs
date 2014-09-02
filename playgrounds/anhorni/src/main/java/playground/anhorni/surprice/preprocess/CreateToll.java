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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkImpl;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.roadpricing.RoadPricingWriterXMLv1;

public class CreateToll {
	
	private final static Logger log = Logger.getLogger(CreateToll.class);
	       
    public void create(String path, Zone tollZone, double startTime, double endTime, double amount, String type, String desc) { 	    	
    	RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
    	scheme.setType(type);
    	scheme.setName("surprice");
    	scheme.setDescription(desc); 	
    	
    	scheme.addCost(startTime, endTime, amount);
    	
    	// add links of center area
    	for (Id linkId : tollZone.getlinksInZone()) {
    		scheme.addLink(linkId);
    	} 
    	log.info("Writing tolls to " + path + "/tolls.xml");
    	RoadPricingWriterXMLv1 tollWriter = new RoadPricingWriterXMLv1(scheme);
    	tollWriter.writeFile(path + "/tolls.xml");	
    }
    
    public void createLinkTolling(String path, NetworkImpl network, Zone tollZone, double startTime, double endTime, double amount, String type, String desc) { 	    	
    	RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
    	scheme.setType(type);
    	scheme.setName("surprice");
    	scheme.setDescription(desc); 	
    	
    	scheme.addCost(startTime, endTime, amount);
    	
    	// add links of center area
    	for (Link link : network.getLinks().values()) {
    		if (link.getFreespeed() >= 60.0 / 3.6 && tollZone.getlinksInZone().contains(link.getId())) {
    			scheme.addLink(link.getId());
    		}
    	} 
    	log.info("Writing tolls to " + path + "/tolls.xml");
    	RoadPricingWriterXMLv1 tollWriter = new RoadPricingWriterXMLv1(scheme);
    	tollWriter.writeFile(path + "/tolls.xml");	
    }
}
