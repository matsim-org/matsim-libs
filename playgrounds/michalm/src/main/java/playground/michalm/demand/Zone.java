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

package playground.michalm.demand;

import java.io.IOException;
import java.util.Map;

import org.matsim.api.core.v01.*;
import org.opengis.feature.simple.SimpleFeature;


public class Zone
    implements Identifiable
{
    public static enum Type
    {
        INTERNAL, EXTERNAL, SPECIAL
    }


    private Id id;
    private Type type;
    private SimpleFeature zonePolygon;


    public Zone(Id id, Type type)
    {
        this.id = id;
        this.type = type;
    }


    @Override
    public Id getId()
    {
        return id;
    }


    public Type getType()
    {
        return type;
    }


    public SimpleFeature getZonePolygon()
    {
        return zonePolygon;
    }


    public void setZonePolygon(SimpleFeature zonePolygon)
    {
        this.zonePolygon = zonePolygon;
    }


    public static Map<Id, Zone> readZones(Scenario scenario, String zonesXmlFile,
            String zonesShpFile, String idField)
        throws IOException
    {
        ZoneXmlReader xmlReader = new ZoneXmlReader(scenario);
        xmlReader.parse(zonesXmlFile);
        Map<Id, Zone> zones = xmlReader.getZones();
        ZoneShpReader.readZones(zonesShpFile, idField, scenario, zones);
        return zones;
    }
}
