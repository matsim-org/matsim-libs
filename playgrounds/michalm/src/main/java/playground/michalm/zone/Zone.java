/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.michalm.zone;

import org.matsim.api.core.v01.*;

import com.vividsolutions.jts.geom.MultiPolygon;


public class Zone
    implements Identifiable<Zone>
{
    private final Id<Zone> id;
    private final String type;
    private MultiPolygon multiPolygon;


    public Zone(Id<Zone> id, String type)
    {
        this.id = id;
        this.type = type;
    }


    public Zone(Id<Zone> id, String type, MultiPolygon multiPolygon)
    {
        this.id = id;
        this.type = type;
        this.multiPolygon = multiPolygon;
    }


    @Override
    public Id<Zone> getId()
    {
        return id;
    }


    public String getType()
    {
        return type;
    }


    public MultiPolygon getMultiPolygon()
    {
        return multiPolygon;
    }


    public void setMultiPolygon(MultiPolygon multiPolygon)
    {
        this.multiPolygon = multiPolygon;
    }
}
