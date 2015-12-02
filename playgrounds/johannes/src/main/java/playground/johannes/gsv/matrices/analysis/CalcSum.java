/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.matrices.analysis;

import org.apache.log4j.Logger;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.MatrixOperations;
import playground.johannes.gsv.zones.io.VisumOMatrixReader;

import java.io.IOException;

/**
 * @author johannes
 */
public class CalcSum {

    private static final Logger logger = Logger.getLogger(CalcSum.class);

//    public static void main(String args[]) {
//        KeyMatrixXMLReader reader = new KeyMatrixXMLReader();
//        reader.setValidating(false);
//        reader.parse("/home/johannes/sge/prj/matsim/run/912/output/matrices-averaged/miv.sun.sym.xml");
//        KeyMatrix m = reader.getMatrix();
//
//        System.out.println(String.format("Trip sum: %s.", MatrixOperations.sum(m)));
//    }

//    private static final String COL_SEPARATOR = ";";
//
//    public static void main(String args[]) throws IOException {
//        BufferedReader reader = new BufferedReader(new FileReader(args[0]));
//        BufferedWriter writer = new BufferedWriter(new FileWriter(args[1]));
//        ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON(args[2], "NO");
//
//        String line = reader.readLine();
//        writer.write(line);
//        writer.newLine();
//
//
//        double deCount = 0;
//        double euCount = 0;
//
//        logger.info("Loading file...");
//        while ((line = reader.readLine()) != null) {
//            String tokens[] = line.split(COL_SEPARATOR);
//
//            String from = tokens[0];
//            String to = tokens[1];
//            String purpose = tokens[2];
//            String year = tokens[3];
//            String mode = tokens[4];
//            String direction = tokens[5];
//            String day = tokens[6];
//            String season = tokens[7];
//            Double volume = new Double(tokens[8]);
//
//            Zone fromZone = zones.get(from);
//            Zone toZone = zones.get(to);
//
//            double factor;
//            if(season.equals("S")) {
//                if(day.equals("2")) factor = 90;
//                else if(day.equals("7")) factor = 31;
//                else factor = 30;
//            } else {
//                if(day.equals("2")) factor = 67;
//                else if(day.equals("7")) factor = 21;
//                else factor = 22;
//            }
//
//            volume = volume * factor;
//
//            if("DE".equalsIgnoreCase(fromZone.getAttribute("NUTS0_CODE")) && "DE".equalsIgnoreCase(toZone.getAttribute
//                    ("NUTS0_CODE"))) {
//                deCount += volume;
//            } else {
//                euCount += volume;
//            }
//        }
//
//        logger.info("DE volume: " + deCount);
//        logger.info("EU volume: " + euCount);
//    }

    public static void main(String args[]) throws IOException {
        KeyMatrix m = new KeyMatrix();
        VisumOMatrixReader.read(m, "/home/johannes/gsv/miv-matrix/deploy/12102015/wkday/miv.wkday.edu.txt");

        System.out.println(MatrixOperations.sum(m));
    }
}
