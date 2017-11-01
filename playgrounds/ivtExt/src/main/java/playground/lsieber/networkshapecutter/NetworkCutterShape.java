/**
 * 
 */
package playground.lsieber.networkshapecutter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
public class NetworkCutterShape implements NetworkCutter {

    private String cutInfo = null;
    private URL shapefileUrl;
    private Network modifiedNetwork;

    public NetworkCutterShape(File shapefile) throws MalformedURLException {
        // TODO check your input here and make sure it fails if the input does not meet required conditions.
        this.shapefileUrl = shapefile.toURI().toURL();
    }

    @Override
    public Network filter(Network network) throws MalformedURLException, IOException {
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

    @Override
    public void printCutSummary() {
        System.out.println(cutInfo); // TODO this will produce an exeption, fill thet info while cutting!
    }

    @Override
    public void checkNetworkConsistency() {
        // TODO Auto-generated method stub

    }

    public Network filterInternal(Network originalNetwork) throws IOException {

        Network filteredNetwork = NetworkUtils.createNetwork();

        Map<String, URL> inputMap = new HashMap<>();
        inputMap.put("url", shapefileUrl);
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

        return filteredNetwork;
    }

}
