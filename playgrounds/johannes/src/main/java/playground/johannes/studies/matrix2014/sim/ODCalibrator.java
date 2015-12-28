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

package playground.johannes.studies.matrix2014.sim;

import com.vividsolutions.jts.geom.Point;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.synpop.analysis.Predicate;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.sim.AttributeChangeListener;
import playground.johannes.synpop.sim.Hamiltonian;
import playground.johannes.synpop.sim.data.*;

import java.util.Collection;

/**
 * @author johannes
 */
public class ODCalibrator implements Hamiltonian, AttributeChangeListener {

    private final static Logger logger = Logger.getLogger(ODCalibrator.class);

    private Object facilityDataKey;

    private final TObjectIntHashMap<ActivityFacility> facility2Index;

    private final TIntObjectHashMap<Point> index2Point;

    private TIntObjectHashMap<TIntDoubleHashMap> simMatrix;

    private final TIntObjectHashMap<TIntDoubleHashMap> refMatrix;

    private double hamiltonianValue;

    private double scaleFactor;

    private long changeCounter;

    private final long rescaleInterval = (long) 1e7;

    private final double distanceThreshold;

    private final double volumeThreshold;

    private final Predicate<CachedSegment> predicate;

    private final double refSum;

    private final boolean useWeights;

    private Object weightDataKey;

    public ODCalibrator(TIntObjectHashMap<TIntDoubleHashMap> refMatrix, TObjectIntHashMap<ActivityFacility>
            facility2Index, TIntObjectHashMap<Point> index2Point, double threshold, double volumeThreshold, Predicate<CachedSegment> predicate, boolean useWeights) {
        this.refMatrix = refMatrix;
        this.facility2Index = facility2Index;
        this.index2Point = index2Point;
        this.distanceThreshold = threshold;
        this.volumeThreshold = volumeThreshold;
        this.predicate = predicate;
        this.useWeights = useWeights;

        refSum = calculateSum(refMatrix, threshold);
    }

    private void calculateScaleFactor() {
        double simSum = calculateSum(simMatrix, distanceThreshold);
        scaleFactor = simSum / refSum;

        logger.debug(String.format("Recalculated scale factor: %s.", scaleFactor));
    }

    private void initHamiltonian() {
        hamiltonianValue = 0;
        long cnt = 0;
        int[] indices = index2Point.keys();
        for (int i : indices) {
            Point p_i = index2Point.get(i);
            for (int j : indices) {
                Point p_j = index2Point.get(j);
                if (CartesianDistanceCalculator.getInstance().distance(p_i, p_j) >= distanceThreshold) {
                    double refVal = getCellValue(i, j, refMatrix);
                    if(refVal >= volumeThreshold) {
                        double simVal = getCellValue(i, j, simMatrix);
                        hamiltonianValue += calculateError(simVal, refVal);
                        cnt++;
                    }
                }
            }
        }

        logger.debug(String.format("Calibrating against %s OD pairs.", cnt));
    }

    private void initSimMatrix(Collection<? extends CachedPerson> persons) {
        logger.debug("Initializing simulation matrix...");

        if (this.facilityDataKey == null)
            this.facilityDataKey = Converters.getObjectKey(CommonKeys.ACTIVITY_FACILITY);

        simMatrix = new TIntObjectHashMap<>();

        for (Person person : persons) {
            double weight = 1.0;
            if(useWeights) weight = Double.parseDouble(person.getAttribute(CommonKeys.PERSON_WEIGHT));

            for (Episode episode : person.getEpisodes()) {
                for (int i = 1; i < episode.getActivities().size(); i++) {

                    CachedSegment leg = (CachedSegment) episode.getLegs().get(i - 1);
                    if (predicate == null || predicate.test(leg)) {

                        CachedSegment origin = (CachedSegment) episode.getActivities().get(i - 1);
                        CachedSegment destination = (CachedSegment) episode.getActivities().get(i);

                        ActivityFacility originFac = (ActivityFacility) origin.getData(facilityDataKey);
                        ActivityFacility destinationFac = (ActivityFacility) destination.getData(facilityDataKey);

                        int idx_i = facility2Index.get(originFac);
                        int idx_j = facility2Index.get(destinationFac);

                        adjustCellValue(idx_i, idx_j, weight, simMatrix);
                    }
                }
            }
        }

        logger.debug("Done.");
    }

