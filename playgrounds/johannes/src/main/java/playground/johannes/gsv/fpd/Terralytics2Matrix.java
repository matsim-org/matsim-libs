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

package playground.johannes.gsv.fpd;

import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixTxtIO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author johannes
 */
public class Terralytics2Matrix {

    public static void main(String[] args) throws IOException {
        String inFile = "/Users/johannes/gsv/fpd/telefonica/032016/data/raw/od_weekly_zip5.csv";
        String outFile = "/Users/johannes/gsv/fpd/telefonica/032016/data/plz5.rail.6week.txt";

        BufferedReader reader = new BufferedReader(new FileReader(inFile));
        String line = reader.readLine();

        NumericMatrix m = new NumericMatrix();

        while((line = reader.readLine()) != null) {
            String[] tokens = line.split(",");

            String i = tokens[0];
            String j = tokens[1];

            if(i.length() == 4) {
                i = "0" + i;
            }

            if(j.length() == 4) {
                j = "0" + j;
            }

            String mode = tokens[2];

            if(mode.equalsIgnoreCase("Bahn")) {
                String week = tokens[3];
                if(!week.equalsIgnoreCase("W1-2") && !week.equalsIgnoreCase("W2-2")) {
                    double volume = Double.parseDouble(tokens[4]);
                    m.add(i, j, volume);
                }
            }
        }

        NumericMatrixTxtIO.write(m, outFile);
    }
}
