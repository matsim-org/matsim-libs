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

package playground.johannes.gsv.popsim;

import com.vividsolutions.jts.geom.Coordinate;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Episode;
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

    private static final Object zoneIndexKey = new Object();

    private TIntObjectHashMap<TIntDoubleHashMap> refMatrix;

    private TIntObjectHashMap<TIntDoubleHashMap> simMatrix;

    private Object facilityDataKey;

    private TObjectIntHashMap<String> zoneIndices;

    private double hamiltonianValue;

    private double scaleFactor;

    private TIntObjectHashMap<TIntDoubleHashMap> initMatrix(Set<? extends Episode> episodes, DataPool dataPool,
                                                            String layerName) {
        ZoneCollection zones = ((ZoneData)dataPool.get(ZoneDataLoader.KEY)).getLayer(layerName);
        ActivityFacilities facilities = ((FacilityData)dataPool.get(FacilityDataLoader.KEY)).getAll();
        Map<ActivityFacility, Zone> fac2zone = new HashMap<>();
        TObjectIntHashMap<Zone> zone2Idx = new TObjectIntHashMap<>();
        AtomicInteger maxIdx = new AtomicInteger(0);

        TIntObjectHashMap<TIntDoubleHashMap> matrix = new TIntObjectHashMap<>();

        for(Episode episode : episodes) {
            for(int i = 1; i < episode.getActivities().size(); i++) {
                Segment origin = episode.getActivities().get(i - 1);
                Segment destination = episode.getActivities().get(i);

                int idx_i = getZoneIndex(origin, zones, facilities, maxIdx, fac2zone, zone2Idx);
                int idx_j = getZoneIndex(destination, zones, facilities, maxIdx, fac2zone, zone2Idx);

                adjustCellValue(idx_i, idx_j, 1.0, matrix);
            }
        }

        return matrix;
    }

    private int getZoneIndex(Segment act, ZoneCollection zones, ActivityFacilities facilities, AtomicInteger maxIdx,
                             Map<ActivityFacility, Zone> fac2zone, TObjectIntHashMap<Zone> zone2Idx) {
        String id = act.getAttribute(CommonKeys.ACTIVITY_FACILITY);
        if(zoneIndices.containsKey(id)) {
            return zoneIndices.get(id);
        } else {
            ActivityFacility facility = facilities.getFacilities().get(Id.create(id, ActivityFacility.class));
            Zone zone = fac2zone.get(facility);
            if(zone == null) {
                zone = zones.get(new Coordinate(facility.getCoord().getX(), facility.getCoord().getY()));
                fac2zone.put(facility, zone);
            }

            int idx = zone2Idx.get(zone);
            if(zone2Idx.containsKey(zone)) {
                idx = maxIdx.incrementAndGet();
                zone2Idx.put(zone, idx);
            }
            zoneIndices.put(facility.getId().toString(), idx);
            return idx;
        }
    }

    @Override
    public void onChange(Object dataKey, Object oldValue, Object newValue, CachedElement element) {
        if(this.facilityDataKey == null) this.facilityDataKey = Converters.getObjectKey(CommonKeys.ACTIVITY_FACILITY);

        if(this.facilityDataKey.equals(dataKey)) {
            CachedSegment act = (CachedSegment)element;
            int oldIdx = zoneIndices.get(oldValue);
            int newIdx = zoneIndices.get(newValue);
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
        Object idx = act.getData(zoneIndexKey);
        if(idx == null) {
            ActivityFacility facility = (ActivityFacility) act.getData(facilityDataKey);
            idx = zoneIndices.get(facility.getId().toString());
            act.setData(idx, facilityDataKey);

        }
        return (Integer)idx;
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
        if (refVal > 0) {
            return Math.abs(simVal - refVal) / refVal;
        } else {
            if (simVal == 0) return 0;
            else return simVal/scaleFactor; //TODO: this should be invariant from the sample size of sim values.
            // Not sure if scaleFactor is the appropriate normalization...
        }
    }
}
