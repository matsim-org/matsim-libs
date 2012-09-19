/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.vrp.data.network.shortestpath.full;

import java.io.*;
import java.lang.reflect.Array;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;

import pl.poznan.put.util.TypedStringTokenizer;
import pl.poznan.put.util.lang.TimeDiscretizer;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.data.network.shortestpath.*;


public class FullShortestPaths
{
    private final static Logger log = Logger.getLogger(FullShortestPaths.class);


    public static ShortestPath[][][] findShortestPaths(
            ShortestPathCalculator shortestPathCalculator, TimeDiscretizer timeDiscretizer,
            MatsimVrpGraph graph)
    {
        log.info("findShortestPaths(Controler) ==== STARTED ====");

        int timeInterval = timeDiscretizer.getTimeInterval();

        // created by: TravelTimeCalculatorFactoryImpl, setting from:
        // TravelTimeCalculatorConfigGroup
        // a. travelTimeCalculatorType: "TravelTimeCalculatorArray"
        // b. travelTimeAggregator: "optimistic"

        int n = graph.getVertexCount();

        ShortestPath[][][] sPaths = (ShortestPath[][][])Array.newInstance(ShortestPath.class, n, n,
                timeInterval);

        List<Vertex> vertices = graph.getVertices();

        for (Vertex a : vertices) {
            MatsimVertex vA = (MatsimVertex)a;

            log.info("findShortestPaths ==== vertex: " + vA + " ====");
            ShortestPath[][] sPaths_A = sPaths[vA.getId()];
            Link fromLink = vA.getLink();

            for (Vertex b : vertices) {
                MatsimVertex vB = (MatsimVertex)b;

                ShortestPath[] sPaths_AB = sPaths_A[vB.getId()];
                Link toLink = vB.getLink();

                for (int k = 0; k < sPaths_AB.length; k++) {
                    int departTime = k * timeInterval;// + travelTimeBinSize/2 TODO
                    sPaths_AB[k] = shortestPathCalculator.calculateShortestPath(fromLink, toLink,
                            departTime);
                }
            }
        }

        log.info("findShortestPaths(Controler) ==== FINISHED ====");
        // Check out "NetworkLegRouter" what one can make with Paths in order to build Routes

        return sPaths;
    }


    public static void writeShortestPaths(ShortestPath[][][] sPaths, String timesFileName,
            String costsFileName, String pathsFileName)
        throws IOException
    {
        log.info("writeShortestPaths() ==== STARTED ====");

        BufferedWriter timesBW = new BufferedWriter(new FileWriter(timesFileName));
        BufferedWriter costsBW = new BufferedWriter(new FileWriter(costsFileName));
        BufferedWriter pathsBW = new BufferedWriter(new FileWriter(pathsFileName));

        for (int i = 0; i < sPaths.length; i++) {
            ShortestPath[][] sPaths_i = sPaths[i];

            for (int j = 0; j < sPaths_i.length; j++) {
                ShortestPath[] sPaths_ij = sPaths_i[j];
                timesBW.write(i + "->" + j + "\t");
                costsBW.write(i + "->" + j + "\t");
                pathsBW.write(i + "->" + j + "\t");

                for (int k = 0; k < sPaths_ij.length; k++) {
                    ShortestPath entry = sPaths_ij[k];
                    timesBW.write(entry.travelTime + "\t");
                    costsBW.write(entry.travelCost + "\t");
                    pathsBW.write(entry.linkIds.length + "\t");// number of linkIds

                    for (Id id : entry.linkIds) {
                        pathsBW.write(id + "\t");// each linkId
                    }
                }

                timesBW.newLine();
                costsBW.newLine();
                pathsBW.newLine();
            }
        }

        timesBW.close();
        costsBW.close();
        pathsBW.close();

        log.info("writeShortestPaths() ==== FINISHED ====");
    }


    public static void readShortestPaths(TimeDiscretizer timeDiscretizer, MatsimVrpData data,
            String timesFileName, String costsFileName)
        throws IOException
    {
        readShortestPaths(timeDiscretizer, data, timesFileName, costsFileName, null);
    }


    private static BufferedReader getReader(File file)
        throws IOException
    {
        if (file.getName().endsWith(".gz")) {
            return new BufferedReader(new InputStreamReader(new GZIPInputStream(
                    new FileInputStream(file))));
        }
        else {
            return new BufferedReader(new FileReader(file));
        }
    }


    public static ShortestPath[][][] readShortestPaths(TimeDiscretizer timeDiscretizer,
            MatsimVrpData data, String timesFileName, String costsFileName, String pathsFileName)
        throws IOException
    {
        log.info("readShortestPaths() ==== STARTED ====");

        int n = data.getMatsimVrpGraph().getVertexCount();
        boolean readPaths = pathsFileName != null;

        ShortestPath[][][] sPaths = (ShortestPath[][][])Array.newInstance(ShortestPath.class, n, n,
                timeDiscretizer.getIntervalCount());

        BufferedReader timesBR = getReader(new File(timesFileName));
        BufferedReader costsBR = getReader(new File(costsFileName));
        BufferedReader pathsBR = readPaths ? getReader(new File(pathsFileName)) : null;

        for (int i = 0; i < n; i++) {
            ShortestPath[][] sPaths_i = sPaths[i];

            for (int j = 0; j < n; j++) {
                TypedStringTokenizer timesTST = new TypedStringTokenizer(timesBR.readLine(), "\t");
                TypedStringTokenizer costsTST = new TypedStringTokenizer(costsBR.readLine(), "\t");
                TypedStringTokenizer pathsTST = readPaths ? new TypedStringTokenizer(
                        pathsBR.readLine(), "\t") : null;

                timesTST.nextToken();// line beginning
                costsTST.nextToken();// line beginning

                if (readPaths) {
                    pathsTST.nextToken();// line beginning
                }

                ShortestPath[] sPaths_ij = sPaths_i[j];

                for (int k = 0; k < sPaths_ij.length; k++) {
                    double travelTime = timesTST.nextDouble();
                    double travelCost = costsTST.nextDouble();
                    Id[] linkIds = null;

                    if (readPaths) {
                        int linkIdCount = pathsTST.nextInt();

                        if (linkIdCount == 0) {
                            sPaths_ij[k] = ShortestPath.ZERO_PATH_ENTRY;
                            continue;
                        }

                        Scenario scenario = data.getScenario();
                        linkIds = new Id[linkIdCount];

                        for (int l = 0; l < linkIdCount; l++) {
                            linkIds[l] = scenario.createId(pathsTST.nextToken());
                        }
                    }

                    sPaths_ij[k] = new ShortestPath((int)travelTime, travelCost, linkIds);
                }
            }
        }

        timesBR.close();
        costsBR.close();

        if (readPaths) {
            pathsBR.close();
        }

        log.info("readShortestPaths() ==== FINISHED ====");
        return sPaths;
    }


    /**
     * Updates travel times and costs
     */
    public static void upadateVrpArcs(ShortestPath[][][] sPaths, TimeDiscretizer timeDiscretizer,
            FixedSizeMatsimVrpGraph graph)
    {
        log.info("upadateVrpArcs() ==== STARTED ====");
        List<Vertex> vertices = graph.getVertices();

        for (Vertex vA : vertices) {
            ShortestPath[][] sPaths_i = sPaths[vA.getId()];

            for (Vertex vB : vertices) {
                graph.setArc(vA, vB,
                        FullShortestPathArc.createArc(timeDiscretizer, sPaths_i[vB.getId()]));
            }
        }

        log.info("upadateVrpArcs() ==== FINISHED ====");
    }
}
