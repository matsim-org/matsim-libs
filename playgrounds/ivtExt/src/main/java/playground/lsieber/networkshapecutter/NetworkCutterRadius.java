package playground.lsieber.networkshapecutter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.CoordUtils;

public class NetworkCutterRadius extends NetworkCutter {

    private double radius;
    private Coord center;
    private Network modifiedNetwork;

    public NetworkCutterRadius(Coord center, double radius) {
        // TODO Auto-generated constructor stub
        this.radius = radius;
        this.center = center;
    }

    @Override
    public Network process(Network network) throws MalformedURLException, IOException {
        if (this.radius == 0.0) {
            System.out.println("hey");
            return network;
        } else {

            modifiedNetwork = filterInternal(network);

            long numberOfLinksOriginal = network.getLinks().size();
            long numberOfNodesOriginal = network.getNodes().size();
            long numberOfLinksFiltered = modifiedNetwork.getLinks().size();
            long numberOfNodesFiltered = modifiedNetwork.getNodes().size();

            cutInfo += "  Number of Links in original network: " + numberOfLinksOriginal + "\n";
            cutInfo += "  Number of nodes in original network: " + numberOfNodesOriginal + "\n";
            cutInfo += String.format("  Number of nodes in filtered network: %d (%.2f%%)", numberOfNodesFiltered,
                    100.0 * numberOfNodesFiltered / numberOfNodesOriginal) + "\n";
            cutInfo += String.format("  Number of links in filtered network: %d (%.2f%%)", numberOfLinksFiltered,
                    100.0 * numberOfLinksFiltered / numberOfLinksOriginal) + "\n";

            printCutSummary();
            return modifiedNetwork;
        }
    }

    private Network filterInternal(Network originalNetwork) {
        if (radius == 0) {
            cutInfo += "No cutting radius given! No cutting executed!";
            return originalNetwork;
        } else {
            Network filteredNetwork = NetworkUtils.createNetwork();

            for (Node node : originalNetwork.getNodes().values()) {
                if (CoordUtils.calcEuclideanDistance(node.getCoord(), center) <= radius) {
                    filteredNetwork.addNode(filteredNetwork.getFactory().createNode(node.getId(), node.getCoord()));
                }
            }

            for (Link link : originalNetwork.getLinks().values()) {
                Node filteredFromNode = filteredNetwork.getNodes().get(link.getFromNode().getId());
                Node filteredToNode = filteredNetwork.getNodes().get(link.getToNode().getId());

                if (filteredFromNode != null && filteredToNode != null) {

                    Link newLink = filteredNetwork.getFactory().createLink(link.getId(), filteredFromNode, filteredToNode);

                    newLink.setAllowedModes(link.getAllowedModes());
                    newLink.setLength(link.getLength());
                    newLink.setCapacity(link.getCapacity());
                    newLink.setFreespeed(link.getFreespeed());
                    newLink.setNumberOfLanes(link.getNumberOfLanes());

                    filteredNetwork.addLink(newLink);
                    
                }
            }

            new NetworkCleaner().run(filteredNetwork);

            return filteredNetwork;
        }
        
    }

}
