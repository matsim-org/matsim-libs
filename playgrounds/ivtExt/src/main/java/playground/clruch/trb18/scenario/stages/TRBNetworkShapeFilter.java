package playground.clruch.trb18.scenario.stages;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;

public class TRBNetworkShapeFilter {
    final private Logger logger = Logger.getLogger(TRBNetworkRadiusFilter.class);

    /**
     * Filters the network for TRB:
     * - Removes all non-car links
     * - Removes all nodes which are outside of the specified area
     * - Removes all corresponding links
     *
     * The original network is left unchanged!
     *
     * Based on a shape file.
     */
    public Network filter(Network originalNetwork, String shapeFileInputPath) throws IOException {
        logger.info("Creating filtered network ...");

        long numberOfLinksOriginal = originalNetwork.getLinks().size();
        long numberOfNodesOriginal = originalNetwork.getNodes().size();

        logger.info("  Number of nodes in original network: " + numberOfNodesOriginal);
        logger.info("  Number of links in original network: " + numberOfLinksOriginal);

        Network filteredNetwork = NetworkUtils.createNetwork();

        Map inputMap = new HashMap<>();
        inputMap.put("url", new File(shapeFileInputPath).toURI().toURL());
        DataStore dataStore = DataStoreFinder.getDataStore(inputMap);

        SimpleFeatureSource featureSource = dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
        SimpleFeatureCollection collection = DataUtilities.collection(featureSource.getFeatures());
        dataStore.dispose();

        GeometryFactory factory = new GeometryFactory();

        for (Node node : originalNetwork.getNodes().values()) {
            SimpleFeatureIterator iterator = collection.features();

            while (iterator.hasNext()) {
                MultiPolygon polygon = (MultiPolygon) iterator.next().getDefaultGeometry();

                if (polygon.contains(factory.createPoint(new Coordinate(node.getCoord().getX(), node.getCoord().getY())))) {
                    filteredNetwork.addNode(filteredNetwork.getFactory().createNode(node.getId(), node.getCoord()));
                    break;
                }
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
    static public void main(String[] args) throws IOException {
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(args[0]);
        new TRBNetworkShapeFilter().filter(network, "stadtkreis/Stadtkreis.shp");
        new NetworkWriter(network).write(args[1]);
    }
}