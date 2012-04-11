package playground.michalm.demand;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.michalm.demand.Zone.Act;
import playground.michalm.demand.Zone.Group;
import playground.michalm.demand.Zone.Type;


public class ZoneXmlReader
    extends MatsimXmlParser
{
    private final static String ZONE = "zone";
    private final static String GROUP = "group";
    private final static String ACTIVITY = "activity";

    private Scenario scenario;

    private Zone currentZone;

    private Map<Id, Zone> zones = new TreeMap<Id, Zone>();

    private List<Zone> fileOrderedZones = new ArrayList<Zone>();


    public ZoneXmlReader(Scenario scenario)
    {
        this.scenario = scenario;
    }


    public Map<Id, Zone> getZones()
    {
        return zones;
    }


    public List<Zone> getZoneFileOrder()
    {
        return fileOrderedZones;
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
        Type type = Type.valueOf(atts.getValue("type").toUpperCase()); 
        currentZone = new Zone(id, type);

        zones.put(id, currentZone);
        fileOrderedZones.add(currentZone);
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
