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

package playground.johannes.studies.matrix2014.analysis.run;

import org.apache.log4j.Logger;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import playground.johannes.studies.matrix2014.analysis.MatrixVolumeCompare;
import playground.johannes.studies.matrix2014.matrix.ODPredicate;
import playground.johannes.studies.matrix2014.matrix.VolumePredicate;
import playground.johannes.studies.matrix2014.matrix.ZoneDistancePredicate;
import playground.johannes.synpop.analysis.FileIOContext;
import playground.johannes.synpop.analysis.StatsContainer;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixIO;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author johannes
 */
public class MatrixCompare {

    private static final Logger logger = Logger.getLogger(MatrixCompare.class);

    public static void main(String args[]) throws IOException {
        String simFile = "/home/johannes/sge/prj/matrix2014/runs/1016/output/8E9/matrix.txt.gz";
        String refFile = "/home/johannes/gsv/matrix2014/sim/data/matrices/itp.de.txt";
        String outDir = "/home/johannes/gsv/matrix2014/matrix-compare/";
        double volumeThreshold = 1;

        NumericMatrix simMatrix = NumericMatrixIO.read(simFile);
        NumericMatrix refMatrix = NumericMatrixIO.read(refFile);

        FileIOContext ioContext = new FileIOContext(outDir);

        ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON("/home/johannes/gsv/gis/zones/geojson/tomtom.de.gk3.geojson", "NO");
        ODPredicate<String, Double> odPredicate = new ZoneDistancePredicate(zones, 100000,
                CartesianDistanceCalculator.getInstance());

        if (odPredicate != null) {
            NumericMatrix tmpMatrix = new NumericMatrix();
            MatrixOperations.subMatrix(odPredicate, simMatrix, tmpMatrix);
            simMatrix = tmpMatrix;
        }

        double simTotal = MatrixOperations.sum(simMatrix);

//        NumericMatrix tmpRefMatrix = refMatrix;
        if (odPredicate != null) {
            refMatrix = (NumericMatrix) MatrixOperations.subMatrix(odPredicate, refMatrix, new NumericMatrix());
        }

        double refTotal = MatrixOperations.sum(refMatrix);

        if(volumeThreshold > 0) {
            ODPredicate volPredicate = new VolumePredicate(volumeThreshold);
            refMatrix = (NumericMatrix) MatrixOperations.subMatrix(volPredicate, refMatrix, new NumericMatrix());
        }

        logger.debug(String.format("Normalization factor: %s.", simTotal/refTotal));
        MatrixOperations.applyFactor(refMatrix, simTotal / refTotal);

        MatrixVolumeCompare task = new MatrixVolumeCompare("matrix");
        task.setReferenceMatrix(refMatrix);
        task.setIoContext(ioContext);

        task.analyze(simMatrix, new ArrayList<StatsContainer>());
    }
}
