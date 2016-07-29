package playground.vaadinexample;


import com.vividsolutions.jts.triangulate.ConformingDelaunayTriangulator;
import com.vividsolutions.jts.triangulate.ConstraintVertex;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdgeSubdivision;
import com.vividsolutions.jts.triangulate.quadedge.Vertex;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.gis.ContourBuilder;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class ContourService {

	private final LeastCostPathTree leastCostPathTree;
	private final Network network;
	private final ConformingDelaunayTriangulator conformingDelaunayTriangulator;

	ContourService(Network network, Node startNode) {
		this.network = network;
		FreespeedTravelTimeAndDisutility freeSpeedTravelTime = new FreespeedTravelTimeAndDisutility(0.0, 6.0, 0.0);
		leastCostPathTree = new LeastCostPathTree(freeSpeedTravelTime, freeSpeedTravelTime);
		leastCostPathTree.calculate(network, startNode, 0.0);
		Collection<ConstraintVertex> sites = new ArrayList<>();
		for (Map.Entry<Id<Node>, LeastCostPathTree.NodeData> entry : leastCostPathTree.getTree().entrySet()) {
			Node node = network.getNodes().get(entry.getKey());
			ConstraintVertex vertex = new ConstraintVertex(MGC.coord2Coordinate(node.getCoord()));
			vertex.setConstraint(true);
			vertex.setConstraint(node);
			vertex.setZ(entry.getValue().getTime());
			sites.add(vertex);
		}
		conformingDelaunayTriangulator = new ConformingDelaunayTriangulator(sites, 0.0);
		conformingDelaunayTriangulator.setConstraints(new ArrayList(), new ArrayList());
		conformingDelaunayTriangulator.formInitialDelaunay();
	}

	public Collection<NodeWithCost> getNodes() {
		Collection<NodeWithCost> collection = new ArrayList<NodeWithCost>();
		for (Map.Entry<Id<Node>, LeastCostPathTree.NodeData> entry : leastCostPathTree.getTree().entrySet()) {
			LeastCostPathTree.NodeData nodeData = entry.getValue();
			NodeWithCost nwc = new NodeWithCost();
			nwc.setGeometry(MGC.coord2Point(network.getNodes().get(entry.getKey()).getCoord()));
			nwc.setTime(nodeData.getTime());
			nwc.setCost(nodeData.getCost());
			collection.add(nwc);
		}
		return collection;
	}

	public Contour getContour(double seconds, String color) {
		QuadEdgeSubdivision subdivision = conformingDelaunayTriangulator.getSubdivision();
		for (Vertex vertex : (Collection<Vertex>) subdivision.getVertices(true)) {
			if (subdivision.isFrameVertex(vertex)) {
				vertex.setZ(Double.MAX_VALUE);
			}
		}
		ContourBuilder contourBuilder = new ContourBuilder(subdivision);
		Contour contour = new Contour();
		contour.setZ(seconds);
		contour.setColor(color);
		contour.setGeometry(contourBuilder.computeIsoline(seconds));
		return contour;
	}

}
