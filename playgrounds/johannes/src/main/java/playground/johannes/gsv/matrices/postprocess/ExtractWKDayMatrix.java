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

package playground.johannes.gsv.matrices.postprocess;

import com.sun.istack.internal.logging.Logger;
import org.matsim.core.utils.io.IOUtils;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.MatrixOperations;
import playground.johannes.gsv.zones.io.VisumOMatrixReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 */
public class ExtractWKDayMatrix {

    private static final Logger logger = Logger.getLogger(ExtractWKDayMatrix.class);

    private static final String COL_SEPARATOR = ";";

    public static void main(String args[]) throws IOException {
//        BufferedReader reader = new BufferedReader(new FileReader(args[0]));
        BufferedReader reader = IOUtils.getBufferedReader("/home/johannes/gsv/miv-matrix/deploy/12102015/miv.2013.txt");
        String output = "/home/johannes/gsv/miv-matrix/deploy/12102015/wkday";

        Map<String, KeyMatrix> matrices = new HashMap<>();

        logger.info("Loading file...");
        String line = reader.readLine();
        while ((line = reader.readLine()) != null) {
            String tokens[] = line.split(COL_SEPARATOR);

            String from = tokens[0];
            String to = tokens[1];
            String purpose = tokens[2];
            String day = tokens[6];
            Double volume = new Double(tokens[8]);

            if(!day.equalsIgnoreCase("6") && !day.equalsIgnoreCase("7")) {
                KeyMatrix m = matrices.get(purpose);
                if(m == null) {
                    m = new KeyMatrix();
                    matrices.put(purpose, m);
                }

                if(day.equalsIgnoreCase("2")) volume = volume * 3;
                m.add(from, to, volume);
            }
        }

        Map<String, String> labels = new HashMap<>();
        labels.put("A", "edu");
        labels.put("B", "work");
        labels.put("G", "buisiness");
        labels.put("E", "shop");
        labels.put("F1", "leisure");
        labels.put("F2", "vacations_short");
        labels.put("U", "vacations_long");
        labels.put("WE", "wecommuter");

        logger.info("Writing matrices...");
        for(Map.Entry<String, KeyMatrix> entry : matrices.entrySet()) {
            KeyMatrix m = entry.getValue();
            MatrixOperations.applyFactor(m, 1/10.0);

            String path = String.format("%s/miv.wkday.%s.txt", output, labels.get(entry.getKey()));
            VisumOMatrixReader.write(m, path);
        }

        logger.info("Done.");
    }
}
