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

import org.wololo.geojson.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author johannes
 */
public class NetworkGeoJson2Csv {

    public static void main(String args[]) throws IOException {
        String file = "/Users/johannes/gsv/fpd/telefonica/032016/analysis/20160623/aug-sep-pkw/obsCounts.json";
        String out = "/Users/johannes/gsv/fpd/telefonica/032016/analysis/20160623/aug-sep-pkw/obsCounts.txt";

        String jsonData = new String(Files.readAllBytes(Paths.get(file)));
        GeoJSON json = GeoJSONFactory.create(jsonData);

        BufferedWriter writer = new BufferedWriter(new FileWriter(out));
        writer.write("id\tvolume\tload");
        writer.newLine();

        if(json instanceof FeatureCollection) {
            FeatureCollection features = (FeatureCollection) json;
            for(Feature feature : features.getFeatures()) {
                String id = feature.getProperties().get("id").toString();
                String name = "noname";
                String volume = feature.getProperties().get("simulation").toString();
                String error = feature.getProperties().get("error").toString();

                Point point = (Point) feature.getGeometry();
                String lat = String.valueOf(point.getCoordinates()[0]);
                String lon = String.valueOf(point.getCoordinates()[1]);

                writer.write(id);
                writer.write("\t");
                writer.write(name);
                writer.write("\t");
                writer.write(lat);
                writer.write("\t");
                writer.write(lon);
                writer.write("\t");
                writer.write(volume);
                writer.write("\t");
                writer.write(error);
                writer.newLine();
            }
        }

        writer.close();
    }
}
