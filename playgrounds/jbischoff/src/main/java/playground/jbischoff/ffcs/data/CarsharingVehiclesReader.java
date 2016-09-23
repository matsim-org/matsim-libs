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
 * 
 */
package playground.jbischoff.ffcs.data;

import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.Attributes;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class CarsharingVehiclesReader  extends MatsimXmlParser
{
    private static final String VEHICLE = "vehicle";


    private CarsharingData data;


    public CarsharingVehiclesReader(CarsharingData data)
    {
        this.data = data;

    }


    @Override
    public void startTag(String name, Attributes atts, Stack<String> context)
    {
        if (VEHICLE.equals(name)) {
            Id<Vehicle> id = Id.create(atts.getValue("id"), Vehicle.class);
            Id<Link> linkId = Id.createLinkId(atts.getValue("start_link"));
            this.data.addVehicle(id, linkId);
        }
    }


    @Override
    public void endTag(String name, String content, Stack<String> context)
    {}


   
    
    
   

	
}
