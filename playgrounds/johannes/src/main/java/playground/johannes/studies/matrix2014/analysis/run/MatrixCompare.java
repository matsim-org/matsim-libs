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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import playground.johannes.studies.matrix2014.analysis.MatrixDistanceCompare;
import playground.johannes.studies.matrix2014.analysis.MatrixIntraVolumeShareCompare;
import playground.johannes.studies.matrix2014.analysis.MatrixMarginalsCompare;
import playground.johannes.studies.matrix2014.analysis.MatrixVolumeCompare;
import playground.johannes.studies.matrix2014.matrix.ODPredicate;
import playground.johannes.studies.matrix2014.matrix.VolumePredicate;
import playground.johannes.synpop.analysis.*;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixIO;

import java.io.IOException;

/**
 * @author johannes
 */
public class MatrixCompare {

    private static final Logger logger = Logger.getLogger(MatrixCompare.class);

    public static void main(String args[]) throws IOException {
//        String simFile = args[0];
//        String refFile = args[1];
//        String zoneFile = args[2];
//        String outDir = args[3];

        String simFile = "/home/johannes/sge/prj/matrix2014/runs/1141/output/1E9/matrix/matrix.txt.gz";
        String refFile = "/home/johannes/gsv/matrix2014/sim/data/matrices/itp.de.txt";
        String outDir = "/home/johannes/gsv/matrix2014/matrix-compare/";
        String zoneFile = "/home/johannes/gsv/gis/zones/geojson/nuts3.psm.airports.gk3.geojson";
        double volumeThreshold = 0;

//        NumericMatrix simMatrix = GSVMatrixIO.read(simFile);//NumericMatrixIO.read(simFile);
//        NumericMatrix refMatrix = GSVMatrixIO.read(refFile);//NumericMatrixIO.read(refFile);
        NumericMatrix simMatrix = NumericMatrixIO.read(simFile);
        NumericMatrix refMatrix = NumericMatrixIO.read(refFile);

        FileIOContext ioContext = new FileIOContext(outDir);

        ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON(zoneFile, "NO");
        ODPredicate<String, Double> odPredicate = null;//new ZoneDistancePredicate(zones, 100000,
//                CartesianDistanceCalculator.getInstance());

        NumericMatrix tmpSimMatrix = simMatrix;
        if (odPredicate != null) {
            tmpSimMatrix = (NumericMatrix) MatrixOperations.subMatrix(odPredicate, simMatrix, new NumericMatrix());
        }

        double simTotal = MatrixOperations.sum(tmpSimMatrix);

        NumericMatrix tmpRefMatrix = refMatrix;
        if (odPredicate != null) {
            tmpRefMatrix = (NumericMatrix) MatrixOperations.subMatrix(odPredicate, refMatrix, new NumericMatrix());
        }

        double refTotal = MatrixOperations.sum(tmpRefMatrix);

        if(volumeThreshold > 0) {
            ODPredicate volPredicate = new VolumePredicate(volumeThreshold);
            refMatrix = (NumericMatrix) MatrixOperations.subMatrix(volPredicate, refMatrix, new NumericMatrix());
        }

        logger.debug(String.format("Normalization factor: %s.", simTotal/refTotal));
        MatrixOperations.applyFactor(refMatrix, simTotal / refTotal);

        AnalyzerTaskComposite<Pair<NumericMatrix, NumericMatrix>> composite = new AnalyzerTaskComposite<>();

        HistogramWriter writer = new HistogramWriter(ioContext, new PassThroughDiscretizerBuilder(new
                LinearDiscretizer(0.05), "linear"));

        MatrixVolumeCompare volTask = new MatrixVolumeCompare("matrix.vol");
        volTask.setIoContext(ioContext);
        volTask.setHistogramWriter(writer);

        MatrixDistanceCompare distTask = new MatrixDistanceCompare("matrix.dist", zones);
//        distTask.setDistanceCalculator(WGS84DistanceCalculator.getInstance());
        distTask.setDistanceCalculator(CartesianDistanceCalculator.getInstance());
        distTask.setFileIoContext(ioContext);
        distTask.setDiscretizer(new LinearDiscretizer(25000));

        MatrixMarginalsCompare marTask = new MatrixMarginalsCompare("matrix");
        marTask.setHistogramWriter(writer);

        MatrixIntraVolumeShareCompare intraTask = new MatrixIntraVolumeShareCompare(ioContext);

        composite.addComponent(volTask);
        composite.addComponent(distTask);
        composite.addComponent(marTask);
        composite.addComponent(intraTask);

        AnalyzerTaskRunner.run(new ImmutablePair<>(refMatrix, simMatrix), composite, ioContext);
    }
}
