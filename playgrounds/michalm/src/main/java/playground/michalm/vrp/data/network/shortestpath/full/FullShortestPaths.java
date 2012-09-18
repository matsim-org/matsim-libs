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
import playground.michalm.vrp.data.network.shortestpath.ShortestPath.SPEntry;


public class FullShortestPaths
{
    private final static Logger log = Logger.getLogger(FullShortestPaths.class);


    public static FullShortestPath[][] findShortestPaths(
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

        FullShortestPath[][] shortestPaths = (FullShortestPath[][])Array.newInstance(
                FullShortestPath.class, n, n);

        List<Vertex> vertices = graph.getVertices();

        for (Vertex a : vertices) {
            MatsimVertex vA = (MatsimVertex)a;

            log.info("findShortestPaths ==== vertex: " + vA + " ====");
            FullShortestPath[] sPath_A = shortestPaths[vA.getId()];
            Link fromLink = vA.getLink();

            for (Vertex b : vertices) {
                MatsimVertex vB = (MatsimVertex)b;

                FullShortestPath sPath_AB = new FullShortestPath(timeDiscretizer);
                sPath_A[vB.getId()] = sPath_AB;

                Link toLink = vB.getLink();
                SPEntry[] entries = sPath_AB.entries;

                for (int k = 0; k < entries.length; k++) {
                    int departTime = k * timeInterval;// + travelTimeBinSize/2 TODO
                    entries[k] = shortestPathCalculator.calculateSPEntry(fromLink, toLink,
                            departTime);
                }
            }
        }

        log.info("findShortestPaths(Controler) ==== FINISHED ====");
        // Check out "NetworkLegRouter" what one can make with Paths in order to build Routes

        return shortestPaths;
    }


    public static void writeShortestPaths(FullShortestPath[][] shortestPaths, String timesFileName,
            String costsFileName, String pathsFileName)
        throws IOException
    {
        log.info("writeShortestPaths() ==== STARTED ====");

        BufferedWriter timesBW = new BufferedWriter(new FileWriter(timesFileName));
        BufferedWriter costsBW = new BufferedWriter(new FileWriter(costsFileName));
        BufferedWriter pathsBW = new BufferedWriter(new FileWriter(pathsFileName));

        for (int i = 0; i < shortestPaths.length; i++) {
            FullShortestPath[] sPath_i = shortestPaths[i];

            for (int j = 0; j < sPath_i.length; j++) {
                FullShortestPath sPath_ij = sPath_i[j];
                timesBW.write(i + "->" + j + "\t");
                costsBW.write(i + "->" + j + "\t");
                pathsBW.write(i + "->" + j + "\t");

                SPEntry[] entries = sPath_ij.entries;

                for (int k = 0; k < entries.length; k++) {
                    SPEntry entry = entries[k];
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


    public static FullShortestPath[][] readShortestPaths(TimeDiscretizer timeDiscretizer,
            MatsimVrpData data, String timesFileName, String costsFileName, String pathsFileName)
        throws IOException
    {
        log.info("readShortestPaths() ==== STARTED ====");

        int n = data.getMatsimVrpGraph().getVertexCount();
        boolean readPaths = pathsFileName != null;

        FullShortestPath[][] shortestPaths = (FullShortestPath[][])Array.newInstance(
                FullShortestPath.class, n, n);

        BufferedReader timesBR = getReader(new File(timesFileName));
        BufferedReader costsBR = getReader(new File(costsFileName));
        BufferedReader pathsBR = readPaths ? getReader(new File(pathsFileName)) : null;

        for (int i = 0; i < n; i++) {
            FullShortestPath[] sPath_i = shortestPaths[i];

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

                FullShortestPath sPath_ij = new FullShortestPath(timeDiscretizer);
                sPath_i[j] = sPath_ij;

                SPEntry[] entries = sPath_ij.entries;

                for (int k = 0; k < entries.length; k++) {
                    double travelTime = timesTST.nextDouble();
                    double travelCost = costsTST.nextDouble();
                    Id[] linkIds = null;

                    if (readPaths) {
                        int linkIdCount = pathsTST.nextInt();

                        if (linkIdCount == 0) {
                            entries[k] = ShortestPath.ZERO_PATH_ENTRY;
                            continue;
                        }

                        Scenario scenario = data.getScenario();
                        linkIds = new Id[linkIdCount];

                        for (int l = 0; l < linkIdCount; l++) {
                            linkIds[l] = scenario.createId(pathsTST.nextToken());
                        }
                    }

                    entries[k] = new SPEntry((int)travelTime, travelCost, linkIds);
                }
            }
        }

        timesBR.close();
        costsBR.close();

        if (readPaths) {
            pathsBR.close();
        }

        log.info("readShortestPaths() ==== FINISHED ====");
        return shortestPaths;
    }


    /**
     * Updates travel times and costs
     */
    public static void upadateVrpArcs(FullShortestPath[][] shortestPaths,
            TimeDiscretizer timeDiscretizer, FixedSizeMatsimVrpGraph graph)
    {
        log.info("upadateVrpArcs() ==== STARTED ====");
        List<Vertex> vertices = graph.getVertices();

        for (Vertex vA : vertices) {
            FullShortestPath[] sPath_i = shortestPaths[vA.getId()];

            for (Vertex vB : vertices) {
                graph.setArc(vA, vB,
                        FullShortestPathArc.createArc(timeDiscretizer, sPath_i[vB.getId()]));
            }
        }

        log.info("upadateVrpArcs() ==== FINISHED ====");
    }
}
