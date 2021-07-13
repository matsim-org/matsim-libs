package org.matsim.application.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.*;
import java.util.*;

public class CongestedLinkIdentifier {
    // At the first step this is a run script

    public static void main(String[] args) throws IOException {
        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem("EPSG:25832");
        config.network().setInputFile("/Users/luchengqi/Documents/MATSimScenarios/Dusseldorf/output/base/dd.output_network.xml.gz");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();

        String linkStatsData = "/Users/luchengqi/Documents/MATSimScenarios/Dusseldorf/output/base/linkStats.csv.gz";
        BufferedReader csvReader = IOUtils.getBufferedReader(linkStatsData);
        String title = csvReader.readLine();
        System.out.println(title);

        Map<Id<Link>, Integer> highTrafficVolumeLinkMap = new HashMap<>();
        Map<Id<Link>, Integer> longTravelTimeLinkMap = new HashMap<>();
        int counter = 0;
        while (true) {
            String dataEntry = csvReader.readLine();
            if (dataEntry == null) {
                break;
            }
            String[] data = dataEntry.split(",");
            Id<Link> linkId = Id.createLinkId(data[0]);
//            double time = Double.parseDouble(data[1]);
            double avgTravelTime = Double.parseDouble(data[2]);
            double trafficVolumeCar = Double.parseDouble(data[3]);

            Link link = network.getLinks().get(linkId);
            if (link.getAttributes().getAttribute("junction") == null) {
                continue;
            }

            if (!link.getAttributes().getAttribute("junction").equals(true)) {
                continue;
            }

            counter += 1;

            double freeTravelTime = Math.floor(link.getLength() / link.getFreespeed() + 1);
            double freeFlowVolume = link.getCapacity() / 4 / 4; // 900s interval, 25% scenario

            if (trafficVolumeCar > 0.85 * freeFlowVolume) {
                int newValue = highTrafficVolumeLinkMap.getOrDefault(linkId, 0) - 10;
                highTrafficVolumeLinkMap.put(linkId, newValue);
            } else if (trafficVolumeCar > 0.75 * freeFlowVolume) {
                int newValue = highTrafficVolumeLinkMap.getOrDefault(linkId, 0) - 3;
                highTrafficVolumeLinkMap.put(linkId, newValue);
            } else if (trafficVolumeCar > 0.6 * freeFlowVolume) {
                int newValue = highTrafficVolumeLinkMap.getOrDefault(linkId, 0) - 1;
                highTrafficVolumeLinkMap.put(linkId, newValue);
            }

            if (avgTravelTime > 2.0 * freeTravelTime && avgTravelTime > 30) {
                int newValue = longTravelTimeLinkMap.getOrDefault(linkId, 0) - 10;
                longTravelTimeLinkMap.put(linkId, newValue);
            } else if (avgTravelTime > 1.5 * freeTravelTime && avgTravelTime > 15) {
                int newValue = longTravelTimeLinkMap.getOrDefault(linkId, 0) - 3;
                longTravelTimeLinkMap.put(linkId, newValue);
            } else if (avgTravelTime > 1.0 * freeTravelTime + 5) {
                int newValue = longTravelTimeLinkMap.getOrDefault(linkId, 0) - 1;
                longTravelTimeLinkMap.put(linkId, newValue);
            }
        }

        int highTrafficVolumeLinksSize = highTrafficVolumeLinkMap.size();
        int longTravelTimeLinksSize = longTravelTimeLinkMap.size();

        System.out.println("High traffic volume links map size = " + highTrafficVolumeLinksSize);
        System.out.println("Long travel time links map size = " + longTravelTimeLinksSize);
        System.out.println("Total number of links with junction: " + counter / 96);

        List<Map.Entry<Id<Link>, Integer>> trafficVolumeRank = new ArrayList<>(highTrafficVolumeLinkMap.entrySet());
        List<Map.Entry<Id<Link>, Integer>> travelTimeRank = new ArrayList<>(longTravelTimeLinkMap.entrySet());

        trafficVolumeRank.sort(Map.Entry.comparingByValue());
        travelTimeRank.sort(Map.Entry.comparingByValue());

        FileWriter csvWriter1 = new FileWriter("/Users/luchengqi/Documents/MATSimScenarios/Dusseldorf/output/base/high-traffic-volume-links.csv");
        FileWriter csvWriter2 = new FileWriter("/Users/luchengqi/Documents/MATSimScenarios/Dusseldorf/output/base/long-travel-time-links.csv");
        writeTitle(csvWriter1);
        writeTitle(csvWriter2);

        Set<Node> nodePool1 = new HashSet<>();
        Set<Node> nodePool2 = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            if (i < highTrafficVolumeLinksSize) {
                Link highTrafficVolumeLink = network.getLinks().get(trafficVolumeRank.get(i).getKey());
                nodePool1.add(highTrafficVolumeLink.getToNode());
                writeEntry(csvWriter1, highTrafficVolumeLink, trafficVolumeRank.get(i).getValue());
            }

            if (i < longTravelTimeLinksSize) {
                Link longTravelTimeLink = network.getLinks().get(travelTimeRank.get(i).getKey());
                nodePool2.add(longTravelTimeLink.getToNode());
                writeEntry(csvWriter2, longTravelTimeLink, travelTimeRank.get(i).getValue());
            }
        }
        csvWriter1.close();
        csvWriter2.close();

        nodePool1.retainAll(nodePool2); // intersection of the two set
        FileWriter csvWriter3 = new FileWriter("/Users/luchengqi/Documents/MATSimScenarios/Dusseldorf/output/base/nodes-to-improve.csv");
        csvWriter3.append("Sequence");
        csvWriter3.append(",");
        csvWriter3.append("node-id");
        csvWriter3.append(",");
        csvWriter3.append("X");
        csvWriter3.append(",");
        csvWriter3.append("Y");
        csvWriter3.append("\n");

        int sequence = 1;
        for (Node node:nodePool1) {
            csvWriter3.append(Integer.toString(sequence));
            csvWriter3.append(",");
            csvWriter3.append(node.getId().toString());
            csvWriter3.append(",");
            csvWriter3.append(Double.toString(node.getCoord().getX()));
            csvWriter3.append(",");
            csvWriter3.append(Double.toString(node.getCoord().getY()));
            csvWriter3.append("\n");
            sequence += 1;
        }
        csvWriter3.close();
    }

    private static void writeTitle (FileWriter csvWriter) throws IOException {
        csvWriter.append("link-id");
        csvWriter.append(",");
        csvWriter.append("to-node-id");
        csvWriter.append(",");
        csvWriter.append("X");
        csvWriter.append(",");
        csvWriter.append("Y");
        csvWriter.append(",");
        csvWriter.append("score");
        csvWriter.append("\n");
    }

    private static void writeEntry (FileWriter csvWriter, Link link, int score) throws IOException {
        csvWriter.append(link.getId().toString());
        csvWriter.append(",");
        csvWriter.append(link.getToNode().getId().toString());
        csvWriter.append(",");
        csvWriter.append(Double.toString(link.getToNode().getCoord().getX()));
        csvWriter.append(",");
        csvWriter.append(Double.toString(link.getToNode().getCoord().getY()));
        csvWriter.append(",");
        csvWriter.append(Integer.toString(score));
        csvWriter.append("\n");
    }
}
