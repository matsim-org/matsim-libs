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

package playground.johannes.studies.matrix2014.matrix;

import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import playground.johannes.synpop.analysis.FileIOContext;
import playground.johannes.synpop.analysis.StratifiedDiscretizerBuilder;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixIO;

import java.io.IOException;

/**
 * @author johannes
 */
public class Analyzer {

    public static void main(String args[]) throws IOException {
        String matrixFile = "/Users/johannes/gsv/fpd/telefonica/032016/data/plz5.rail.6week.txt";
        String zonesFile = "/Users/johannes/gsv/gis/zones/geojson/plz5.gk3.geojson";
        String outDir = "/Users/johannes/gsv/fpd/telefonica/032016/analysis";

        NumericMatrix m = NumericMatrixIO.read(matrixFile);
        ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON(zonesFile, "plz", "plz5");
        FileIOContext ioContext = new FileIOContext(outDir);

        GeoDistanceTask task = new GeoDistanceTask(
                zones,
                ioContext,
                new StratifiedDiscretizerBuilder(100, 1),
                CartesianDistanceCalculator.getInstance());

        task.analyze(m, null);

    }
}
