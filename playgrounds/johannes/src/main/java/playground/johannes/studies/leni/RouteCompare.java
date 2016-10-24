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
import java.util.*;

/**
 * @author johannes
 */
public class RouteCompare {

    public static void main(String args[]) throws IOException {
        String refFile = "/Volumes/GSV/C_Vertrieb/2015_06_01_StrategieServiceeinrichtungen/02_Analysen/Sendungsrouten/357.csv";
        String expFile = "/Volumes/GSV/C_Vertrieb/2015_06_01_StrategieServiceeinrichtungen/02_Analysen/Sendungsrouten/366.csv";
        String nodeId = "95045";
        String outFile = "/Volumes/GSV/C_Vertrieb/2015_06_01_StrategieServiceeinrichtungen/02_Analysen/Sendungsrouten/out.txt";

        Map<String, Route> refScenario = readRoutes(refFile, nodeId);
        Map<String, Route> expScenario = readRoutes(expFile, nodeId);

        BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
        writer.write("id\trefLinks\trefLength\texpLinks\texpLength\tflag");
        writer.newLine();

        for(Map.Entry<String, Route> entry : refScenario.entrySet()) {
            Route refRoute = entry.getValue();
//            if(refRoute.isAffected) {
                writer.write(refRoute.id);
                writer.write("\t");
                writer.write(String.valueOf(refRoute.links));
                writer.write("\t");
                writer.write(String.valueOf(refRoute.length));

                Route expRoute = expScenario.get(refRoute.id);
                if(expRoute != null) {
                    writer.write("\t");
                    writer.write(String.valueOf(expRoute.links));
                    writer.write("\t");
                    writer.write(String.valueOf(expRoute.length));
                }
                writer.write("\t");
                writer.write(refRoute.flag);
                writer.newLine();
//            }
        }
        writer.close();
    }

    private static Map<String, Route> readRoutes(String filename, String nodeId) throws IOException {
        Map<String, List<Link>> tmpRoutes = new HashMap<>();

        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line = reader.readLine();

        while((line = reader.readLine()) != null) {
            String tokens[] = line.split("\t");

            Link link = new Link();
            link.index = Integer.parseInt(tokens[1]);
            link.length = Double.parseDouble(tokens[2]);
            link.sourceNode = tokens[3];
            link.targetNode = tokens[4];

            String commodityId = tokens[0];
            List<Link> route = tmpRoutes.get(commodityId);
            if(route == null) {
                route = new ArrayList<>();
                tmpRoutes.put(commodityId, route);
            }
            route.add(link);
        }

        reader.close();

        Map<String, Route> routes = new HashMap<>();

        for(Map.Entry<String, List<Link>> entry : tmpRoutes.entrySet()) {
            List<Link> route = entry.getValue();
            Collections.sort(route, new Comparator<Link>() {
                @Override
                public int compare(Link o1, Link o2) {
                    return Integer.compare(o1.index, o2.index);
                }
            });
            double length = 0;
            boolean isAffected = false;
            for(Link link : entry.getValue()) {
                length += link.length;
                if(link.sourceNode.equals(nodeId) || link.targetNode.equals(nodeId)) isAffected = true;
            }

            Route newRoute = new Route();
            newRoute.id = entry.getKey();
            newRoute.isAffected = isAffected;
            newRoute.length = length;
            newRoute.links = entry.getValue().size();

            if(route.get(0).sourceNode.equals(nodeId)) newRoute.flag = "start";
            else if(route.get(route.size()-1).targetNode.equals(nodeId)) newRoute.flag = "end";
            else if(isAffected) newRoute.flag = "over";
            else newRoute.flag = "";

            routes.put(newRoute.id, newRoute);
        }


        return routes;
    }

    private static class Link {

        private int index;

        private String sourceNode;

        private String targetNode;

        private double length;
    }

    private static class Route {

        String id;

        boolean isAffected;

        double length;

        int links;

        String flag;
    }
}
