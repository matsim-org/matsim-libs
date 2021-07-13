package org.matsim.application.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

            if (trafficVolumeCar > 0.8 * freeFlowVolume) {
                int newValue = highTrafficVolumeLinkMap.getOrDefault(linkId, 0) - 10;
                highTrafficVolumeLinkMap.put(linkId, newValue);
            } else if (trafficVolumeCar > 0.7 * freeFlowVolume) {
                int newValue = highTrafficVolumeLinkMap.getOrDefault(linkId, 0) - 3;
                highTrafficVolumeLinkMap.put(linkId, newValue);
            } else if (trafficVolumeCar > 0.6 * freeFlowVolume) {
                int newValue = highTrafficVolumeLinkMap.getOrDefault(linkId, 0) - 1;
                highTrafficVolumeLinkMap.put(linkId, newValue);
            }

            if (avgTravelTime > 1.5 * freeTravelTime + 10) {
                int newValue = longTravelTimeLinkMap.getOrDefault(linkId, 0) - 10;
                longTravelTimeLinkMap.put(linkId, newValue);
            } else if (avgTravelTime > 1.2 * freeTravelTime + 5) {
                int newValue = longTravelTimeLinkMap.getOrDefault(linkId, 0) - 3;
                longTravelTimeLinkMap.put(linkId, newValue);
            } else if (avgTravelTime > 1.0 * freeTravelTime + 2) {
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
        csvWriter1.append("link-id");
        csvWriter1.append(",");
        csvWriter1.append("to-node-id");
        csvWriter1.append(",");
        csvWriter1.append("X");
        csvWriter1.append(",");
        csvWriter1.append("Y");
        csvWriter1.append(",");
        csvWriter1.append("score");
        csvWriter1.append("\n");

        csvWriter2.append("link-id");
        csvWriter2.append(",");
        csvWriter2.append("to-node-id");
        csvWriter2.append(",");
        csvWriter2.append("X");
        csvWriter2.append(",");
        csvWriter2.append("Y");
        csvWriter2.append(",");
        csvWriter2.append("score");
        csvWriter2.append("\n");

        for (int i = 0; i < 100; i++) {
            if (i < highTrafficVolumeLinksSize) {
                Link highTrafficVolumeLink = network.getLinks().get(trafficVolumeRank.get(i).getKey());
                csvWriter1.append(highTrafficVolumeLink.getId().toString());
                csvWriter1.append(",");
                csvWriter1.append(highTrafficVolumeLink.getToNode().getId().toString());
                csvWriter1.append(",");
                csvWriter1.append(Double.toString(highTrafficVolumeLink.getToNode().getCoord().getX()));
                csvWriter1.append(",");
                csvWriter1.append(Double.toString(highTrafficVolumeLink.getToNode().getCoord().getY()));
                csvWriter1.append(",");
                csvWriter1.append(Integer.toString(trafficVolumeRank.get(i).getValue()));
                csvWriter1.append("\n");
            }

            if (i < longTravelTimeLinksSize) {
                Link longTravelTimeLink = network.getLinks().get(travelTimeRank.get(i).getKey());
                csvWriter2.append(longTravelTimeLink.getId().toString());
                csvWriter2.append(",");
                csvWriter2.append(longTravelTimeLink.getToNode().getId().toString());
                csvWriter2.append(",");
                csvWriter2.append(Double.toString(longTravelTimeLink.getToNode().getCoord().getX()));
                csvWriter2.append(",");
                csvWriter2.append(Double.toString(longTravelTimeLink.getToNode().getCoord().getY()));
                csvWriter2.append(",");
                csvWriter2.append(Integer.toString(travelTimeRank.get(i).getValue()));
                csvWriter2.append("\n");
            }
        }
        csvWriter1.close();
        csvWriter2.close();
    }
}
