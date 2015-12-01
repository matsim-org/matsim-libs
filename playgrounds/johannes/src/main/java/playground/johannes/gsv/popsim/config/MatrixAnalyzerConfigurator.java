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

package playground.johannes.gsv.popsim.config;

import org.matsim.core.config.ConfigGroup;
import playground.johannes.gsv.popsim.analysis.MatrixAnalyzer;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.io.KeyMatrixTxtIO;
import playground.johannes.gsv.zones.io.KeyMatrixXMLReader;
import playground.johannes.synpop.gis.*;

import java.io.IOException;
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

    public MatrixAnalyzerConfigurator(ConfigGroup config, DataPool dataPool) {
        this.config = config;
        this.dataPool = dataPool;
    }

    @Override
    public Object load() {
        String zoneLayerName = config.getValue(ZONE_LAYER_NAME);

        FacilityData facilityData = (FacilityData) dataPool.get(FacilityDataLoader.KEY);
        ZoneData zoneData = (ZoneData)dataPool.get(ZoneDataLoader.KEY);
        ZoneCollection zones = zoneData.getLayer(zoneLayerName);

        Map<String, KeyMatrix> referenceMatrices = new HashMap<>();
        Collection<? extends ConfigGroup> modules = config.getParameterSets(PARAM_SET_KEY);
        for(ConfigGroup paramset : modules) {
            String name = paramset.getValue(MATRIX_NAME);
            String path = paramset.getValue(MATRIX_FILE);

            KeyMatrix m;
            if(path.endsWith(".xml") || path.endsWith(".xml.gz")) {
                KeyMatrixXMLReader reader = new KeyMatrixXMLReader();
                reader.setValidating(false);
                reader.parse(path);
                m = reader.getMatrix();
            } else {
                m = new KeyMatrix();
                try {
                    KeyMatrixTxtIO.read(m, path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            referenceMatrices.put(name, m);
        }

        return new MatrixAnalyzer(facilityData, zones, referenceMatrices);
    }
}
