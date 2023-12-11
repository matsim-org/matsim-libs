package org.matsim.contrib.accessibility.utils;

import com.google.gson.Gson;
import org.matsim.api.core.v01.Coord;

import java.util.*;

public class GeoJsonPolygonFeatureWriter {

    private BoundingBox boundingBox;
    private List<AccessibilityPoi> accessibilityPoiList = new ArrayList<>();

    GeoJsonPolygonFeatureWriter(BoundingBox boundingBox) {

        this.boundingBox = boundingBox;
    }

    public void setAccessibilityPoiList(List<AccessibilityPoi> accessibilityPoiList) {
        this.accessibilityPoiList = accessibilityPoiList;
    }

    public void addPoiData(AccessibilityPoi accessibilityPoi) {

        this.accessibilityPoiList.add(accessibilityPoi);
    }

    public String asGeoJson() {

        List<Map<String, Object>> featuresList = parseFeatureList();

        Map<String, Object> featureCollectionMap = new LinkedHashMap<>();
        featureCollectionMap.put("type", "FeatureCollection");
        featureCollectionMap.put("totalFeatures", featuresList.size());
        featureCollectionMap.put("features", featuresList);
        featureCollectionMap.put("crs", parseCRS());
        featureCollectionMap.put("bbox", boundingBox.getBoundingBox());
        return new Gson().toJson(featureCollectionMap);
    }

    private List<Map<String, Object>> parseFeatureList() {

        List<Map<String, Object>> featuresList = new ArrayList<>();
        for (AccessibilityPoi poi : accessibilityPoiList) {

            List<List<List<Double>>> coordinatesList = new ArrayList<>();
            coordinatesList.add(poi.getGeometry().getCoordinates());

            Map<String, Object> geometryMap = new LinkedHashMap<>();
            geometryMap.put("type", "Polygon");
            geometryMap.put("coordinates", coordinatesList);

            Map<String, Object> propertiesMap = new LinkedHashMap<>();
            propertiesMap.put("properties", poi.getProperties().getProperties());

            Map<String, Object> featureMap = new LinkedHashMap<>();
            featureMap.put("type", poi.getTYPE());
            featureMap.put("id", poi.getId());
            featureMap.put("geometry", geometryMap);
            featureMap.put("geometry_name", poi.getGEOMETRY_NAME());
            featureMap.put("properties", propertiesMap);
            featuresList.add(featureMap);
        }
        return featuresList;
    }

    private Map<String, Object> parseCRS() {

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("name", "urn:ogc:def:crs:EPSG::4326");

        Map<String, Object> crs = new LinkedHashMap<>();
        crs.put("type", "name");
        crs.put("properties", properties);

        return crs;
    }

    static public class AccessibilityPoi {

        private String TYPE = "Feature";
        private String id;
        private Geometry geometry;
        private final String GEOMETRY_NAME = "the_geom";
        private Properties properties;

        public AccessibilityPoi(String id, Geometry geometry, Properties properties) {
            this.id = id;
            this.geometry = geometry;
            this.properties = properties;
        }

        String getTYPE() {
            return TYPE;
        }

        String getId() {
            return id;
        }

        Geometry getGeometry() {
            return geometry;
        }

        String getGEOMETRY_NAME() {
            return GEOMETRY_NAME;
        }

        Properties getProperties() {
            return properties;
        }
    }

    static public class Geometry {

        private String type;
        private List<Coord> coordinates;

        public Geometry(String type, List<Coord> coordinates) {

            this.type = type;
            this.coordinates = coordinates;
        }

        String getType() {
            return type;
        }

        List<List<Double>> getCoordinates() {

            List<List<Double>> result = new ArrayList<>();
            for (Coord coord : coordinates) {

                result.add(Arrays.asList(coord.getX(), coord.getY()));
            }
            return result;
        }
    }

    static public class Properties {

        private String id;
        private int time;
        private double freespeed;
        private double car;
        private double walk;
        private double bike;
        private double pt;
        private double matrixBasedPt;
        private BoundingBox bbox;

        public Properties(String id, int time, double freespeed, double car, double walk, double bike, double pt, double matrixBasedPt, BoundingBox bbox) {
            this.id = id;
            this.time = time;
            this.freespeed = freespeed;
            this.car = car;
            this.walk = walk;
            this.bike = bike;
            this.pt = pt;
            this.matrixBasedPt = matrixBasedPt;
            this.bbox = bbox;
        }

        Map<String, Object> getProperties() {

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", id);
            result.put("time", time);
            result.put("freespeed", freespeed);
            result.put("car", car);
            result.put("walk", walk);
            result.put("bike", bike);
            result.put("pt", pt);
            result.put("matrixBasedPt", matrixBasedPt);
            result.put("bbox", bbox.getBoundingBox());
            return result;
        }
    }

    static public class BoundingBox {

        private double minX;
        private double minY;
        private double maxX;
        private double maxY;

        public BoundingBox(double minX, double minY, double maxX, double maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        List<Double> getBoundingBox() {
            return Arrays.asList(minX, minY, maxX, maxY);
        }
    }
}
