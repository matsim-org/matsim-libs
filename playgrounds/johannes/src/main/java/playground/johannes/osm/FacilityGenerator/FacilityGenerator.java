package playground.johannes.osm.FacilityGenerator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.facilities.*;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import playground.johannes.coopsim.utils.MatsimCoordUtils;
import playground.johannes.gsv.synPop.osm.OSMNode;
import playground.johannes.gsv.synPop.osm.OSMWay;
import playground.johannes.gsv.synPop.osm.XMLParser;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;


public class FacilityGenerator {

    private static final Logger logger = Logger.getLogger(FacilityGenerator.class);

    public static void main(String[] args) throws Exception {

        if (args.length != 4) {
            System.out.println("Wrong arguments provided. Pattern: [0] path to osm file [1] output xml path [2] minimum area for unclassified buildings [3] tag2type csv file");
            return;
        }

        FileAppender fileAppender = new FileAppender(new SimpleLayout(), "logs/" + LocalDate.now().toString() + "_" + LocalTime.now().getHour() + "-" + LocalTime.now().getMinute() + ".log", false);
        logger.addAppender(fileAppender);

        Set<OSMObject> objects = parseOsm(args[0], args[3]);

        logger.info("Transforming objects...");
        transform(objects);

        logger.info("Building facilities...");

        ActivityFacilities facilities = FacilitiesUtils.createActivityFacilities();

        Double minimumSize = Double.parseDouble(args[2]);
        synthesize(facilities, objects, minimumSize);

        logger.info(String.format("Created %s facilities.", facilities.getFacilities().size()));

        FacilitiesWriter writer = new FacilitiesWriter(facilities);

        writer.write(args[1]);

    }

    static Set<OSMObject> parseOsm(String input, String tag2Type) throws SQLException {

        Mapping.initTag2TypeFromCsv(tag2Type);

        XMLParser parser = new XMLParser();
        parser.setValidating(false);
        parser.readFile(input);


        Collection<OSMWay> ways = parser.getWays().values();
        Collection<OSMNode> nodes = parser.getNodes().values();

        OSMObjectBuilder builder = new OSMObjectBuilder();
        Set<OSMObject> objects = new HashSet<OSMObject>();
        logger.info("Processing ways...");
        ProgressLogger.init(ways.size(), 1, 10);
        int failures = 0;
        for (OSMWay way : ways) {
            OSMObject obj = builder.build(way);
            if (obj != null)
                objects.add(obj);
            else
                failures++;
            ProgressLogger.step();
        }
        ways.clear();


        logger.info("Processing nodes...");
        ProgressLogger.init(nodes.size(), 1, 10);
        for (OSMNode node : nodes) {
            OSMObject obj = builder.build(node);
            if (obj != null) {
                objects.add(obj);
            } else
                failures++;
            ProgressLogger.step();
        }
        nodes.clear();

        logger.info(String.format("Total built %s objects, %s failures.", objects.size(), failures));
        logger.info(String.format("Total built %s objects, %s failures.", objects.size(), failures));
        for (Map.Entry<String, Integer> counters : builder.getTypeCounter().entrySet()) {
            logger.info(String.format("Built %d objects of type %s.", counters.getValue(), counters.getKey()));
        }
        return objects;
    }

    private static void transform(Collection<OSMObject> objects) {
        MathTransform transform = null;

        try {
            transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, CRSUtils.getCRS(31467));
        } catch (FactoryException var11) {
            var11.printStackTrace();
        }

        Iterator it = objects.iterator();

