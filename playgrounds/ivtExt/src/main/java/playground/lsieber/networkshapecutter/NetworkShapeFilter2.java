/**
 * 
 */
package playground.lsieber.networkshapecutter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;

/** @author Claudio Ruch */
public class NetworkShapeFilter2 implements NetworkCutter {

    private String shapefilePath;
    private String cutInfo = "no cut done yet";

    // TODO can you pass a File instead of a String?
    public NetworkShapeFilter2(String shapefilePath) {
        // TODO check your input here and make sure it fails if the input does not meet required conditions.
        this.shapefilePath = shapefilePath;
    }

    @Override
    public Network filter(Network network) {
        return filterInternal(network);
    }

    @Override
    public void printCutSummary() {
        System.out.println(cutInfo); // TODO this will produce an exeption, fill thet info while cutting!

    }

    private Network filterInternal(Network originalNetwork) {
        // logger.info("Creating filtered network ...");

        long numberOfLinksOriginal = originalNetwork.getLinks().size();
        long numberOfNodesOriginal = originalNetwork.getNodes().size();

        // logger.info(" Number of nodes in original network: " + numberOfNodesOriginal);
        // logger.info(" Number of links in original network: " + numberOfLinksOriginal);

        Network filteredNetwork = NetworkUtils.createNetwork();

        Map inputMap = new HashMap<>();
        try {
            inputMap.put("url", new File(shapefilePath).toURI().toURL());
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        DataStore dataStore = null;
        try {
            dataStore = DataStoreFinder.getDataStore(inputMap);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        SimpleFeatureSource featureSource = null;
        try {
            featureSource = dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        SimpleFeatureCollection collection = null;
        try {
            collection = DataUtilities.collection(featureSource.getFeatures());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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

        // logger.info("Finished creating filtered network!");

        long numberOfLinksFiltered = filteredNetwork.getLinks().size();
        long numberOfNodesFiltered = filteredNetwork.getNodes().size();

        // logger.info(String.format(" Number of nodes in filtered network: %d (%.2f%%)", numberOfNodesFiltered,
        // 100.0 * numberOfNodesFiltered / numberOfNodesOriginal));
        // logger.info(String.format(" Number of links in filtered network: %d (%.2f%%)", numberOfLinksFiltered,
        // 100.0 * numberOfLinksFiltered / numberOfLinksOriginal));

        return filteredNetwork;
    }

}
