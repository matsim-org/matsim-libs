package playground.jbischoff.taxi.berlin.converters;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.zone.*;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.michalm.berlin.BerlinZoneUtils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;


public class BerlinBrandenburgShape2ZoneConverter
{

    private Map<String, MultiPolygon> polMap;


    public static void main(String[] args)
    {
        BerlinBrandenburgShape2ZoneConverter bc = new BerlinBrandenburgShape2ZoneConverter();
        bc.readShapeFile("C:/local_jb/data/OD/shp_merged/Planungsraum.shp", "SCHLUESSEL");
        bc.readShapeFile("C:/local_jb/data/OD/shp_merged/gemeinden.shp", "NR");
        bc.writeZones();
    }


    public BerlinBrandenburgShape2ZoneConverter()
    {
        this.polMap = new TreeMap<String, MultiPolygon>();
    }


    public void readShapeFile(String filename, String attrString)
    {

        for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {

            GeometryFactory geometryFactory = new GeometryFactory();
            WKTReader wktReader = new WKTReader(geometryFactory);
            Geometry geometry;

            try {
                geometry = wktReader.read( (ft.getAttribute("the_geom")).toString());

                if (geometry.getGeometryType().equals("MultiPolygon")) {
                    MultiPolygon mp = (MultiPolygon)geometry;
                    this.polMap.put(ft.getAttribute(attrString).toString(), mp);
                }

            }
            catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }


    private void writeZones()
    {
        Map<Id<Zone>, Zone> zoneMap = new HashMap<Id<Zone>, Zone>();
        for (Entry<String, MultiPolygon> e : polMap.entrySet()) {
            Id<Zone> zoneId = Id.create(e.getKey(), Zone.class);
            Zone zone = new Zone(zoneId, zoneId.toString(), e.getValue());
            zoneMap.put(zoneId, zone);
        }
        Zones.writeZones(zoneMap, BerlinZoneUtils.ZONE_COORD_SYSTEM,
                "C:/local_jb/data/OD/shp_merged/zones.xml",
                "C:/local_jb/data/OD/shp_merged/zones.shp");
    }
}