    @Override
    public void onChange(Object dataKey, Object oldValue, Object newValue, CachedElement element) {
        if (simMatrix != null) {
            if (this.facilityDataKey.equals(dataKey)) {
                if (this.facilityDataKey == null)
                    this.facilityDataKey = Converters.getObjectKey(CommonKeys.ACTIVITY_FACILITY);

                if(weightDataKey == null)
                    weightDataKey = Converters.register(CommonKeys.PERSON_WEIGHT, DoubleConverter.getInstance());

                changeCounter++;
                if (changeCounter % rescaleInterval == 0) {
                    calculateScaleFactor();
                }

                CachedSegment act = (CachedSegment) element;
                int oldIdx = facility2Index.get(oldValue);
                int newIdx = facility2Index.get(newValue);
            /*
            if there is a preceding trip...
             */
                CachedSegment toLeg = (CachedSegment) act.previous();
                if (toLeg != null && predicate.test(toLeg)) {
                    CachedSegment prevAct = (CachedSegment) toLeg.previous();
                    ActivityFacility prevFac = (ActivityFacility) prevAct.getData(facilityDataKey);

                    int i = facility2Index.get(prevFac);
                    int j = oldIdx;

                    //Point p_i = index2Point.get(i);
                    //Point p_j = index2Point.get(j);

                    double w = 1.0;
                    if(useWeights) w = (Double)toLeg.getData(weightDataKey);

                    double diff1 = changeCellContent(i, j, -w);
                    //if (CartesianDistanceCalculator.getInstance().distance(p_i, p_j) < distanceThreshold) {
                    //    diff1 = 0;
                   // }

                    j = newIdx;
                    //p_j = index2Point.get(j);

                    double diff2 = changeCellContent(i, j, w);
                    //if (CartesianDistanceCalculator.getInstance().distance(p_i, p_j) < distanceThreshold) {
                    //    diff2 = 0;
                    //}

                    hamiltonianValue += diff1 + diff2;
                }
            /*
            if there is a succeeding trip...
             */
                CachedSegment fromLeg = (CachedSegment) act.next();
                if (fromLeg != null && predicate.test(fromLeg)) {
                    CachedSegment nextAct = (CachedSegment) fromLeg.next();
                    ActivityFacility nextFac = (ActivityFacility) nextAct.getData(facilityDataKey);

                    int i = oldIdx;
                    int j = facility2Index.get(nextFac);

                    //Point p_i = index2Point.get(i);
                    //Point p_j = index2Point.get(j);

                    double w = 1.0;
                    if(useWeights) w = (Double)fromLeg.getData(weightDataKey);

                    double diff1 = changeCellContent(i, j, -w);
                    //if (CartesianDistanceCalculator.getInstance().distance(p_i, p_j) < distanceThreshold) {
                    //    diff1 = 0;
                    //}

                    i = newIdx;
                    //p_i = index2Point.get(i);

                    double diff2 = changeCellContent(i, j, w);
                    //if (CartesianDistanceCalculator.getInstance().distance(p_i, p_j) < distanceThreshold) {
                    //    diff2 = 0;
                    //}

                    hamiltonianValue += diff1 + diff2;
                }
            }
        }
    }

    @Override
    public double evaluate(Collection<CachedPerson> population) {
        if (simMatrix == null) {
            initSimMatrix(population);
            calculateScaleFactor();
            initHamiltonian();
        }
        return hamiltonianValue;
    }

    private double changeCellContent(int i, int j, double amount) {
        Point p_i = index2Point.get(i);
        Point p_j = index2Point.get(j);

        double refVal = getCellValue(i, j, refMatrix);

        if(refVal >= volumeThreshold && CartesianDistanceCalculator.getInstance().distance(p_i, p_j) >= distanceThreshold) {
            double simVal = getCellValue(i, j, simMatrix);
            double oldDiff = calculateError(simVal, refVal);

            adjustCellValue(i, j, amount, simMatrix);

            simVal = getCellValue(i, j, simMatrix);
            double newDiff = calculateError(simVal, refVal);

            return newDiff - oldDiff;
        } else {
            adjustCellValue(i, j, amount, simMatrix);
            return 0.0;
        }
    }

    private void adjustCellValue(int i, int j, double amount, TIntObjectHashMap<TIntDoubleHashMap> matrix) {
        TIntDoubleHashMap row = matrix.get(i);
        if (row == null) {
            row = new TIntDoubleHashMap();
            matrix.put(i, row);
        }
        row.adjustOrPutValue(j, amount, amount);
    }

    private double getCellValue(int i, int j, TIntObjectHashMap<TIntDoubleHashMap> matrix) {
        TIntDoubleHashMap row = matrix.get(i);
        if (row == null) return 0.0;
        else return row.get(j);
    }

    private double calculateError(double simVal, double refVal) {
        simVal = simVal / scaleFactor;
        if (refVal > 0) {
            return Math.abs(simVal - refVal) / refVal;
        } else {
            if (simVal == 0) return 0;
            else return simVal;
            // Not sure if scaleFactor is the appropriate normalization...
        }
    }

    private double calculateSum(TIntObjectHashMap<TIntDoubleHashMap> matrix, double threshold) {
        double sum = 0;

        DistanceCalculator dCalc = CartesianDistanceCalculator.getInstance();

        TIntObjectIterator<TIntDoubleHashMap> rowIt = matrix.iterator();
        for (int i = 0; i < matrix.size(); i++) {
            rowIt.advance();
            TIntDoubleHashMap row = rowIt.value();
            int idx_i = rowIt.key();
            Point p_i = index2Point.get(idx_i);

            TIntDoubleIterator colIt = row.iterator();
            for (int j = 0; j < row.size(); j++) {
                colIt.advance();
                int idx_j = colIt.key();
                Point p_j = index2Point.get(idx_j);

                double d = dCalc.distance(p_i, p_j);
                if (d >= threshold) {
                    sum += colIt.value();
                }
            }
        }

        return sum;
    }
}
