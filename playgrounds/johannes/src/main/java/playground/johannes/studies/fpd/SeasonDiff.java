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

import com.vividsolutions.jts.geom.Point;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.jts2geojson.GeoJSONWriter;
import playground.johannes.coopsim.utils.MatsimCoordUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author johannes
 */
public class SeasonDiff {

    public static void main(String args[]) throws IOException {
        String countsFile = "/Users/johannes/gsv/fpd/telefonica/032016/analysis/20160623/counts.2014.net20140909.5.24h.id.xml";
        String volumeFile = "/Users/johannes/gsv/bast/counts.aug-sep.rates.txt";
        String jsonFile = "/Users/johannes/gsv/fpd/telefonica/032016/analysis/20160623/aug-sep-pkw/rates.json";

        Map<String, Double> rates = new HashMap<>();
        Map<String, Double> volumes = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader(volumeFile));
        String line = reader.readLine();
        while((line = reader.readLine()) != null) {
            String tokens[] = line.split(";");
            String id = tokens[0];
            double val_R1 = Double.parseDouble(tokens[13]);
            double val_R2 = Double.parseDouble(tokens[14]);
            double vol_R1 = Double.parseDouble(tokens[5]);
            double vol_R2 = Double.parseDouble(tokens[6]);

            rates.put(id + "_R1", val_R1 - 1);
            rates.put(id + "_R2", val_R2 - 1);
            volumes.put(id + "_R1", vol_R1);
            volumes.put(id + "_R2", vol_R2);
        }
        reader.close();

        Counts<Link> counts = new Counts();
        MatsimCountsReader cReader = new MatsimCountsReader(counts);
        cReader.readFile(countsFile);

        MathTransform transform = null;
        try {
            transform = CRS.findMathTransform(CRSUtils.getCRS(31467), CRSUtils.getCRS(4326));
        } catch (FactoryException e) {
            e.printStackTrace();
        }

        GeoJSONWriter jsonWriter = new GeoJSONWriter();
        List<Feature> features = new ArrayList<>(counts.getCounts().size());

        for(Count<Link> count : counts.getCounts().values()) {

            Double relErr = rates.get(count.getCsId().toString());
            if(relErr != null) {
                Double vol = volumes.get(count.getCsId().toString());
                Coord obsPos = count.getCoord();

                Point obsPoint = MatsimCoordUtils.coordToPoint(obsPos);

                obsPoint = CRSUtils.transformPoint(obsPoint, transform);

                Map<String, Object> properties = new HashMap<>();

                properties.put("id", count.getCsId());
                properties.put("error", relErr);
                properties.put("observation", vol);

                Feature obsFeature = new Feature(jsonWriter.write(obsPoint), properties);

                features.add(obsFeature);
            }
        }

        FeatureCollection fCollection = jsonWriter.write(features);
        BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile));
        writer.write(fCollection.toString());
        writer.close();
    }
}
