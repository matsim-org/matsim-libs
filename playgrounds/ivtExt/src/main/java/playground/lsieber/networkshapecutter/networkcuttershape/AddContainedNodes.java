/**
 * 
 */
package playground.lsieber.networkshapecutter.networkcuttershape;

import java.util.HashSet;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;

/** @author Claudio Ruch */
public class AddContainedNodes {

    private final Network originalNetwork;
    private HashSet<MultiPolygon> polygons;

    /* package */ static AddContainedNodes of(Network originalNetwork) {
        return new AddContainedNodes(originalNetwork);
    }

    private AddContainedNodes(Network originalNetwork) {
        this.originalNetwork = originalNetwork;

    }
    
    

    /* package */ AddContainedNodes in(HashSet<MultiPolygon> polygons) {
        this.polygons = polygons;
        return this;
    }

    /* package */ void to(Network modifiedNetwork) {

        GeometryFactory factory = new GeometryFactory();
        for (Node node : originalNetwork.getNodes().values()) {
            for (MultiPolygon polygon : polygons) {
                Coordinate coordinate = new Coordinate(node.getCoord().getX(), node.getCoord().getY());
                if (polygon.contains(factory.createPoint(coordinate))) {
                    modifiedNetwork.addNode(modifiedNetwork.getFactory().createNode(node.getId(), node.getCoord()));
                    break;
                }
            }
        }

    }

}
