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

package playground.johannes.studies.leni;

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.matsim.contrib.common.collections.CollectionUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author johannes
 */
public class TypeHistogram {

    public static void main(String args[]) throws IOException {
        Map<String, List<String>> routes = buildRoutes("/Users/johannes/gsv/anlagen-prognose/357.csv");
        TObjectDoubleMap<String> hist = new TObjectDoubleHashMap<>();

        for(List<String> route : routes.values()) {
            StringBuilder builder = new StringBuilder();
            for(String type : route) {
                if(type != null) {
                    builder.append(str2Type(type));
                    builder.append("-");
                }
            }
            String sequence;
            if(builder.length() < 5) {
                sequence = "keine Umstellung";
            } else {
                sequence = builder.substring(0, builder.length() - 5);
            }
            hist.adjustOrPutValue(sequence, 1, 1);
        }

//        Histogram.normalize(hist);
        Map<String, Double> hist2 = new HashMap<>();
        TObjectDoubleIterator<String> it = hist.iterator();
        for(int i = 0; i < hist.size();  i++) {
            it.advance();
            hist2.put(it.key(), it.value());
        }
        hist2 = CollectionUtils.sortByValue(hist2, true);

        BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/johannes/gsv/anlagen-prognose/357-hist.txt"));
        writer.write("sequence\tproba");
        writer.newLine();

        for(Map.Entry<String, Double> entry : hist2.entrySet()) {
            writer.write(entry.getKey());
            writer.write("\t");
            writer.write(String.valueOf(entry.getValue()));
            writer.newLine();
        }

        writer.close();
    }

    private static String str2Type(String str) {
        if(str.equals("1")) return "Rbf";
        else if(str.equals("2")) return "Kbf";
        else if(str.equals("3")) return "Smr";
        else if(str.equals("4")) return "Sat";
        else return "NA";
    }

    private static Map<String, List<String>> buildRoutes(String file) throws IOException {
        Map<String, List<String>> routes = new HashMap<>();

        BufferedReader reader = new BufferedReader(new FileReader(file));

        String line = reader.readLine();

        while((line = reader.readLine()) != null) {
            String[] tokens = line.split(";");

            String commodity = tokens[0];
            String node = tokens[2];
            int idx = Integer.parseInt(tokens[3]);

            List<String> route = routes.get(commodity);
            if(route == null) {
                route = new ArrayList<>();
                for(int i = 0; i < 10; i++) {
                    route.add(null);
                }
                routes.put(commodity, route);
            }

            route.set(idx, node);
        }

        return routes;
    }
}
