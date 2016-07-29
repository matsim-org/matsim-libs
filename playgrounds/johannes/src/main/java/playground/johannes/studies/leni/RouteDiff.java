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

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author johannes
 */
public class RouteDiff {

    public static void main(String args[]) throws IOException {
        Map<String, List<String>> routes1 = buildRoutes("/Users/johannes/Desktop/357.csv");
        Map<String, List<String>> routes2 = buildRoutes("/Users/johannes/Desktop/361.csv");

        BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/johannes/Desktop/diff.txt"));
        writer.write("id\troute1\troute2");
        writer.newLine();

        for(Map.Entry<String, List<String>> entry : routes1.entrySet()) {
            List<String> route1 = entry.getValue();
            List<String> route2 = routes2.get(entry.getKey());

            if(!route1.equals(route2)) {
                writer.write(entry.getKey());
                writer.write("\t");
                writer.write(routeToString(route1));
                writer.write("\t");
                writer.write(routeToString(route2));
                writer.newLine();
            }
        }

        writer.close();
    }

    private static String routeToString(List<String> route) {
        StringBuilder builder = new StringBuilder();
        for(String id : route) {
            if(id != null) {
                builder.append(id);
                builder.append(",");
            }
        }

        return builder.toString();
    }

    private static Map<String, List<String>> buildRoutes(String file) throws IOException {
        Map<String, List<String>> routes = new HashMap<>();

        BufferedReader reader = new BufferedReader(new FileReader(file));

        String line = reader.readLine();

        while((line = reader.readLine()) != null) {
            String[] tokens = line.split(";");

            String commodity = tokens[4];
            String node = tokens[3];
            int idx = Integer.parseInt(tokens[2]);

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
