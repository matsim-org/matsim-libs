package playground.lsieber.networkshapecutter;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Population;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPolygon;

import jogamp.common.util.locks.RecursiveThreadGroupLockImpl01Unfairish;

public class PopulationCutterShape {

    public PopulationCutterShape() {
        // TODO Auto-generated constructor stub
    }

    
    Population filter(Population population) {
//        for (Node node : originalNetwork.getNodes().values()) {
//            SimpleFeatureIterator iterator = collection.features();
//
//            while (iterator.hasNext()) {
//                MultiPolygon polygon = (MultiPolygon) iterator.next().getDefaultGeometry();
//
//                if (polygon.contains(factory.createPoint(new Coordinate(node.getCoord().getX(), node.getCoord().getY())))) {
//                    filteredNetwork.addNode(filteredNetwork.getFactory().createNode(node.getId(), node.getCoord()));
//                    break;
//                }
//            }
//        }
        return population;
    }
}
