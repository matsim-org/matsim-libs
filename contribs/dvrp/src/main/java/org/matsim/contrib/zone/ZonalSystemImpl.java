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

package org.matsim.contrib.zone;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.zone.util.*;


public class ZonalSystemImpl
    implements ZonalSystem
{
    private final Map<Id<Zone>, Zone> zones;
    private final Map<Id<Node>, Zone> nodeToZoneMap;
    
    public ZonalSystemImpl(Map<Id<Zone>, Zone> zones, ZoneFinder zoneFinder, Network network)
    {
        this.zones = zones;
        
        nodeToZoneMap = NetworkWithZonesUtils.createNodeToZoneMap(network, zoneFinder);
    }
    
    
    @Override
    public Map<Id<Zone>, Zone> getZones()
    {
        return zones;
    }
    
    
    @Override
    public Zone getZone(Node node)
    {
        return nodeToZoneMap.get(node);
    }
}
