/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.emissions;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by molloyj on 01.12.2017.
 * class to mimic the old org.matsim.contrib.emissions.roadTypeMapping that berlin uses with VISUM
 */
public class VisumHbefaRoadTypeMapping extends HbefaRoadTypeMapping {
    private static final Logger logger = Logger.getLogger(VisumHbefaRoadTypeMapping.class);

    private final Map<String, String> mapping = new HashMap<>();

    private VisumHbefaRoadTypeMapping() {
    }

    @Override
    public String determineHebfaType(Link link) {
        String roadType = NetworkUtils.getType(link);
        return mapping.get(roadType);
    }

    public void put(String visumRtNr, String hbefaRtName) {
        mapping.put(visumRtNr, hbefaRtName);
    }

    public static VisumHbefaRoadTypeMapping emptyMapping() {
        return new VisumHbefaRoadTypeMapping();
    }

    /*package-private*/ static HbefaRoadTypeMapping createVisumRoadTypeMapping(URL filename){
        logger.info("entering createRoadTypeMapping ...") ;

        VisumHbefaRoadTypeMapping mapping = new VisumHbefaRoadTypeMapping();
        try{
            BufferedReader br = IOUtils.getBufferedReader(filename);
            String strLine = br.readLine();
            Map<String, Integer> indexFromKey = createIndexFromKey(strLine);

            while ((strLine = br.readLine()) != null){
                if ( strLine.contains("\"")) throw new RuntimeException("cannot handle this character in parsing") ;

                String[] inputArray = strLine.split(";");
                String visumRtNr = inputArray[indexFromKey.get("VISUM_RT_NR")];
                String hbefaRtName = (inputArray[indexFromKey.get("HBEFA_RT_NAME")]);

                mapping.put(visumRtNr, hbefaRtName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("leaving createRoadTypeMapping ...");
        return mapping;
    }

    private static Map<String, Integer> createIndexFromKey(String strLine) {
        String[] keys = strLine.split(";");

        Map<String, Integer> indexFromKey = new HashMap<>();
        for (int ii = 0; ii < keys.length; ii++) {
            indexFromKey.put(keys[ii], ii);
        }
        return indexFromKey;
    }
}