        while (it.hasNext()) {
            OSMObject obj = (OSMObject) it.next();
            Coordinate[] coords = obj.getGeometry().getCoordinates();
            int length = coords.length;

            for (int i = 0; i < length; ++i) {
                Coordinate coord = coords[i];
                double[] points = new double[]{coord.x, coord.y};

                try {
                    transform.transform(points, 0, points, 0, 1);
                } catch (TransformException var10) {
                    var10.printStackTrace();
                }

                coord.x = points[0];
                coord.y = points[1];
            }
        }
    }

    public static void synthesize(ActivityFacilities facilities, Collection<OSMObject> objects, double minimumSize) {
        Quadtree quadTree = new Quadtree();

        logger.info("Inserting areas in quad tree...");
        ProgressLogger.init(objects.size(), 1, 10);
        for (OSMObject obj : objects) {
            if (obj.getObjectType().equalsIgnoreCase(OSMObject.AREA)) {
                Envelope env = obj.getGeometry().getEnvelopeInternal();
                if (env != null) {
                    quadTree.insert(env, obj); // exponent out of bounds exception?
                }
            }
            ProgressLogger.step();
        }

        Map<OSMObject, Set<OSMObject>> areaBuildingMap = new HashMap<OSMObject, Set<OSMObject>>();

        logger.info("Assigning buildings to areas...");
        ProgressLogger.init(objects.size(), 1, 10);
        for (OSMObject obj : objects) {
            if (obj.getObjectType().equalsIgnoreCase(OSMObject.BUILDING)) {
                List<OSMObject> areas = quadTree.query(obj.getGeometry().getEnvelopeInternal());

                for (OSMObject area : areas) {
                    if (area.getGeometry().contains(obj.getGeometry())) {
                        Set<OSMObject> buildings = areaBuildingMap.get(area);
                        if (buildings == null) {
                            buildings = new HashSet<OSMObject>();
                            areaBuildingMap.put(area, buildings);
                        }
                        buildings.add(obj);
                    }
                }
            }
            ProgressLogger.step();
        }

//        int idCounter = 0;

        int inheritFromAreaCounter = 0;
        int fallBackToHomeCounter = 0;
        int building2FacilityCounter = 0;

        logger.info("Creating buildings in areas...");
        ProgressLogger.init(areaBuildingMap.size(), 1, 10);
        for (Map.Entry<OSMObject, Set<OSMObject>> entry : areaBuildingMap.entrySet()) {
            OSMObject area = entry.getKey();
//            double A = area.getGeometry().getArea();
//            double size = defaultSize * defaultSize;
//            double n = A / size;

//            if (entry.getValue().size() < n) {
//                int n2 = (int) (n - entry.getValue().size());
//
//                for (int i = 0; i < n2; i++) {
//                    Coord c = generateRandomCoordinate(area.getGeometry());
//                    ActivityFacility facility = facilities.getFactory().createActivityFacility(
//                            Id.create("new" + idCounter++, ActivityFacility.class), c);
//                    facilities.addActivityFacility(facility);
//                }
//            }

            for (OSMObject building : entry.getValue()) {
                Coord c = MatsimCoordUtils.pointToCoord(building.getGeometry().getCentroid());
                Id<ActivityFacility> id = Id.create(building.getId(), ActivityFacility.class);
                String buildingType = building.getFacilityType();
                String areaType = area.getFacilityType();
                if (facilities.getFacilities().get(id) == null) {
                    objects.remove(building);
                    if (buildingType.equals("unclassified")) {
                        buildingType = areaType;
                        inheritFromAreaCounter++;
                    }
                    createActivityFacility(facilities, id, c, buildingType);
                    building2FacilityCounter++;
                }
            }
        }
        ProgressLogger.step();

        logger.info(String.format("Built %d facilities from buildings in areas. %d classified from area", building2FacilityCounter, inheritFromAreaCounter));

        Quadtree buildingTree = new Quadtree();

        fallBackToHomeCounter = 0;
        building2FacilityCounter = 0;


        logger.info("Processing buildings...");
        int skippedSmall = 0;
        ProgressLogger.init(objects.size(), 1, 10);
        for (OSMObject building : objects) {
            if (building.getObjectType().equalsIgnoreCase(OSMObject.BUILDING)) {
                Coord c = MatsimCoordUtils.pointToCoord(building.getGeometry().getCentroid());

                String buildingType = building.getFacilityType();
                if (buildingType.equals("unclassified")) {
                    if (building.getGeometry().getArea() < minimumSize) {
                        skippedSmall++;
                        continue;
                    } else {
                        buildingType = "home";
                        fallBackToHomeCounter++;
                    }
                }

                createActivityFacility(facilities, Id.create(building.getId(), ActivityFacility.class), c, buildingType);
                building2FacilityCounter++;
                buildingTree.insert(building.getGeometry().getEnvelopeInternal(), building);
            }
            ProgressLogger.step();
        }
        logger.info(String.format("Built %d facilities. %d classified as 'home' fallback. Skipped %d too small unclassified", building2FacilityCounter, fallBackToHomeCounter, skippedSmall));

        int poiCounter = 0;
        int poiIgnored = 0;
        int poiFailure = 0;
        logger.info("Processing POIs...");
        ProgressLogger.init(objects.size(), 1, 10);
        for (OSMObject poi : objects) {
            if (poi.getObjectType().equalsIgnoreCase(OSMObject.POI)) {
                List<OSMObject> result = buildingTree.query(poi.getGeometry().getEnvelopeInternal());
                boolean hit = false;
                for (OSMObject geo : result) {
                    if (geo.getGeometry().contains(poi.getGeometry())) {
                        hit = true;
                        poiIgnored++;
                        break;
                    }
                }

                // check if in area
                if (!hit) {
                    result = quadTree.query(poi.getGeometry().getEnvelopeInternal());
                    hit = false;
                    for (OSMObject geo : result) {
                        if (geo.getGeometry().contains(poi.getGeometry())) {
                            hit = true;
                            poiIgnored++;
                            break;
                        }
                    }
                }

                if (!hit) {
                    String type = poi.getFacilityType();
                    if (type.equals("unclassified")) {
                        poiFailure++;
                        continue;
                    }
                    Coord c = MatsimCoordUtils.pointToCoord(poi.getGeometry().getCentroid());
                    createActivityFacility(facilities, Id.create("poi" + poi.getId(), ActivityFacility.class), c, type);
                    poiCounter++;
                }
            }
            ProgressLogger.step();
        }
        logger.info(String.format("Created %d POI. %d ignored, %d failed due to unrecognised activity type.", poiCounter, poiIgnored, poiFailure));
    }


    private static void createActivityFacility(ActivityFacilities facilities, Id<ActivityFacility> id, Coord coord, String type) {

        ActivityFacility facility = facilities.getFactory().createActivityFacility(id, coord);
        String[] types = type.split(";");
        for (int i = 0; i < types.length; i++) {
            type = types[i];
            facility.addActivityOption(new ActivityOptionImpl(type));
        }
        facilities.addActivityFacility(facility);
    }

//    private Coord generateRandomCoordinate(Geometry geometry) {
//        Envelope env = geometry.getEnvelopeInternal();
//        double deltaX = env.getMaxX() - env.getMinX();
//        double deltaY = env.getMaxY() - env.getMinY();
//        boolean hit = false;
//        double x = 0.0D;
//        double y = 0.0D;
//
//        for(Point p = null; !hit; hit = geometry.contains(p)) {
//            x = env.getMinX() + this.random.nextDouble() * deltaX;
//            y = env.getMinY() + this.random.nextDouble() * deltaY;
//            p = this.factory.createPoint(new Coordinate(x, y));
//        }
//
//        return new Coord(x, y);
//    }
}
