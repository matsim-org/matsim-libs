/**
 * 
 */
package playground.lsieber.networkshapecutter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;

/** @author Claudio Ruch */
public class NetworkCutterShape extends NetworkCutter {

    private final URL shapefileUrl;
    private Network modifiedNetwork;

    public NetworkCutterShape(File shapefile) throws MalformedURLException {
        // TODO check your input here and make sure it fails if the input does not meet required conditions.
        shapefileUrl = shapefile.toURI().toURL();
    }

    @Override
    public Network process(Network network) throws MalformedURLException, IOException {
        Network modifiedNetwork = filterInternal(network);
        return modifiedNetwork;
    }

    public Network filterInternal(Network originalNetwork) throws IOException {

        modifiedNetwork = NetworkUtils.createNetwork();

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
                    modifiedNetwork.addNode(modifiedNetwork.getFactory().createNode(node.getId(), node.getCoord()));
                    break;
                }
            }
        }

        for (Link link : originalNetwork.getLinks().values()) {
            Node filteredFromNode = modifiedNetwork.getNodes().get(link.getFromNode().getId());
            Node filteredToNode = modifiedNetwork.getNodes().get(link.getToNode().getId());

            if (filteredFromNode != null && filteredToNode != null) {
                Link newLink = modifiedNetwork.getFactory().createLink(link.getId(), filteredFromNode, filteredToNode);

                // newLink.setAllowedModes(Collections.singleton("car"));
                newLink.setAllowedModes(link.getAllowedModes());

                newLink.setLength(link.getLength());
                newLink.setCapacity(link.getCapacity());
                newLink.setFreespeed(link.getFreespeed());
                // newLink.setNumberOfLanes(link.getNumberOfLanes());

                modifiedNetwork.addLink(newLink);
            }
        }

        // new NetworkCleaner().run(modesFilteredNetwork);

        // new NetworkCleaner().run(filteredNetwork);

        return modifiedNetwork;
    }

}
