/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.johannes.synpop.source.mid2008.generator;

import playground.johannes.synpop.data.ActivityTypes;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Segment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 */
public class LegPurposeHandler implements LegAttributeHandler {

    private Map<String, String> mapping;

    public LegPurposeHandler(Map<String, String> mapping) {
        this.mapping = mapping;
    }

    @Override
    public void handle(Segment leg, Map<String, String> attributes) {
        /*
        First, assign the main purpose label. Fallback to "misc" if no mapping is available.
         */
        String mainTypeCode = attributes.get(VariableNames.LEG_MAIN_TYPE);
        String mainTypeLabel = mapping.get(mainTypeCode);

        if(mainTypeLabel == null) mainTypeLabel = ActivityTypes.MISC;
        leg.setAttribute(CommonKeys.LEG_PURPOSE, mainTypeLabel);
        /*
        Second, override the main purpose if a sub purpose label is available.
         */
        String subTypeCode = attributes.get(VariableNames.LEG_SUB_TYPE);
        String subTypeLabel = mapping.get(subTypeCode);

        if(subTypeLabel != null) leg.setAttribute(CommonKeys.LEG_PURPOSE, subTypeLabel);
    }


    public static Map<String, String> loadMappingFromFile(String filename) throws IOException {
        Map<String, String> mapping = new HashMap<>();

        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line = reader.readLine();

        while((line = reader.readLine()) != null) {
            String tokens[] = line.split("\\s");
            mapping.put(tokens[0], tokens[1]);
        }

        return mapping;
    }
}
