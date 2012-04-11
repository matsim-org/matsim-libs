package playground.michalm.demand;

import java.io.IOException;
import java.util.*;

import org.geotools.data.FeatureSource;
import org.geotools.feature.*;
import org.matsim.api.core.v01.*;
import org.matsim.core.utils.gis.ShapeFileReader;


public class ZoneShpReader
{
    private Scenario scenario;
    private Map<Id, Zone> zones;


    public ZoneShpReader(Scenario scenario, Map<Id, Zone> zones)
    {
        this.scenario = scenario;
        this.zones = zones;
    }


    public void readZones(String file, String idField)
        throws IOException
    {
        FeatureSource fts = ShapeFileReader.readDataFile(file);
        FeatureCollection ftColl = fts.getFeatures();

        if (ftColl.size() != zones.size()) {
            throw new RuntimeException("Features: " + ftColl.size() + "; zones: " + zones.size());
        }

        @SuppressWarnings("unchecked")
        Iterator<Feature> iter = ftColl.iterator();

        while (iter.hasNext()) {
            Feature ft = iter.next();
            String id = ft.getAttribute(idField).toString();
            Zone z = zones.get(scenario.createId(id));
            z.setZonePolygon(ft);
        }
    }
}
