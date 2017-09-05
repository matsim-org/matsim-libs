package playground.clruch.trb18.scenario.stages;

import java.util.Collections;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.CoordUtils;

import contrib.baseline.preparation.ZHCutter;

public class TRBNetworkRadiusFilter {
    final private Logger logger = Logger.getLogger(TRBNetworkRadiusFilter.class);

    /**
     * Filters the network for TRB:
     * - Removes all non-car links
     * - Removes all nodes which are outside of the specified area
     * - Removes all corresponding links
     *
     * The original network is left unchanged!
     */
    public Network filter(Network originalNetwork, Coord scenarioCenterCoord, double scenarioRadius) {
        logger.info("Creating filtered network ...");

        long numberOfLinksOriginal = originalNetwork.getLinks().size();
        long numberOfNodesOriginal = originalNetwork.getNodes().size();

        logger.info("  Number of nodes in original network: " + numberOfNodesOriginal);
        logger.info("  Number of links in original network: " + numberOfLinksOriginal);

        Network filteredNetwork = NetworkUtils.createNetwork();

        for (Node node : originalNetwork.getNodes().values()) {
            if (CoordUtils.calcEuclideanDistance(node.getCoord(), scenarioCenterCoord) <= scenarioRadius) {
                filteredNetwork.addNode(filteredNetwork.getFactory().createNode(node.getId(), node.getCoord()));
            }
        }

        for (Link link : originalNetwork.getLinks().values()) {
            Node filteredFromNode = filteredNetwork.getNodes().get(link.getFromNode().getId());
            Node filteredToNode = filteredNetwork.getNodes().get(link.getToNode().getId());

            if (filteredFromNode != null && filteredToNode != null) {
                if (link.getAllowedModes().contains("car")) {
                    Link newLink = filteredNetwork.getFactory().createLink(link.getId(), filteredFromNode, filteredToNode);

                    newLink.setAllowedModes(Collections.singleton("car"));
                    newLink.setLength(link.getLength());
                    newLink.setCapacity(link.getCapacity());
                    newLink.setFreespeed(link.getFreespeed());
                    newLink.setNumberOfLanes(link.getNumberOfLanes());

                    filteredNetwork.addLink(newLink);
                }
            }
        }

        new NetworkCleaner().run(filteredNetwork);

        logger.info("Finished creating filtered network!");

        long numberOfLinksFiltered = filteredNetwork.getLinks().size();
        long numberOfNodesFiltered = filteredNetwork.getNodes().size();

        logger.info(String.format("  Number of nodes in filtered network: %d (%.2f%%)", numberOfNodesFiltered, 100.0 * numberOfNodesFiltered / numberOfNodesOriginal));
        logger.info(String.format("  Number of links in filtered network: %d (%.2f%%)", numberOfLinksFiltered, 100.0 * numberOfLinksFiltered / numberOfLinksOriginal));

        return filteredNetwork;
    }

    /**
     * For testing purposes
     */
    static public void main(String[] args) {
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(args[0]);
        ZHCutter.ZHCutterConfigGroup zhCutterConfigGroup = new ZHCutter.ZHCutterConfigGroup("");
        new TRBNetworkRadiusFilter().filter(network, new Coord(zhCutterConfigGroup.getxCoordCenter(), zhCutterConfigGroup.getyCoordCenter()), 15000.0);
        new NetworkWriter(network).write(args[1]);
    }
}
