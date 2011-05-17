package playground.michalm.demand;

import java.util.*;

import org.geotools.feature.*;
import org.matsim.api.core.v01.*;


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


    private Id id;
    private Feature zonePolygon;

    private EnumMap<Group, Integer> groupSizes = new EnumMap<Group, Integer>(Group.class);

    private EnumMap<Act, Integer> actPlaces = new EnumMap<Act, Integer>(Act.class);


    public Zone(Id id)
    {
        this.id = id;
    }


    @Override
    public Id getId()
    {
        return id;
    }


    public Feature getZonePolygon()
    {
        return zonePolygon;
    }


    public void setZonePolygon(Feature zonePolygon)
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
