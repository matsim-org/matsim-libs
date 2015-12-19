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

package playground.johannes.studies.matrix2014.config;

import org.matsim.core.config.ConfigGroup;
import playground.johannes.studies.matrix2014.analysis.MatrixAnalyzer;
import playground.johannes.synpop.analysis.FileIOContext;
import playground.johannes.synpop.gis.*;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixIO;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 */
public class MatrixAnalyzerConfigurator implements DataLoader {

    public static final String ZONE_LAYER_NAME = "zoneLayer";

    public static final String PARAM_SET_KEY = "referenceMatrix";

    public static final String MATRIX_NAME = "name";

    public static final String MATRIX_FILE = "file";

    private final ConfigGroup config;

    private final DataPool dataPool;

    private final FileIOContext ioContext;

    public MatrixAnalyzerConfigurator(ConfigGroup config, DataPool dataPool, FileIOContext ioContext) {
        this.config = config;
        this.dataPool = dataPool;
        this.ioContext = ioContext;
    }

    @Override
    public Object load() {
        String zoneLayerName = config.getValue(ZONE_LAYER_NAME);

        FacilityData facilityData = (FacilityData) dataPool.get(FacilityDataLoader.KEY);
        ZoneData zoneData = (ZoneData)dataPool.get(ZoneDataLoader.KEY);
        ZoneCollection zones = zoneData.getLayer(zoneLayerName);

        Map<String, NumericMatrix> referenceMatrices = new HashMap<>();
        Collection<? extends ConfigGroup> modules = config.getParameterSets(PARAM_SET_KEY);
        for(ConfigGroup paramset : modules) {
            String name = paramset.getValue(MATRIX_NAME);
            String path = paramset.getValue(MATRIX_FILE);

            referenceMatrices.put(name, NumericMatrixIO.read(path));
        }

        return new MatrixAnalyzer(facilityData, zones, referenceMatrices, ioContext);
    }
}
