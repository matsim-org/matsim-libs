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

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author johannes
 */
public class ExtractRoutes {

    private static final String COMMODITY_RESULTS = "CommodityResults";

    private static final String COMMODITY_PATH = "CommodityPath";

    private static final String COMMODITY_ID = "ExternId";

    private static final String NODE_ID = "NodeExternId";

    private static final String NODE_IDX = "Index";

    public static final void main(String args[]) throws IOException {
        XMLReader reader = new XMLReader();
        reader.setValidating(false);
        reader.parse("/Users/johannes/Desktop/341.xml");
        Map<String, List<String>> routes1 = reader.getRoutes();

        reader = new XMLReader();
        reader.setValidating(false);
        reader.parse("/Users/johannes/Desktop/351d.xml");
        Map<String, List<String>> routes2 = reader.getRoutes();

        Set<String> ids = new TreeSet<>(routes1.keySet());
        ids.addAll(routes2.keySet());

        BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/johannes/Desktop/diff4.txt"));
        for(String id : ids) {
            List<String> route1 = routes1.get(id);
            List<String> route2 = routes2.get(id);

            if(route1 != null && route2 != null) {
                boolean diff = false;
                int size = Math.max(route1.size(), route2.size());
                for(int i = 0; i < size; i++) {
                    String node1 = null;
                    String node2 = null;
                    if(route1.size() > i) node1 = route1.get(i);
                    if(route2.size() > i) node2 = route2.get(i);

                    if(node1 == null || node2 == null) {
                        diff = true;
                        break;
                    } else {
                        if(!node1.equalsIgnoreCase(node2)) {
                            diff = true;
                            break;
                        }
                    }
                }

                if(diff) {
                    writer.write(id);
                    writer.write(" route1=[");
                    for(String nodeId : route1) {
                        writer.write(nodeId);
                        writer.write(" ");
                    }
                    writer.write("] route2=[");
                    for(String nodeId : route2) {
                        writer.write(nodeId);
                        writer.write(" ");
                    }
                    writer.write("]");
                    writer.newLine();
                }
            } else {
                writer.write(id);
                writer.write(" ");
                if(route1 == null) {
                    writer.write("route1=null");
                } else {
                    writer.write("route2=null");
                }
                writer.newLine();
            }

        }
        writer.close();
    }

    private static class XMLReader extends MatsimXmlParser {

        private SortedMap<Double, Map<String, List<String>>> solutions = new TreeMap<>();

        private Map<String, List<String>> routes;

        private Double objectiveValue;

        private Map<Integer, String> route;

        private String commodityId;

        private String nodeId;

        private String nodeIdx;

        private boolean ignore = true;

        public Map<String, List<String>> getRoutes() {
            return solutions.get(solutions.firstKey());
        }

        @Override
        public void startTag(String name, Attributes atts, Stack<String> context) {
            if("Solution".equalsIgnoreCase(name)) {
                routes = new TreeMap<>();
            } else if(COMMODITY_RESULTS.equalsIgnoreCase(name)) {
                route = new TreeMap<>();
            }
        }

        @Override
        public void endTag(String name, String content, Stack<String> context) {
            if("Solution".equalsIgnoreCase(name)) {
                solutions.put(objectiveValue, routes);
                objectiveValue = null;
                route = null;
            } else if("ObjectiveValue".equalsIgnoreCase(name)) {
                if(context.lastElement().equalsIgnoreCase("Solution")) {
                    objectiveValue = new Double(content);
                }

            } else if (COMMODITY_RESULTS.equalsIgnoreCase(name)) {
                    routes.put(commodityId, new ArrayList<>(route.values()));
                    route = null;
                    commodityId = null;
                    nodeId = null;
                    nodeIdx = null;
                } else if (COMMODITY_ID.equalsIgnoreCase(name)) {
                    commodityId = new String(content);
                } else if (COMMODITY_PATH.equalsIgnoreCase(name)) {
                    route.put(new Integer(nodeIdx), nodeId);
                    nodeId = null;
                    nodeIdx = null;
                } else if (NODE_ID.equalsIgnoreCase(name)) {
                    nodeId = new String(content);
                } else if (NODE_IDX.equalsIgnoreCase(name)) {
                    nodeIdx = new String(content);
                }

        }
    }
}
