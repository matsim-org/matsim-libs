/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.util.visum;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;


public class VisumODMatrixReader
{
    public static double[][] readMatrixFile(File file)
    {
        try (Scanner scanner = new Scanner(file)) {
            scanner.useLocale(Locale.US);

            scanner.nextLine(); // $V;D2;Y5
            scanner.nextLine(); // *
            scanner.nextLine(); // * time interval
            scanner.nextLine(); // 0.00 0.00
            scanner.nextLine(); // * factor
            scanner.nextLine(); // 1.000000
            scanner.nextLine(); // * Number of zones
            int nZones = scanner.nextInt(); // 417

            // =================

            scanner.next(); // *
            scanner.next(); // Zone
            scanner.next(); // numbers

            // Id[] zoneIds = new Id[nZones];

            for (int i = 0; i < nZones; i++) {
                // zoneIds[i] = scenario.createId(scanner.next());
                scanner.next();
            }

            // =================

            for (int i = 0; i < nZones;) {
                if (scanner.next().charAt(0) != '*') {
                    i++;
                }
            }

            // =================

            double[][] odMatrix = (double[][])Array.newInstance(double.class, nZones, nZones);

            for (int i = 0; i < nZones; i++) {
                scanner.next(); // *
                scanner.next(); // 1
                scanner.next(); // 0.00

                for (int j = 0; j < nZones; j++) {
                    odMatrix[i][j] = scanner.nextDouble();
                }
            }

            return odMatrix;
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args)
    {
        readMatrixFile(new File("d:\\eTaxi\\Poznan_MATSim\\odMatricesByType\\D_I_0-1"));
    }
}
