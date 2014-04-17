/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MyLinkToZoneResolver.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.stratum;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import playground.mzilske.cdr.ZoneTracker;

class MyLinkToZoneResolver implements ZoneTracker.LinkToZoneResolver {
    @Override
    public Id resolveLinkToZone(Id linkId) {
        return linkId;
    }

    public IdImpl chooseLinkInZone(String zoneId) {
        return new IdImpl(zoneId);
    }
}
