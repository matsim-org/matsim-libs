package org.matsim.application.prepare.freight.dataProcessing;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GermanNutsTransformation {
    private final ShpOptions oldShapeFile;
    private final ShpOptions shapeFile2021;

    private final Map<String, String> nuts2006To2021Mapping = new HashMap<>();
    private static int counter = 0;

    GermanNutsTransformation(ShpOptions german2006shp, ShpOptions nuts2021shp) {
        this.oldShapeFile = german2006shp;
        this.shapeFile2021 = nuts2021shp;
        generateTransformationMap();
    }

    private void generateTransformationMap() {
        CoordinateTransformation ct = new GeotoolsTransformation("EPSG:5677", "EPSG:4326");
        List<SimpleFeature> featuresNuts2021 = shapeFile2021.readFeatures();
        Map<String, SimpleFeature> germanNuts2021Lv3Features = new HashMap<>();
        for (SimpleFeature feature : featuresNuts2021) {
            if (feature.getAttribute("NUTS_ID").toString().startsWith("DE")
                    && feature.getAttribute("LEVL_CODE").toString().equals("3")) {
                germanNuts2021Lv3Features.put(feature.getAttribute("NUTS_ID").toString(), feature);
            }
        }
        System.out.println("There are " + germanNuts2021Lv3Features.size() + " NUTS-3 regions in the new shape file (2021 version)");

        List<SimpleFeature> oldFeatures = oldShapeFile.readFeatures();
        for (SimpleFeature feature2006 : oldFeatures) {
            String oldNutsId = (String) feature2006.getAttribute("NUTS_ID");
            String oldNutsName = (String) feature2006.getAttribute("NUTS_NAME");
            if (germanNuts2021Lv3Features.containsKey(oldNutsId) &&
                    germanNuts2021Lv3Features.get(oldNutsId).getAttribute("NUTS_NAME").toString().equals(oldNutsName)) {
                nuts2006To2021Mapping.put(oldNutsId, oldNutsId); // NUTS region remains the same (mapping to itself)
            } else {
                counter++;
                System.out.println("NUTS region has changed for " + oldNutsName);
                // Use geometry to decide new NUTS region
                Geometry geometry = (Geometry) feature2006.getDefaultGeometry();
                Point centroid = geometry.getCentroid();
                Coord coord = ct.transform(MGC.point2Coord(centroid));
                for (SimpleFeature feature2021 : germanNuts2021Lv3Features.values()) {
                    Geometry geometry2021Candidate = (Geometry) feature2021.getDefaultGeometry();
                    if (geometry2021Candidate.contains(MGC.coordinate2Point(MGC.coord2Coordinate(coord)))) {
                        String nutsId2021 = feature2021.getAttribute("NUTS_ID").toString();
                        nuts2006To2021Mapping.put(oldNutsId, nutsId2021);
                        System.out.println("Old NUTS ID:" + oldNutsId + " Old NUTS name = " + feature2006.getAttribute("NUTS_NAME"));
                        System.out.println("New NUTS ID: " + nutsId2021 + " New NUTS name = " + feature2021.getAttribute("NUTS_NAME"));
                        System.out.println("=====================================================");
                    }
                }
            }
        }
    }

    public Map<String, String> getNuts2006To2021Mapping() {
        return nuts2006To2021Mapping;
    }

    public static int getCounter() {
        return counter;
    }

    public static void main(String[] args) {
        String german2006shp = "/Users/luchengqi/Documents/MATSimScenarios/GermanFreight/to-put-on-public-svn/raw-data/NUTS3/NUTS3_2010_DE.shp"; //TODO
        String nuts2021shp = "/Users/luchengqi/Documents/MATSimScenarios/GermanFreight/NUTS_RG_20M_2016_4326.shp/NUTS_RG_20M_2016_4326.shp";
        ShpOptions oldShapeFile = new ShpOptions(Path.of(german2006shp), "EPSG:5677", StandardCharsets.UTF_8);
        ShpOptions shapeFile2021 = new ShpOptions(Path.of(nuts2021shp), "EPSG:4326", StandardCharsets.UTF_8);
        GermanNutsTransformation germanNutsTransformation = new GermanNutsTransformation(oldShapeFile, shapeFile2021);
        Map<String, String> transformationMap = germanNutsTransformation.getNuts2006To2021Mapping();

        System.out.println("Mapping results:" + transformationMap.size() + " mapping is calculated");
        System.out.println("There are " + getCounter() + " NUTS regions that has been changed from 2006 version to 2021 version. " +
                "Manual inspection and modification is needed");
    }
}
