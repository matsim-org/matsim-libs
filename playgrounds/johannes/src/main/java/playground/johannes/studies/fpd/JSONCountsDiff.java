/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.johannes.studies.fpd;

import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSON;
import org.wololo.geojson.GeoJSONFactory;
import org.wololo.jts2geojson.GeoJSONWriter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author johannes
 */
public class JSONCountsDiff {

    public static void main(String args[]) throws IOException {
        String jsonFile1 = "/Users/johannes/gsv/fpd/telefonica/032016/analysis/20160623/aug-sep-pkw/simCounts.json";
        String jsonFile2 = "/Users/johannes/gsv/fpd/telefonica/032016/analysis/20160623/aug-sep-lkw/simCounts.json";
        String jsonOutFile = "/Users/johannes/gsv/fpd/telefonica/032016/analysis/20160623/aug-sep-lkw/simCounts.lkw-pkw.json";

        Map<Object, Feature> features1 = readJSON(jsonFile1);
        Map<Object, Feature> features2 = readJSON(jsonFile2);

        List<Feature> newFeatures = new ArrayList<>();

        for(Feature feature1 : features1.values()) {
            Feature feature2 = features2.get(feature1.getProperties().get("id"));
            double error1 = Double.parseDouble(feature1.getProperties().get("error").toString());
            double error2 = Double.parseDouble(feature2.getProperties().get("error").toString());
            double errorDiff = Math.abs(error2) - Math.abs(error1);
            errorDiff = Math.max(-1, errorDiff);
            errorDiff = Math.min(1, errorDiff);
            feature2.getProperties().put("errorDiff", errorDiff);
            newFeatures.add(feature2);
        }

        GeoJSONWriter jsonWriter = new GeoJSONWriter();
        FeatureCollection featureCollection = jsonWriter.write(newFeatures);

        BufferedWriter writer = new BufferedWriter(new FileWriter(jsonOutFile));
        writer.write(featureCollection.toString());
        writer.close();
    }

    private static Map<Object, Feature> readJSON(String file) throws IOException {
        String jsonData = new String(Files.readAllBytes(Paths.get(file)));
        GeoJSON json = GeoJSONFactory.create(jsonData);

        Map<Object, Feature> refFeatures = new HashMap<>();

        if(json instanceof FeatureCollection) {
            FeatureCollection features = (FeatureCollection) json;
            for(Feature feature : features.getFeatures()) {
                refFeatures.put(feature.getProperties().get("id"), feature);

            }
        }

        return refFeatures;
    }
}
