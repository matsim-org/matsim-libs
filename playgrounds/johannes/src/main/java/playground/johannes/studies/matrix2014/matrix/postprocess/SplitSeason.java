/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.johannes.studies.matrix2014.matrix.postprocess;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedBordersDiscretizer;
import playground.johannes.studies.matrix2014.analysis.SetSeason;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * @author johannes
 */
public class SplitSeason {

    private static final Logger logger = Logger.getLogger(SplitSeason.class);

    public static void main(String args[]) throws IOException {
        String sourceFile = args[0];//"/Users/johannes/gsv/matrix2014/sim/output/1E7/gsv-matrix.txt";
        String targetFile = args[3];//"/Users/johannes/gsv/matrix2014/sim/output/1E7/gsv-matrix-season.txt";
        String shareFile = args[1];//"/Users/johannes/gsv/matrix2014/sim/output/season-share.txt";
        String zoneFile = args[2];//"/Users/johannes/gsv/gis/zones/geojson/tomtom.de.gk3.geojson";
        /*
        Load share table.
         */
        logger.info("Loading share table...");
        List<Pair<Map<String, String>, TObjectDoubleMap<String>>> shareTable = loadShareTable(shareFile, 3);
        /*
        Create distance discretizer.
         */
        // TODO: Consolidate with SeasonTask
        TDoubleArrayList borders = new TDoubleArrayList();
        borders.add(-1);
        borders.add(50000);
        borders.add(100000);
        borders.add(1000000);
        borders.add(Double.MAX_VALUE);
        Discretizer discretizer = new FixedBordersDiscretizer(borders.toArray());
        /*
        Load zones...
         */
        logger.info("Loading zones...");
        ZoneCollection zones = ZoneGeoJsonIO.readFromGeoJSON(zoneFile, "NO", "modena");
        /*
        Create dimension calculator.
         */
//        DistanceCalculator distanceCalculator = new WGS84DistanceCalculator();
        DistanceCalculator distanceCalculator = CartesianDistanceCalculator.getInstance();
        DistanceDimensionCalculator calculator = new DistanceDimensionCalculator(zones, distanceCalculator, discretizer);
        /*
        Run matrix splitter...
         */
        MatrixSplitter splitter = new MatrixSplitter(SetSeason.SEASON_KEY, shareTable);
        splitter.addDimensionCalculator(CommonKeys.LEG_GEO_DISTANCE, calculator);

        logger.info("Processing matrix...");
        splitter.process(sourceFile, targetFile);
        logger.info("Done.");
    }

    public static List<Pair<Map<String, String>, TObjectDoubleMap<String>>> loadShareTable(String file, int offset) throws IOException {
        List<Pair<Map<String, String>, TObjectDoubleMap<String>>> shareTable = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();
        String tokens[] = line.split("\t");

        List<String> dimNames = new ArrayList<>();
        for(int i = 0; i < offset; i++) {
            dimNames.add(tokens[i]);
        }

        List<String> values = new ArrayList<>();
        for(int i = offset; i < tokens.length; i++) {
            values.add(tokens[i]);
        }

        while((line = reader.readLine()) != null) {
            tokens = line.split("\t");

            SortedMap<String, String> dimensions = new TreeMap<>();
            for(int i = 0; i < dimNames.size(); i++) {
                dimensions.put(dimNames.get(i), tokens[i]);
            }

            TObjectDoubleMap<String> shares = new TObjectDoubleHashMap<>();
            for(int i = 0; i < values.size(); i++) {
                shares.put(values.get(i), Double.parseDouble(tokens[i + offset]));
            }

            shareTable.add(new ImmutablePair<>(dimensions, shares));
        }

        return shareTable;
    }
}
