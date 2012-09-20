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

package playground.michalm.vrp.data.network.shortestpath;

import java.io.*;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.*;

import pl.poznan.put.util.TypedStringTokenizer;
import pl.poznan.put.util.lang.TimeDiscretizer;
import pl.poznan.put.vrp.dynamic.data.network.*;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.network.*;


public class FullDiscreteMatsimArcIO
{
    private final static Logger log = Logger.getLogger(FullDiscreteMatsimArcIO.class);


    public static void writeShortestPaths(MatsimVrpGraph graph, String timesFileName,
            String costsFileName, String pathsFileName)
        throws IOException
    {
        log.info("writeShortestPaths() ==== STARTED ====");

        BufferedWriter timesBW = new BufferedWriter(new FileWriter(timesFileName));
        BufferedWriter costsBW = new BufferedWriter(new FileWriter(costsFileName));
        BufferedWriter pathsBW = new BufferedWriter(new FileWriter(pathsFileName));

        ArcIterator arcIter = graph.arcIterator();

        while (arcIter.hasNext()) {
            int aId = arcIter.getVertexFrom().getId();
            int bId = arcIter.getVertexTo().getId();

            FullDiscreteMatsimArc arc = (FullDiscreteMatsimArc)arcIter.getArc();
            timesBW.write(aId + "->" + bId + "\t");
            costsBW.write(aId + "->" + bId + "\t");
            pathsBW.write(aId + "->" + bId + "\t");

            ShortestPath[] sPath = arc.getShortestPaths();

            for (int k = 0; k < sPath.length; k++) {
                ShortestPath sPaths_ijk = sPath[k];
                timesBW.write(sPaths_ijk.travelTime + "\t");
                costsBW.write(sPaths_ijk.travelCost + "\t");
                pathsBW.write(sPaths_ijk.linkIds.length + "\t");// number of linkIds

                for (Id id : sPaths_ijk.linkIds) {
                    pathsBW.write(id + "\t");// each linkId
                }
            }

            timesBW.newLine();
            costsBW.newLine();
            pathsBW.newLine();
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


    public static void readShortestPaths(TimeDiscretizer timeDiscretizer, MatsimVrpData data,
            String timesFileName, String costsFileName, String pathsFileName)
        throws IOException
    {
        log.info("readShortestPaths() ==== STARTED ====");

        FixedSizeMatsimVrpGraph graph = (FixedSizeMatsimVrpGraph)data.getMatsimVrpGraph();

        boolean readPaths = pathsFileName != null;
        BufferedReader timesBR = getReader(new File(timesFileName));
        BufferedReader costsBR = getReader(new File(costsFileName));
        BufferedReader pathsBR = readPaths ? getReader(new File(pathsFileName)) : null;

        String timesBRLine;
        while ( (timesBRLine = timesBR.readLine()) != null) {
            TypedStringTokenizer timesTST = new TypedStringTokenizer(timesBRLine, "\t");
            TypedStringTokenizer costsTST = new TypedStringTokenizer(costsBR.readLine(), "\t");
            TypedStringTokenizer pathsTST = readPaths ? new TypedStringTokenizer(
                    pathsBR.readLine(), "\t") : null;

            String arcId = timesTST.nextToken();// line beginning
            costsTST.nextToken();// line beginning

            if (readPaths) {
                pathsTST.nextToken();// line beginning
            }

            int arrowMarkIdx = arcId.indexOf("->");
            int fromIdx = Integer.valueOf(arcId.substring(0, arrowMarkIdx));
            int toIdx = Integer.valueOf(arcId.substring(arrowMarkIdx + 2));

            Vertex fromVertex = graph.getVertex(fromIdx);
            Vertex toVertex = graph.getVertex(toIdx);

            ShortestPath[] sPaths_ij = new ShortestPath[timeDiscretizer.getIntervalCount()];

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

            graph.setArc(fromVertex, toVertex,
                    new FullDiscreteMatsimArc(timeDiscretizer, sPaths_ij));
        }

        timesBR.close();
        costsBR.close();

        if (readPaths) {
            pathsBR.close();
        }

        log.info("readShortestPaths() ==== FINISHED ====");
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

}
