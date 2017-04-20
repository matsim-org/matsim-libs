package playground.johannes.osm.FacilityGenerator;

import com.vividsolutions.jts.geom.Coordinate;
import org.apache.log4j.Logger;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.FacilitiesWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by johannesillenberger on 30.03.2017.
 */
public class CropFacilities {

    private static final Logger logger = Logger.getLogger(CropFacilities.class);

    public static void main(String args[]) {
        logger.info("Loading facilities...");
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        FacilitiesReaderMatsimV1 reader = new FacilitiesReaderMatsimV1(scenario);
        reader.readFile(args[0]);
        logger.info(String.format("Loaded %s facilities.", scenario.getActivityFacilities().getFacilities().size()));

        ActivityFacilities facilities = scenario.getActivityFacilities();
        List<Id> remove = new ArrayList<>(facilities.getFacilities().size());

        double minx = Double.parseDouble(args[1]);
        double miny = Double.parseDouble(args[2]);
        double maxx = Double.parseDouble(args[3]);
        double maxy = Double.parseDouble(args[4]);

        MathTransform transform = null;
        try {
            transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, CRSUtils.getCRS(31467));
        } catch (FactoryException var11) {
            var11.printStackTrace();
        }

        Coordinate cmin = new Coordinate(minx, miny);
        Coordinate cmax = new Coordinate(maxx, maxy);

        CRSUtils.transformCoordinate(cmin, transform);
        CRSUtils.transformCoordinate(cmax, transform);

        minx = cmin.x;
        miny = cmin.y;
        maxx = cmax.x;
        maxy = cmax.y;

        logger.info(String.format("Bounding box is %s %s %s %s", minx, miny, maxx, maxy));

        logger.info("Filtering facilities...");
        for(ActivityFacility facility : facilities.getFacilities().values()) {
            Coord coord = facility.getCoord();

            if(coord.getX() < minx ||
                    coord.getX() > maxx ||
                    coord.getY() < miny ||
                    coord.getY() > maxy) {
                remove.add(facility.getId());
            }
        }
        logger.info(String.format("Identified %s facilities for removal.", remove.size()));
        for(Id id : remove) {
            facilities.getFacilities().remove(id);
        }

        logger.info(String.format("Remaining %s facilities.", facilities.getFacilities().size()));
        FacilitiesWriter writer = new FacilitiesWriter(facilities);
        writer.write(args[5]);
    }
}
