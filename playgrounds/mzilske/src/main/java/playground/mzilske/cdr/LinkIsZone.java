/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * LinkIsZone.java
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

package playground.mzilske.cdr;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class LinkIsZone implements ZoneTracker.LinkToZoneResolver {

    @Override
    public Id resolveLinkToZone(Id<Link> linkId) {
        return linkId;
    }

    @Override
		public Id<Link> chooseLinkInZone(String zoneId) {
        return Id.create(zoneId, Link.class);
    }

}
