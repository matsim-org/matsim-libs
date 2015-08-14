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

;
import org.apache.log4j.Logger;
import playground.johannes.gsv.matrices.episodes2matrix.Episodes2Matrix;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.mid.MIDKeys;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.MatrixOperations;
import playground.johannes.gsv.zones.io.KeyMatrixTxtIO;

import java.io.*;

/**
 * @author johannes
 */
public class CalculateDayFractions {

    private static final Logger logger = Logger.getLogger(CalculateDayFractions.class);

    public static void main(String args[]) throws IOException {
        String root = args[0];
        String out = args[1];

        BufferedWriter writer = new BufferedWriter(new FileWriter(out));
        writer.write("day\tseason\tvolume");
        writer.newLine();

        KeyMatrix m = new KeyMatrix();
        KeyMatrixTxtIO.read(m, String.format("%s/car.txt.gz", root));
        writer.write("all\tall\t");
        writer.write(String.valueOf(MatrixOperations.sum(m)));
        writer.newLine();

        File rootDir = new File(root);

        String seasons[] = new String[] {Episodes2Matrix.SUMMER, Episodes2Matrix.WINTER};
        String days[] = new String[] {CommonKeys.MONDAY, Episodes2Matrix.DIMIDO, CommonKeys.FRIDAY, CommonKeys
                .SATURDAY, CommonKeys.SUNDAY};
        int count = 0;
        for(String season : seasons) {
            for(String day : days) {
                double sum = 0;
                String pattern = String.format(".*\\.%s\\.%s\\..*", day, season);
                for(File file : rootDir.listFiles()) {

                    if(file.getName().matches(pattern)) {
                        logger.info(String.format("Loading matrix %s...", file.getName()));
                        m = new KeyMatrix();
                        KeyMatrixTxtIO.read(m, file.getAbsolutePath());
                        sum += MatrixOperations.sum(m);

                        count++;
                    }
                }

                writer.write(day);
                writer.write("\t");
                writer.write(season);
                writer.write("\t");
                writer.write(String.valueOf(sum));
                writer.newLine();
                writer.flush();
            }
        }

        writer.close();

        System.out.println("Parsed " + count + " matrices");
    }
}
