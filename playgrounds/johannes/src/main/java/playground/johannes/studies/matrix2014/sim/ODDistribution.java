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

import com.vividsolutions.jts.geom.Coordinate;
import gnu.trove.impl.Constants;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.gis.*;
import playground.johannes.synpop.sim.AttributeChangeListener;
import playground.johannes.synpop.sim.Hamiltonian;
import playground.johannes.synpop.sim.data.CachedElement;
import playground.johannes.synpop.sim.data.CachedPerson;
import playground.johannes.synpop.sim.data.CachedSegment;
import playground.johannes.synpop.sim.data.Converters;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author johannes
 */
public class ODDistribution implements AttributeChangeListener, Hamiltonian {

    private static final Logger logger = Logger.getLogger(ODDistribution.class);

    private static final Object zoneIndexKey = new Object();

    private TIntObjectHashMap<TIntDoubleHashMap> refMatrix;

    private TIntObjectHashMap<TIntDoubleHashMap> simMatrix;

    private Object facilityDataKey;

    private TObjectIntHashMap<String> facId2ZoneIdx;

    private TIntObjectHashMap<Zone> idx2Zone;

    private TObjectIntHashMap<Zone> zone2Idx;

    private TObjectIntHashMap<String> id2Idx;

    private Map<ActivityFacility, Zone> fac2zone = new HashMap<>();

    private ZoneCollection zones;

    private String primaryKey;

    private final double threshold;

    private double hamiltonianValue;

    private double scaleFactor;

    private final long rescaleInterval = 1000000;

    private long changeCounter;

    private final double refSum;

