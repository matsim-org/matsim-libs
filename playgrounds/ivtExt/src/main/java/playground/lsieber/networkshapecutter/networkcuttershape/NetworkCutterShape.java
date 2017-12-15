/**
 * 
 */
package playground.lsieber.networkshapecutter.networkcuttershape;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashSet;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

import com.vividsolutions.jts.geom.MultiPolygon;

import ch.ethz.idsc.queuey.core.networks.MultiPolygons;
import playground.lsieber.networkshapecutter.NetworkCutter;

/** @author Claudio Ruch */
public class NetworkCutterShape extends NetworkCutter {

    private Network modifiedNetwork;
    private final MultiPolygons multipolygons;

    public NetworkCutterShape(File shapefile) throws IOException {
        this.multipolygons = new MultiPolygons(shapefile);
    }

    @Override
    public Network process(Network network) throws MalformedURLException, IOException {
        Network modifiedNetwork = filterInternal(network);
        return modifiedNetwork;
    }

    public Network filterInternal(Network originalNetwork) throws IOException {
        modifiedNetwork = NetworkUtils.createNetwork();
        HashSet<MultiPolygon> polygons = multipolygons.getPolygons();
        AddContainedNodes.of(originalNetwork).in(polygons).to(modifiedNetwork);
        AddContainedLinks.of(originalNetwork).to(modifiedNetwork);
        return modifiedNetwork;
    }

}

// GeometryFactory factory = new GeometryFactory();
//
// for (Node node : originalNetwork.getNodes().values()) {
// SimpleFeatureIterator iterator = collection.features();
// System.out.println("the iterator has " + collection.size() + " entries.");
//
// while (iterator.hasNext()) {
// MultiPolygon polygon = (MultiPolygon) iterator.next().getDefaultGeometry();
//
// if (polygon.contains(factory.createPoint(new Coordinate(node.getCoord().getX(), node.getCoord().getY())))) {
// modifiedNetwork.addNode(modifiedNetwork.getFactory().createNode(node.getId(), node.getCoord()));
// break;
// }
// }
// }

// private final void addContainedNodesIn(HashSet<MultiPolygon> polygons, Network originalNetwork) {
//
// GeometryFactory factory = new GeometryFactory();
// for (Node node : originalNetwork.getNodes().values()) {
// for (MultiPolygon polygon : polygons) {
// Coordinate coordinate = new Coordinate(node.getCoord().getX(), node.getCoord().getY());
// if (polygon.contains(factory.createPoint(coordinate))) {
// modifiedNetwork.addNode(modifiedNetwork.getFactory().createNode(node.getId(), node.getCoord()));
// break;
// }
// }
// }
// }

// addContainedNodesIn(polygons, originalNetwork);