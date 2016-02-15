package playground.michalm.zone.io;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

import playground.michalm.zone.Zone;


public class ZoneXmlWriter
    extends MatsimXmlWriter
{
    private Map<Id<Zone>, Zone> zones;


    public ZoneXmlWriter(Map<Id<Zone>, Zone> zones)
    {
        this.zones = zones;
    }


    public void write(String file)
    {
        openFile(file);
        writeDoctype("zones", "http://matsim.org/files/dtd/zones_v1.dtd");
        writeStartTag("zones", Collections.<Tuple<String, String>>emptyList());
        writeZones();
        writeEndTag("zones");
        close();
    }


    private void writeZones()
    {
        for (Zone z : zones.values()) {
            List<Tuple<String, String>> atts = new ArrayList<>();
            atts.add(new Tuple<String, String>("id", z.getId().toString()));

            String type = z.getType();
            if (type != null && !type.isEmpty()) {
                atts.add(new Tuple<String, String>("type", type));
            }

            writeStartTag("zone", atts, true);
        }
    }
}
