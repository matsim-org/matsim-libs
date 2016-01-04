/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,       *
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
package playground.johannes.studies.matrix2014.config;

import org.matsim.core.config.ConfigGroup;
import playground.johannes.studies.matrix2014.sim.ODCalibrator;
import playground.johannes.synpop.gis.*;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixTxtIO;

import java.io.IOException;

/**
 * @author jillenberger
 */
public class ODCalibratorConfigurator {

    private static final String FILE = "file";

    private static final String LAYER = "zoneLayer";

    private static final String DIST_THRESHOLD = "distanceThreshold";

    private static final String VOL_THRESHOLD = "volumeThreshold";

    private final DataPool dataPool;

    public ODCalibratorConfigurator(DataPool dataPool) {
        this.dataPool = dataPool;
    }

    public ODCalibrator configure(ConfigGroup config) {
        String file = config.getValue(FILE);
        String layer = config.getValue(LAYER);
        double distanceThreshold = Double.parseDouble(config.getValue(DIST_THRESHOLD));
        double volumeThreshold = Double.parseDouble(config.getValue(VOL_THRESHOLD));

        NumericMatrix refMatrix = new NumericMatrix();
        try {
            NumericMatrixTxtIO.read(refMatrix, file);
        } catch (IOException e) {
            e.printStackTrace();
        }


        FacilityData facilityData = (FacilityData) dataPool.get(FacilityDataLoader.KEY);
        ZoneData zoneData = (ZoneData) dataPool.get(ZoneDataLoader.KEY);
        ZoneCollection zones = zoneData.getLayer(layer);

        ODCalibrator calibrator = new ODCalibrator.Builder(refMatrix, zones, facilityData.getAll()).build();
        calibrator.setDistanceThreshold(distanceThreshold);
        calibrator.setVolumeThreshold(volumeThreshold);

        return calibrator;
    }
}
