package playground.tnicolai.matsim4opus.accessibility;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.matsim4opus.gis.SpatialGrid;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.config.ConfigUtils;

import playground.tnicolai.matsim4opus.utils.UtilityCollection;
import playground.tnicolai.matsim4opus.utils.helperObjects.NetworkBoundary;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class SpatialGridFillOrder {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		network2SpatialGrid(createNetworkNr2());
		network2SpatialGrid(createNetwork());
	}

	/**
	 * @param network
	 */
	private static void network2SpatialGrid(NetworkImpl network) {
		NetworkBoundary nb = UtilityCollection.getNetworkBoundary(network);
		
		double xmin = nb.getMinX();
		double xmax = nb.getMaxX();
		double ymin = nb.getMinY();
		double ymax = nb.getMaxY();
		int res = 100;
		int counter = 0;
		GeometryFactory factory = new GeometryFactory();
		
		SpatialGrid<Interpolation> grid = new SpatialGrid<Interpolation>(xmin, ymin, xmax, ymax, res);

		Iterator<Node> nodeIterator = network.getNodes().values().iterator();
		
		// assign nodes to right square
		for(;nodeIterator.hasNext();){
			Node node = nodeIterator.next();
			Coord coord = node.getCoord();
			
			if(grid.getValue(factory.createPoint( new Coordinate(coord.getX(), coord.getY()))) == null)
				grid.setValue(new Interpolation(), factory.createPoint( new Coordinate(coord.getX(), coord.getY())) );
			
			Interpolation io = grid.getValue(factory.createPoint( new Coordinate(coord.getX(), coord.getY())));
			io.addNode( node );			
		}
		// determine centroid and nearest node
		for(double x = grid.getXmin(); x <= grid.getXmax(); x += res){
			for(double y = grid.getYmin(); y <= grid.getYmax(); y += res){
				
				Coord centroid = new CoordImpl(x + (res/2), y + (res/2));
				Node nearestNode = network.getNearestNode( centroid );
				
				if(grid.getValue(factory.createPoint( new Coordinate(x, y))) == null)
					grid.setValue(new Interpolation(), factory.createPoint(new Coordinate(x, y)) );
				
				Interpolation io = grid.getValue(factory.createPoint(new Coordinate(x, y)));
				io.setID(counter++);
				io.setSquareCentroid(centroid, nearestNode);
			}
		}
		
		System.gc();
	}
	
	/**
	 * creating a test network
	 */
	private static NetworkImpl createNetwork() {
		System.out.println("Creating road network ...");
		
		/*
		 * 							   (4)-------------(5)
		 * 								|				|
		 * 								|				|
		 * (1)-------------------------(2)-----(3)		|
		 * 								|				|
		 * 								|				|
		 * 							   (7)-------------(6)
		 */
		
		double freespeed = 13.8888889;	// this is m/s and corresponds to 50km/h
		double capacity = 500.;
		double numLanes = 1.;
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		NetworkImpl network = scenario.getNetwork();
		
		// add nodes
		Node node1 = network.createAndAddNode(new IdImpl(1), scenario.createCoord(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl(2), scenario.createCoord(1000, 0));
		Node node3 = network.createAndAddNode(new IdImpl(3), scenario.createCoord(1100, 0));
		Node node4 = network.createAndAddNode(new IdImpl(4), scenario.createCoord(1000, 100));
		Node node5 = network.createAndAddNode(new IdImpl(5), scenario.createCoord(1200, 100));
		Node node6 = network.createAndAddNode(new IdImpl(6), scenario.createCoord(1200, -100));
		Node node7 = network.createAndAddNode(new IdImpl(7), scenario.createCoord(1000, -100));
		
		// add links (bi-directional)
		network.createAndAddLink(new IdImpl(1), node1, node2, 1000, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(2), node2, node1, 1000, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(3), node2, node3, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(4), node3, node2, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(5), node2, node4, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(6), node4, node2, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(7), node4, node5, 200, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(8), node5, node4, 200, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(9), node5, node6, 200, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(10), node6, node5, 200, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(11), node6, node7, 200, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(12), node7, node6, 200, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(13), node7, node2, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(14), node2, node7, 100, freespeed, capacity, numLanes);
		
		System.out.println("... done!");
		return network;
	}
	
	private static NetworkImpl createNetworkNr2() {
		
		/*
		 * (2)		(5)			
		 * 	|		 |			
		 * 	|		 |		  	
		 * (1)------(4)------(7)
		 * 	|		 |	
		 * 	|		 |	
		 * (3)		(6)	
		 */
		double freespeed = 13.8888889;	// this is m/s and corresponds to 50km/h
		double capacity = 500.;
		double numLanes = 1.;
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		NetworkImpl network = scenario.getNetwork();
		
		// add nodes
		Node node1 = network.createAndAddNode(new IdImpl(1), scenario.createCoord(0, 100));
		Node node2 = network.createAndAddNode(new IdImpl(2), scenario.createCoord(0, 200));
		Node node3 = network.createAndAddNode(new IdImpl(3), scenario.createCoord(0, 0));
		Node node4 = network.createAndAddNode(new IdImpl(4), scenario.createCoord(100, 100));
		Node node5 = network.createAndAddNode(new IdImpl(5), scenario.createCoord(100, 200));
		Node node6 = network.createAndAddNode(new IdImpl(6), scenario.createCoord(100, 0));
		Node node7 = network.createAndAddNode(new IdImpl(7), scenario.createCoord(200, 100));
		
		// add links (bi-directional)
		network.createAndAddLink(new IdImpl(1), node1, node2, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(2), node2, node1, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(3), node1, node3, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(4), node3, node1, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(5), node1, node4, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(6), node4, node1, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(7), node4, node5, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(8), node5, node4, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(9), node4, node6, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(10), node6, node4, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(11), node4, node7, 100, freespeed, capacity, numLanes);
		network.createAndAddLink(new IdImpl(12), node7, node4, 100, freespeed, capacity, numLanes);
		
		System.out.println("... done!");
		return network;
	}
	
	static class Interpolation{
		
		private int id = -1;
		
		/** fields regarding square centroid (Layer1) */
		private Coord squareCentroid = null;
		private Node squareCentroidNode = null;
		
		/** fields regarding square interpolation (Layer2) */
		private ArrayList<Node> squareInterpolationNodeList = null;
		
		public void setSquareCentroid(Coord centroidCoord, Node nearestNode){
			this.squareCentroid = centroidCoord;
			this.squareCentroidNode = nearestNode;
		}
		
		public void addNode(Node node){
			if(this.squareInterpolationNodeList == null)
				this.squareInterpolationNodeList = new ArrayList<Node>();
			this.squareInterpolationNodeList.add( node );
		}
		
		public void setID(int id){
			this.id  = id;
		}
	}
}
