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

package playground.johannes.gsv.popsim.analysis;

import org.matsim.contrib.common.stats.LinearDiscretizer;
import playground.johannes.gsv.popsim.CollectionUtils;
import playground.johannes.gsv.popsim.MatrixBuilder;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.MatrixOperations;
import playground.johannes.gsv.zones.io.KeyMatrixTxtIO;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.gis.FacilityData;
import playground.johannes.synpop.gis.ZoneCollection;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author johannes
 */
public class MatrixAnalyzer extends AbstractAnalyzerTask<Collection<? extends Person>> {

    private static final String KEY = "matrix";

    private Map<String, KeyMatrix> refMatrices;

    private MatrixBuilder matrixBuilder;

    private Predicate<Segment> predicate;

    public MatrixAnalyzer(FacilityData facilityData, ZoneCollection zones, Map<String, KeyMatrix> refMatrices) {
        matrixBuilder = new MatrixBuilder(facilityData, zones);
        this.refMatrices = refMatrices;
        addDiscretizer(new LinearDiscretizer(0.05), "linear");
    }

    public void setPredicate(Predicate<Segment> predicate) {
        this.predicate = predicate;
    }

    @Override
    public void analyze(Collection<? extends Person> persons, List<StatsContainer> containers) {
        KeyMatrix simMatrix = matrixBuilder.build(persons, predicate);

        double simTotal = MatrixOperations.sum(simMatrix);

        for(Map.Entry<String, KeyMatrix> entry : refMatrices.entrySet()) {
            KeyMatrix refMatrix = entry.getValue();

            double refTotal = MatrixOperations.sum(refMatrix);
            MatrixOperations.applyFactor(refMatrix, simTotal/refTotal);

            KeyMatrix errMatrix = MatrixOperations.errorMatrix(refMatrix, simMatrix);

            double[] errors = CollectionUtils.toNativeArray(errMatrix.values());

            String name = String.format("%s.%s.err", KEY, entry.getKey());
            StatsContainer container = new StatsContainer(name, errors);
            containers.add(container);

            writeHistograms(errors, name);

            if(ioContext != null) {
                try {
                    KeyMatrixTxtIO.write(simMatrix, String.format("%s/matrix.txt.gz", ioContext.getPath()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
