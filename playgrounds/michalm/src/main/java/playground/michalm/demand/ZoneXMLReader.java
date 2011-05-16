package playground.michalm.demand;

import java.util.*;

import playground.michalm.demand.Zone.Act;
import playground.michalm.demand.Zone.Group;

import org.matsim.api.core.v01.*;
import org.matsim.core.utils.io.*;
import org.xml.sax.*;


public class ZoneXMLReader
    extends MatsimXmlParser
{
    private final static String ZONE = "zone";
    private final static String GROUP = "group";
    private final static String ACTIVITY = "activity";

    private Scenario scenario;

    private Zone currentZone;

    private Map<Id, Zone> zones = new TreeMap<Id, Zone>();


    public ZoneXMLReader(Scenario scenario)
    {
        this.scenario = scenario;
    }


    public Map<Id, Zone> getZones()
    {
        return zones;
    }


    @Override
    public void startTag(String name, Attributes atts, Stack<String> context)
    {
        if (ZONE.equals(name)) {
            startZone(atts);
        }
        else if (GROUP.equals(name)) {
            startGroup(atts);
        }
        else if (ACTIVITY.equals(name)) {
            startActivity(atts);
        }
    }


    @Override
    public void endTag(String name, String content, Stack<String> context)
    {}


    private void startZone(Attributes atts)
    {
        Id id = scenario.createId(atts.getValue("id"));
        currentZone = new Zone(id);

        zones.put(id, currentZone);
    }


    private void startGroup(Attributes atts)
    {
        String type = atts.getValue("type");
        Integer size = new Integer(atts.getValue("size"));

        currentZone.setGroupSize(Group.valueOf(type), size);
    }


    private void startActivity(Attributes atts)
    {
        String type = atts.getValue("type");
        Integer size = new Integer(atts.getValue("size"));

        currentZone.setActPlaces(Act.valueOf(type), size);
    }
}
