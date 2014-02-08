/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.tnicolai.matsim4opus.accessibility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.utils.LeastCostPathTree;

import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelTimeCostCalculator;
import playground.tnicolai.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import playground.tnicolai.matsim4opus.utils.helperObjects.SpatialReferenceObject;
import playground.tnicolai.matsim4opus.utils.io.ReadFromUrbanSimModel;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class SpatialGridFillOrder {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		NetworkImpl network = createNetwork();
		AggregateObject2NearestNode[] dummyJobClusterArray = createWorkplaces(network);
		SpatialGridOld<Interpolation> grid = network2SpatialGrid(network);
		Map<Id, Double> resultMap = travelTimeAccessibility(network, dummyJobClusterArray);

		// compute derivation ...
		int rows = grid.getNumRows();
		int cols = grid.getNumCols(0);

		for(int r = 0; r < rows; r++){
			for(int c = 0; c < cols; c++){
				Interpolation interpol = grid.getValue(r, c);
				interpol.computeDerivation(resultMap);
			}
		}
		System.out.println("Finish");
	}

	/**
	 * @param network
	 */
	private static SpatialGridOld<Interpolation> network2SpatialGrid(final NetworkImpl network) {
		// The bounding box of all the given nodes as double[] = {minX, minY, maxX, maxY}
		double networkBoundingBox[] = NetworkUtils.getBoundingBox(network.getNodes().values());
		double xmin = networkBoundingBox[0];
		double xmax = networkBoundingBox[1];
		double ymin = networkBoundingBox[2];
		double ymax = networkBoundingBox[3];
		
		int res = 100;
		int counter = 0;
		GeometryFactory factory = new GeometryFactory();

		SpatialGridOld<Interpolation> grid = new SpatialGridOld<Interpolation>(xmin, ymin, xmax, ymax, res);

		Iterator<Node> nodeIterator = network.getNodes().values().iterator();


		// assign nodes to right square (only for Interpolation)
		for(;nodeIterator.hasNext();){
			Node node = nodeIterator.next();
			Point point = factory.createPoint( new Coordinate(node.getCoord().getX(), node.getCoord().getY()));

			if(grid.getValue( point ) == null)
				grid.setValue(new Interpolation(), point );

			Interpolation io = grid.getValue( point );
			io.addNode( node );
		}

		// determine centroid and nearest node
		for(double x = grid.getXmin(); x <= grid.getXmax(); x += res){
			for(double y = grid.getYmin(); y <= grid.getYmax(); y += res){

				Coord centroid = new CoordImpl(x + (res/2), y + (res/2));
				Node nearestNode = network.getNearestNode( centroid );
				Point point = factory.createPoint( new Coordinate(x, y));

				if(grid.getValue( point ) == null)
					grid.setValue(new Interpolation(), point );

				Interpolation io = grid.getValue( point );
				io.setID(counter++);
				io.setSquareCentroid(centroid, nearestNode.getId());
			}
		}

		return grid;
	}

	static NetworkImpl createNetwork() {

		/*
		 * (2)		(5)
		 * 	|		 |
		 * 	|		 |
		 * (1)------(4)------(7)
		 * 	|		 |
		 * 	|		 |
		 * (3)		(6)
		 */
		double freespeed = 2.7;	// this is m/s and corresponds to 50km/h
		double capacity = 500.;
		double numLanes = 1.;

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

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

	/**
	 * creating workplaces ...
	 */
	static AggregateObject2NearestNode[] createWorkplaces(final NetworkImpl network){

		System.out.println("Creating workplaces ...");

		/*
		 * JJ
		 * (2)		(5)
		 * 	|		 |
		 * 	|		 |		JJJJJ
		 * (1)------(4)------(7)
		 * 	|		 |
		 * 	|		 |
		 * (3)		(6)
		 *
		 * J = Jobs
		 */

		ReadFromUrbanSimModel dummyUrbanSimPracelModel = new ReadFromUrbanSimModel(2000);

		// create dummy jobs
		Id zoneID = new IdImpl(1);
		Id parcelID1 = new IdImpl(1);
		Id parcelID2 = new IdImpl(2);
		List<SpatialReferenceObject> dummyJobSampleList = new ArrayList<SpatialReferenceObject>();
		// 1 job at node 1
		dummyJobSampleList.add( new SpatialReferenceObject(new IdImpl(0), parcelID1, zoneID, new CoordImpl(0, 210)));
		// 5 jobs at node 7
		dummyJobSampleList.add( new SpatialReferenceObject(new IdImpl(2), parcelID2, zoneID, new CoordImpl(200, 110)));
		dummyJobSampleList.add( new SpatialReferenceObject(new IdImpl(3), parcelID2, zoneID, new CoordImpl(200, 110)));
		dummyJobSampleList.add( new SpatialReferenceObject(new IdImpl(4), parcelID2, zoneID, new CoordImpl(200, 110)));
		dummyJobSampleList.add( new SpatialReferenceObject(new IdImpl(5), parcelID2, zoneID, new CoordImpl(200, 110)));
		dummyJobSampleList.add( new SpatialReferenceObject(new IdImpl(6), parcelID2, zoneID, new CoordImpl(200, 110)));

		// aggregate jobs
		AggregateObject2NearestNode[] dummyJobClusterArray = dummyUrbanSimPracelModel.aggregateJobsWithSameParcelID(dummyJobSampleList);

		// add nearest network node
		for(int i = 0; i < dummyJobClusterArray.length; i++){

			assert ( dummyJobClusterArray[ i ].getCoordinate() != null );
			Node nearestNode = network.getNearestNode( dummyJobClusterArray[ i ].getCoordinate() );
			assert ( nearestNode != null );
			// add nearest node to job object
			dummyJobClusterArray[ i ].setNearestNode( nearestNode );
		}

		System.out.println("... done!");
		return dummyJobClusterArray;
	}


	/**
	 * computing travel time accessibility
	 * @param network
	 * @param dummyJobClusterArray
	 * @return
	 */
	static Map<Id, Double> travelTimeAccessibility(final NetworkImpl network,
													final AggregateObject2NearestNode[] dummyJobClusterArray){

		System.out.println("Computing travel time accessibility ...");

		double dummyCostFactor = 1.;
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		TravelTime ttc = new TravelTimeCalculator(network,60,30*3600, scenario.getConfig().travelTimeCalculator()).getLinkTravelTimes();
		// init least cost path tree computing shortest paths (according to travel times)
		LeastCostPathTree lcptTime = new LeastCostPathTree(ttc, new TravelTimeCostCalculator(ttc));

		// setting depature time not important here but necessary for LeastCostPathTree
		final double depatureTime = 8.*3600;
		// get a beta for accessibility computation
		double beta_per_hr = scenario.getConfig().planCalcScore().getTraveling_utils_hr() - scenario.getConfig().planCalcScore().getPerforming_utils_hr();
		double beta_per_min = beta_per_hr / 60.; // get utility per minute

		Iterator<Node> nodeIterator = network.getNodes().values().iterator();
		Map<Id, Double> resultMap = new HashMap<Id, Double>();

		while(nodeIterator.hasNext()){
			// set origin
			Node originNode = nodeIterator.next();
			Coord coord = originNode.getCoord();
			assert( coord != null );

			lcptTime.calculate(network, originNode, depatureTime);

			double accessibilityTravelTimes = 0.;

			// go through all jobs (nearest network node) and calculate workplace accessibility
			for ( int i = 0; i < dummyJobClusterArray.length; i++ ) {

				Node destinationNode = dummyJobClusterArray[i].getNearestNode();
				Id nodeID = destinationNode.getId();
				int jobCounter = dummyJobClusterArray[i].getNumberOfObjects();

				double arrivalTime = lcptTime.getTree().get( nodeID ).getTime();
				// with "dummyCostFactor=1" this is the same as: lcptTime.getTree().get( nodeID ).getCost() / 60
				double travelTime_min = (arrivalTime - depatureTime) / 60.;

				// sum travel times
				accessibilityTravelTimes += Math.exp( beta_per_min * travelTime_min ) * jobCounter;
			}

			resultMap.put( originNode.getId(), Math.log( accessibilityTravelTimes ) );
		}
		System.out.println("... done!");
		return resultMap;
	}


	static class Interpolation{

		private int id = -1;

		/** fields regarding square centroid (Layer1) */
		private Coord squareCentroid = null;
		private Id squareCentroidNodeID = null;
		private double squareCentroidAccessibilityValue = 0.;

		/** fields regarding square interpolation (Layer2) */
		private ArrayList<Id> squareInterpolationNodeIDList = null;
		private double interpolatedAccessibilityValue = 0.;

		/** fields regarding derivation */
		private double derivation = 0.;

		public void setSquareCentroid(final Coord centroidCoord, final Id nearestNodeID){
			this.squareCentroid = centroidCoord;
			this.squareCentroidNodeID = nearestNodeID;
		}

		public void addNode(final Node node){
			if(this.squareInterpolationNodeIDList == null)
				this.squareInterpolationNodeIDList = new ArrayList<Id>();
			this.squareInterpolationNodeIDList.add( node.getId() );
		}

		public void setID(final int id){
			this.id  = id;
		}

		public boolean computeDerivation(final Map<Id, Double> resultMap){

			assert(resultMap != null);

			// 1. get accessibility value for square centroid
			if( !resultMap.containsKey( this.squareCentroidNodeID ) )
				return false;
			this.squareCentroidAccessibilityValue = resultMap.get( this.squareCentroidNodeID );

			// 2. check if interpolation nodes are assigned
			if(this.squareInterpolationNodeIDList != null){

				for(int i = 0; i < this.squareInterpolationNodeIDList.size(); i++){
					Id nodeId = this.squareInterpolationNodeIDList.get( i );
					this.interpolatedAccessibilityValue =+ resultMap.get( nodeId );
				}
				this.interpolatedAccessibilityValue = (this.interpolatedAccessibilityValue / this.squareInterpolationNodeIDList.size());

				// 3. determine derivation
				this.derivation = Math.abs(this.interpolatedAccessibilityValue) - Math.abs(this.squareCentroidAccessibilityValue);
			}

			return true;

		}
	}
}
