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

package playground.michalm.zone.util;

import java.io.*;
import java.util.*;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.run.VrpConfigUtils;
import org.matsim.core.network.*;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;


public class InterZoneDistanceCalculator
{
    private static class ZoneCentroid
    {
        private final int zoneId;
        private final Node node;


        private ZoneCentroid(int zoneId, Node node)
        {
            this.zoneId = zoneId;
            this.node = node;
        }
    }


    private Scenario scenario;
    private LeastCostPathCalculator router;
    private ZoneCentroid[] zoneCentroids;


    private void readNetwork(String file)
    {
        scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        MatsimNetworkReader nr = new MatsimNetworkReader(scenario);
        nr.readFile(file);
    }


    private void readZoneCentroids(String file)
    {
        NetworkImpl network = (NetworkImpl)scenario.getNetwork();
        List<ZoneCentroid> zoneCentroidList = new ArrayList<>();

        try (Scanner scanner = new Scanner(new File(file))) {
            scanner.nextLine();// skip the header line

            while (scanner.hasNext()) {
                int zoneId = scanner.nextInt();
                double x = scanner.nextDouble();
                double y = scanner.nextDouble();
                Node node = network.getNearestNode(scenario.createCoord(x, y));

                zoneCentroidList.add(new ZoneCentroid(zoneId, node));
            }
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        zoneCentroids = zoneCentroidList.toArray(new ZoneCentroid[zoneCentroidList.size()]);
    }


    private void initDijkstra(boolean distanceMode) // modes: distance or freeflow-speed time
    {
        TravelTime ttimeCalc = new FreeSpeedTravelTime();

        TravelDisutility tcostCalc = distanceMode ? new DistanceAsTravelDisutility()
                : new TimeAsTravelDisutility(ttimeCalc);

        router = new Dijkstra(scenario.getNetwork(), tcostCalc, ttimeCalc);
    }


    private void writeDistances(String file)
    {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {

            // Header line
            bw.write("\t");
            for (ZoneCentroid j : zoneCentroids) {
                bw.write(j.zoneId + "\t");
            }

            bw.newLine();

            // normal lines
            for (ZoneCentroid i : zoneCentroids) {
                System.out.println(i.zoneId + "...");

                bw.write(i.zoneId + "\t");
                for (ZoneCentroid j : zoneCentroids) {
                    Path path = router.calcLeastCostPath(i.node, j.node, 0, null, null);
                    bw.write(path.travelCost + "\t");
                }

                bw.newLine();
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void go(String networkFile, String centroidsFile, String distancesFile,
            boolean distanceMode)
    {
        readNetwork(networkFile);
        readZoneCentroids(centroidsFile);
        initDijkstra(distanceMode);
        writeDistances(distancesFile);
    }


    public static void main(String[] args)
    {
        String networkFile = "d:\\PP-rad\\poznan\\network.xml";
        String centroidsFile = "d:\\PP-rad\\poznan\\wspol_centr.txt";

        boolean distanceMode = !true;

        String distancesFile = "d:\\PP-rad\\poznan\\inter_zone_"
                + (distanceMode ? "distances.txt" : "times.txt");

        new InterZoneDistanceCalculator().go(networkFile, centroidsFile, distancesFile,
                distanceMode);
    }
}
