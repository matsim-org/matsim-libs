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

import java.util.EnumMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.opengis.feature.simple.SimpleFeature;


public class Zone
    implements Identifiable
{
    public static enum Group
    {
        S, W, O
    }


    public static enum Act
    {
        S, W, L
    }


    public static enum Type
    {
        INTERNAL, EXTERNAL, SPECIAL
    }


    private Id id;
    private Type type;
    private SimpleFeature zonePolygon;

    private EnumMap<Group, Integer> groupSizes = new EnumMap<Group, Integer>(Group.class);

    private EnumMap<Act, Integer> actPlaces = new EnumMap<Act, Integer>(Act.class);


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


    public void setGroupSize(Group group, Integer size)
    {
        groupSizes.put(group, size);
    }


    public void setActPlaces(Act act, Integer places)
    {
        actPlaces.put(act, places);
    }


    public Integer getGroupSize(Group group)
    {
        return groupSizes.get(group);
    }


    public Integer getActPlaces(Act act)
    {
        return actPlaces.get(act);
    }


    public EnumMap<Group, Integer> getGroupSizes()
    {
        return groupSizes;
    }


    public EnumMap<Act, Integer> getActPlaces()
    {
        return actPlaces;
    }
}
