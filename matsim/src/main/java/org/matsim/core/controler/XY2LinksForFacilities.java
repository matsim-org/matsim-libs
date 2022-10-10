/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.core.controler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

/**
 * Created by amit on 13.07.18.
 */

public class XY2LinksForFacilities {

    public static final Logger LOGGER = LogManager.getLogger(XY2LinksForFacilities.class);

    public static void run(Network network, ActivityFacilities facilities){

        int coordNullWarn = 0;
        int linkNullWarn = 0;

        for (ActivityFacility activityFacility : facilities.getFacilities().values()) {

            if (activityFacility.getCoord()==null && activityFacility.getLinkId()== null) {
                throw new RuntimeException("Neither coordinate nor linkId are available for facility id "+ activityFacility.getId()+". Aborting....");
            } else if (activityFacility.getLinkId()==null){
                if (linkNullWarn==0) {
                    LOGGER.warn("There is no link for at least a facility. Assigning links for such facilities from coords.");
                    LOGGER.warn(Gbl.ONLYONCE);
                    linkNullWarn++;
                }
                Link link = NetworkUtils.getNearestLink(network, activityFacility.getCoord());
                if (link==null) {
                    LOGGER.warn("No nearest link is found for coord "+activityFacility.getCoord());
                } else{
                    ((ActivityFacilityImpl)activityFacility).setLinkId(link.getId());
                }

            } else if (activityFacility.getCoord()==null){
                if (coordNullWarn==0) {
                    LOGGER.warn("There is no coord for the facility.");
                    LOGGER.warn(Gbl.ONLYONCE);
                    coordNullWarn++;
                }
            }
        }
    }
}
