/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.poznan.demand.ptap;

import java.io.*;
import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.matrices.Matrix;

import playground.michalm.util.matrices.MatrixUtils;
import playground.michalm.util.visum.VisumMatrixReader;


public class VisumDemandDataReader
{
    public static Map<String, double[]> readHourlyShares(String sharesFile)
    {
        Map<String, double[]> hourlyShares = new LinkedHashMap<>();

        try (Scanner scanner = new Scanner(new File(sharesFile))) {
            scanner.useLocale(Locale.US);
            scanner.nextLine();

            while (scanner.hasNext()) {
                String key = scanner.next();

                double[] shares = new double[24];
                for (int i = 0; i < shares.length; i++) {
                    shares[i] = scanner.nextDouble();
                }

                hourlyShares.put(key, shares);
            }
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return hourlyShares;
    }


    public static Map<String, Tuple<String, String>> readActivityPairs(String activityFile)
    {
        Map<String, Tuple<String, String>> activityPairs = new LinkedHashMap<>();

        try (Scanner scanner = new Scanner(new File(activityFile))) {
            scanner.useLocale(Locale.US);

            while (scanner.hasNext()) {
                String key = scanner.next();
                String fromActivity = scanner.next();
                String toActivity = scanner.next();

                activityPairs.put(key, new Tuple<>(fromActivity, toActivity));
            }
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return activityPairs;
    }


    public static Map<String, Matrix> readODMatrices(String bindingsFile, String mainDir,
            Map<Id<Zone>, Zone> zones)
    {
        Map<String, Matrix> odMatrices = new LinkedHashMap<>();

        try (Scanner scanner = new Scanner(new File(bindingsFile))) {
            while (scanner.hasNext()) {
                String key = scanner.next();
                String odMatrixFile = mainDir + scanner.next();

                double[][] visumODMatrix = VisumMatrixReader.readMatrix(odMatrixFile);
                Matrix odMatrix = MatrixUtils.createSparseMatrix(key, zones.keySet(),
                        visumODMatrix);

                odMatrices.put(key, odMatrix);
            }
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return odMatrices;
    }
}
