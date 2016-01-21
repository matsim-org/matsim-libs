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

/**
 * @author johannes
 */
public class MatrixAnalyzerConfigurator implements DataLoader {

    public static final String ZONE_LAYER_NAME = "zoneLayer";

    //public static final String PARAM_SET_KEY = "referenceMatrix";

    public static final String MATRIX_NAME = "name";

    public static final String MATRIX_FILE = "file";

    public static final String THRESHOLD = "volumeThreshold";

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
        ZoneData zoneData = (ZoneData) dataPool.get(ZoneDataLoader.KEY);
        ZoneCollection zones = zoneData.getLayer(zoneLayerName);

        String name = config.getValue(MATRIX_NAME);
        String path = config.getValue(MATRIX_FILE);
        String strThreshold = config.getValue(THRESHOLD);
        double threshold = 0;
        if(strThreshold != null)
         threshold = Double.parseDouble(strThreshold);

        NumericMatrix m = NumericMatrixIO.read(path);

        MatrixAnalyzer analyzer = new MatrixAnalyzer(facilityData.getAll(), zones, m, name);
        analyzer.setFileIOContext(ioContext);
        analyzer.setVolumeThreshold(threshold);

        return analyzer;
    }
}
