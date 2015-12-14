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
import com.vividsolutions.jts.geom.Point;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.synpop.gis.*;

import java.util.Set;

/**
 * @author johannes
 */
public class ODCalibratorBuilder {

    public ODCalibrator build(KeyMatrix refKeyMatrix, DataPool dataPool, String layerName, String primaryKey, int
            threshold) {
        FacilityData fData = (FacilityData) dataPool.get(FacilityDataLoader.KEY);
        ActivityFacilities facilities = fData.getAll();
        ZoneData zoneData = (ZoneData) dataPool.get(ZoneDataLoader.KEY);
        ZoneCollection zones = zoneData.getLayer(layerName);

        Set<Zone> zoneSet = zones.getZones();
        TObjectIntHashMap<String> id2Index = new TObjectIntHashMap<>(zoneSet.size());
        TIntObjectHashMap<Point> index2Point = new TIntObjectHashMap<>();
        int index = 0;
        for(Zone zone : zoneSet) {
            id2Index.put(zone.getAttribute(primaryKey), index);
            index2Point.put(index, zone.getGeometry().getCentroid());

            index++;
        }
        TIntObjectHashMap<TIntDoubleHashMap> refMatrix = initRefMatrix(refKeyMatrix, id2Index);

        TObjectIntHashMap<ActivityFacility> facility2Index = new TObjectIntHashMap<>();

        for(ActivityFacility fac : facilities.getFacilities().values()) {
            Zone zone = zones.get(new Coordinate(fac.getCoord().getX(), fac.getCoord().getY()));
            int idx = id2Index.get(zone.getAttribute(primaryKey));
            facility2Index.put(fac, idx);
        }

        return new ODCalibrator(refMatrix, facility2Index, index2Point, threshold);
    }

    private TIntObjectHashMap<TIntDoubleHashMap> initRefMatrix(KeyMatrix keyMatrix, TObjectIntHashMap<String> id2Idx) {
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

    private void adjustCellValue(int i, int j, double amount, TIntObjectHashMap<TIntDoubleHashMap> matrix) {
        TIntDoubleHashMap row = matrix.get(i);
        if(row == null) {
            row = new TIntDoubleHashMap();
            matrix.put(i, row);
        }
        row.adjustOrPutValue(j, amount, amount);
    }
}
