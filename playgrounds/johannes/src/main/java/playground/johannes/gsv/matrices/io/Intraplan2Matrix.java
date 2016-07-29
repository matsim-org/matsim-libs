/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.matrices.io;

import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLWriter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 */
public class Intraplan2Matrix {

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        /*
		 * read nuts to gsv mappings
		 */
//		ZoneLayer<Map<String, Object>> zones = ZoneLayerSHP.read("/home/johannes/gsv/matrices/zones_zone.SHP");
        ZoneCollection zones = new ZoneCollection(null);
        String data = new String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/de.nuts3.json")));
        zones.addAll(ZoneGeoJsonIO.parseFeatureCollection(data));

        Map<String, String> nuts2gsv = new HashMap<String, String>();
        for (Zone zone : zones.getZones()) {
//			nuts2gsv.put((String) zone.getAttribute().get("CODE"), zone.getAttribute().get("NO").toString());
            nuts2gsv.put(zone.getAttribute("nuts3_code"), zone.getAttribute("gsvId"));
        }
		/*
		 * read itp to nuts mappings
		 */
        Map<String, String> itp2nuts = new HashMap<>();
        Map<String, Boolean> isDE = new HashMap<>();
        BufferedReader reader = new BufferedReader(new FileReader("/home/johannes/gsv/matrices/Lieferung_Intraplan/Zellen_Liefer_Euromat.csv"));
        String line = reader.readLine();

        while ((line = reader.readLine()) != null) {
            String tokens[] = line.split("\t");
            if (tokens.length == 3) {
                itp2nuts.put(tokens[0], tokens[2]);
                if (tokens[2].startsWith("DE")) {
                    isDE.put(tokens[0], true);
                } else {
                    isDE.put(tokens[0], false);
                }
            }
        }
        reader.close();
		/*
		 * nuts2003 to nuts2008
		 */
        Map<String, String> nuts2nuts = new HashMap<>();
        reader = new BufferedReader(new FileReader("/home/johannes/gsv/matrices/Lieferung_Intraplan/nuts2003to2008.txt.csv"));
        line = reader.readLine();
        while ((line = reader.readLine()) != null) {
            String tokens[] = line.split("\t");
            nuts2nuts.put(tokens[0], tokens[1]);
        }
        reader.close();
		/*
		 * read file
		 */
        NumericMatrix m = new NumericMatrix();
        zones.setPrimaryKey("gsvId");
        int notfound = 0;
        reader = new BufferedReader(new FileReader("/home/johannes/gsv/matrices/Lieferung_Intraplan/2007_12_04/Europamatrix_071204.csv"));
        double totaltrips = 0;
        while ((line = reader.readLine()) != null) {
            if (!(line.startsWith("#"))) {
                String tokens[] = line.split(";");
                String itpFrom = tokens[0];
                String itpTo = tokens[1];

                if (itpFrom.startsWith("0")) itpFrom = itpFrom.substring(1);
                if (itpTo.startsWith("0")) itpTo = itpTo.substring(1);

//				if(itpFrom.equalsIgnoreCase("5425")) {
//					int trips = 0;
//					for (int i = 8; i < 14; i++) {
//						trips += Integer.parseInt(tokens[i]);
//					}
//					totaltrips += trips;
//				}

                String nutsFrom = itp2nuts.get(itpFrom);
                String nutsTo = itp2nuts.get(itpTo);

//				if(nutsFrom != null && nutsFrom.equals("DE929")) {
//					System.out.println();
//				}

                if (nuts2nuts.containsKey(nutsFrom)) nutsFrom = nuts2nuts.get(nutsFrom);
                if (nuts2nuts.containsKey(nutsTo)) nutsTo = nuts2nuts.get(nutsTo);

                if (nutsFrom != null && nutsTo != null) {
                    if (nutsFrom.startsWith("DE") && nutsTo.startsWith("DE")) {
//					if(isDE.get(itpTo) && isDE.get(itpFrom)) {

                        String gsvIdFrom = nuts2gsv.get(nutsFrom);
//                        Zone gsvFrom = zones.get(gsvIdFrom);

                        String gsvIdTo = nuts2gsv.get(nutsTo);
//                        Zone gsvTo = zones.get(gsvIdTo);

                        int trips = 0;
                        for (int i = 8; i < 14; i++) {
                            trips += Integer.parseInt(tokens[i]);
                        }
                        totaltrips += trips;

                        if (gsvIdFrom != null && gsvIdTo != null) {
							/*
							 * outward trip
							 */
//							if(gsvFrom.equalsIgnoreCase("5382")) {
//								System.out.println();
//							}
//							Entry e = m.getEntry(gsvFrom, gsvTo);
//							if(e == null) {
//								m.createEntry(gsvFrom, gsvTo, trips);
//							} else {
//								e.setValue(e.getValue() + trips);
//							}
                            Double val = m.get(gsvIdFrom, gsvIdTo);
                            if (val == null) {
                                val = new Double(0);
                            }
                            m.set(gsvIdFrom, gsvIdTo, val + trips);
							/*
							 * return trip
							 */
//							e = m.getEntry(gsvTo, gsvFrom);
//							if(e == null) {
//								m.createEntry(gsvTo, gsvFrom, trips);
//							} else {
//								e.setValue(e.getValue() + trips);
//							}
                            val = m.get(gsvIdTo, gsvIdFrom);
                            if (val == null) {
                                val = new Double(0);
                            }
                            m.set(gsvIdTo, gsvIdFrom, val + trips);
                        } else {
                            notfound++;
//							System.out.println(String.format("Cell not found: %s -> %s", nutsFrom, nutsTo));
                        }
                    }
                }
            }
        }
        System.out.println("Cells not found: " + notfound);
        System.out.println("Total trips = " + totaltrips);

        reader.close();

        NumericMatrixXMLWriter writer = new NumericMatrixXMLWriter();
		writer.write(m, "/home/johannes/gsv/matrices/itp.xml");
    }

}
