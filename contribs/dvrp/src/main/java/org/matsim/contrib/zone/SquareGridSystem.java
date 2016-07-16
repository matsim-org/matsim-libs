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

package org.matsim.contrib.zone;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;


public class SquareGridSystem
    implements ZonalSystem
{
    private final SquareGrid grid;
    private final Map<Id<Zone>, Zone> zones = new HashMap<>();
    private final Map<Id<Zone>, Zone> safeZones = Collections.unmodifiableMap(zones);


    public SquareGridSystem(Network network, double cellSize)
    {
        this.grid = new SquareGrid(network, cellSize);

        for (Node n : network.getNodes().values()) {
            Zone zone = grid.getZone(n.getCoord());
            zones.put(zone.getId(), zone);
        }
    }


    public static Map<Id<Zone>, Zone> filterZonesWithNodes(Map<Id<Zone>, Zone> zones,
            Network network, ZonalSystem zonalSystem)
    {
        Map<Id<Zone>, Zone> zonesWithNodes = new HashMap<>();
        for (Node n : network.getNodes().values()) {
            Zone zone = zonalSystem.getZone(n);
            zonesWithNodes.put(zone.getId(), zone);
        }

        return zonesWithNodes;
    }


    @Override
    public Map<Id<Zone>, Zone> getZones()
    {
        return safeZones;
    }


    @Override
    public Zone getZone(Node node)
    {
        return grid.getZone(node.getCoord());
    }
}