    public ODDistribution(Collection<? extends Person> simPersons, KeyMatrix refKeyMatrix, DataPool dataPool, String
            layerName, String primaryKey, double threshold) {
        this.primaryKey = primaryKey;
        this.threshold = threshold;
        facId2ZoneIdx = new TObjectIntHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1);
        idx2Zone = new TIntObjectHashMap<>();
        zone2Idx = new TObjectIntHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1);
        id2Idx = new TObjectIntHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1);

        simMatrix = initMatrix(simPersons, dataPool, layerName);
        refMatrix = initRefMatrix(refKeyMatrix);

        ZoneData zoneData = (ZoneData)dataPool.get(ZoneDataLoader.KEY);
        zones = zoneData.getLayer(layerName);
        double simSum = calculateSum(simMatrix, threshold, zones);
        refSum = calculateSum(refMatrix, threshold, zones);
        scaleFactor = simSum/refSum;

        int[] indices = idx2Zone.keys();
        for(int i : indices) {
            for(int j : indices) {
                double simVal = getCellValue(i, j, simMatrix);
                double refVal = getCellValue(i, j, refMatrix);
                hamiltonianValue += calculateError(simVal, refVal);
            }
        }
    }

    private TIntObjectHashMap<TIntDoubleHashMap> initRefMatrix(KeyMatrix keyMatrix) {
        TIntObjectHashMap<TIntDoubleHashMap> matrix = new TIntObjectHashMap<>();
        Set<String> keys = keyMatrix.keys();
        for(String i : keys) {
            int idx_i = id2Idx.get(i);
            for(String j : keys) {
                Double val = keyMatrix.get(i, j);
                if(val != null) {
                    int idx_j = id2Idx.get(j);
                    adjustCellValue(idx_i, idx_j, val, matrix);
                }
            }
        }

        return matrix;
    }

    private TIntObjectHashMap<TIntDoubleHashMap> initMatrix(Collection<? extends Person> persons, DataPool dataPool,
                                                            String layerName) {
        ZoneCollection zones = ((ZoneData)dataPool.get(ZoneDataLoader.KEY)).getLayer(layerName);
        ActivityFacilities facilities = ((FacilityData)dataPool.get(FacilityDataLoader.KEY)).getAll();


        AtomicInteger maxIdx = new AtomicInteger(0);

        TIntObjectHashMap<TIntDoubleHashMap> matrix = new TIntObjectHashMap<>();

        for(Person person : persons) {
            for (Episode episode : person.getEpisodes()) {
                for (int i = 1; i < episode.getActivities().size(); i++) {
                    Segment origin = episode.getActivities().get(i - 1);
                    Segment destination = episode.getActivities().get(i);

                    int idx_i = getZoneIndex(origin, zones, facilities, maxIdx, fac2zone);
                    int idx_j = getZoneIndex(destination, zones, facilities, maxIdx, fac2zone);

                    adjustCellValue(idx_i, idx_j, 1.0, matrix);
                }
            }
        }

        return matrix;
    }

    private double calculateSum(TIntObjectHashMap<TIntDoubleHashMap> matrix, double threshold, ZoneCollection zones) {
        double sum = 0;

        DistanceCalculator dCalc = CartesianDistanceCalculator.getInstance();

        TIntObjectIterator<TIntDoubleHashMap> rowIt = matrix.iterator();
        for(int i = 0; i < matrix.size(); i++) {
            rowIt.advance();
            TIntDoubleHashMap row = rowIt.value();
            int iIdx = rowIt.key();
            Zone zone_i = idx2Zone.get(iIdx);

            TIntDoubleIterator colIt = row.iterator();
            for(int j = 0; j < row.size(); j++) {
                colIt.advance();
                int jIdx = colIt.key();
                Zone zone_j = idx2Zone.get(jIdx);

                double d = dCalc.distance(zone_i.getGeometry().getCentroid(), zone_j.getGeometry().getCentroid());
                if(d >= threshold) {
                    sum += colIt.value();
                }
            }
        }

        return sum;
    }

    private int getZoneIndex(Segment act, ZoneCollection zones, ActivityFacilities facilities, AtomicInteger maxIdx,
                             Map<ActivityFacility, Zone> fac2zone) {
        String id = act.getAttribute(CommonKeys.ACTIVITY_FACILITY);
        if(facId2ZoneIdx.containsKey(id)) {
            return facId2ZoneIdx.get(id);
        } else {
            ActivityFacility facility = facilities.getFacilities().get(Id.create(id, ActivityFacility.class));
            Zone zone = fac2zone.get(facility);
            if(zone == null) {
                zone = zones.get(new Coordinate(facility.getCoord().getX(), facility.getCoord().getY()));
                fac2zone.put(facility, zone);
            }

            int idx = zone2Idx.get(zone);
            if(!zone2Idx.containsKey(zone)) {
                idx = maxIdx.incrementAndGet();
                zone2Idx.put(zone, idx);
                idx2Zone.put(idx, zone);
                id2Idx.put(zone.getAttribute(primaryKey), idx);
            }
            facId2ZoneIdx.put(facility.getId().toString(), idx);
            return idx;
        }
    }

    private int getIndex(ActivityFacility facility) {
        int idx = facId2ZoneIdx.get(facility.getId().toString());
        if(idx == -1) {
            Zone zone = zones.get(new Coordinate(facility.getCoord().getX(), facility.getCoord().getY()));
            idx = zone2Idx.get(zone);
            facId2ZoneIdx.put(facility.getId().toString(), idx);
        }

        return idx;
    }

    @Override
    public void onChange(Object dataKey, Object oldValue, Object newValue, CachedElement element) {
        if(this.facilityDataKey == null) this.facilityDataKey = Converters.getObjectKey(CommonKeys.ACTIVITY_FACILITY);

        if(this.facilityDataKey.equals(dataKey)) {
            changeCounter++;
            if(changeCounter % rescaleInterval == 0) {
                logger.info("Rescaling simulation matrix...");
                double simSum = calculateSum(simMatrix, threshold, zones);
//                double refSum = calculateSum(refMatrix, threshold, zones);
                scaleFactor = simSum/refSum;
                logger.info(String.format("Done. New scale factor %s.", scaleFactor));
            }

            CachedSegment act = (CachedSegment)element;
            int oldIdx = getIndex((ActivityFacility)oldValue);
            int newIdx = getIndex((ActivityFacility)newValue);
            /*
            if there is a preceding trip...
             */
            CachedSegment toLeg = (CachedSegment) act.previous();
            if(toLeg != null) {
                int i = getZoneIndex((CachedSegment) toLeg.previous());
                int j = oldIdx;
                double diff1 = changeCellContent(i, j, -1.0);

                j = newIdx;
                double diff2 = changeCellContent(i, j, 1.0);

                hamiltonianValue += diff1 + diff2;
            }
            /*
            if there is a succeeding trip...
             */
            CachedSegment fromLeg = (CachedSegment)act.next();
            if(fromLeg != null) {
                int i = oldIdx;
                int j = getZoneIndex((CachedSegment) fromLeg.next());
                double diff1 = changeCellContent(i, j, -1.0);

                i = newIdx;
                double diff2 = changeCellContent(i, j, 1.0);

                hamiltonianValue += diff1 + diff2;
            }
        }
    }

    @Override
    public double evaluate(Collection<CachedPerson> population) {
        return hamiltonianValue;
    }

    private int getZoneIndex(CachedSegment act) {
//        Object idx = act.getData(zoneIndexKey);
//        if(idx == null) {
            ActivityFacility facility = (ActivityFacility) act.getData(facilityDataKey);
            return facId2ZoneIdx.get(facility.getId().toString());
//            act.setData(idx, facilityDataKey);

//        }
//        return (Integer)idx;
    }

    private double changeCellContent(int i, int j, double amount) {
        double simVal = getCellValue(i, j, simMatrix);
        double refVal = getCellValue(i, j, refMatrix);
        double oldDiff = calculateError(simVal, refVal);

        adjustCellValue(i, j, amount, simMatrix);

        simVal = getCellValue(i, j, simMatrix);
        refVal = getCellValue(i, j, refMatrix);
        double newDiff = calculateError(simVal, refVal);

        return newDiff - oldDiff;
    }

    private void adjustCellValue(int i, int j, double amount, TIntObjectHashMap<TIntDoubleHashMap> matrix) {
        TIntDoubleHashMap row = matrix.get(i);
        if(row == null) {
            row = new TIntDoubleHashMap();
            matrix.put(i, row);
        }
        row.adjustOrPutValue(j, amount, amount);
    }

    private double getCellValue(int i, int j, TIntObjectHashMap<TIntDoubleHashMap> matrix) {
        TIntDoubleHashMap row = matrix.get(i);
        if(row == null) return 0.0;
        else return row.get(j);
    }

    private double calculateError(double simVal, double refVal) {
        simVal = simVal/scaleFactor;
        if (refVal > 0) {
            return Math.abs(simVal - refVal) / refVal;
        } else {
            if (simVal == 0) return 0;
            else return simVal;
            // Not sure if scaleFactor is the appropriate normalization...
        }
    }
}
