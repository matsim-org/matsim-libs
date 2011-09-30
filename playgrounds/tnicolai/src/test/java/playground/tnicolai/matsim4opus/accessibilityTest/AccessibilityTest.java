package playground.tnicolai.matsim4opus.accessibilityTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.testcases.MatsimTestCase;

import playground.tnicolai.matsim4opus.gis.SpatialGrid;
import playground.tnicolai.matsim4opus.testCostCalculators.TravelTimeCostCalculatorTest;
import playground.tnicolai.matsim4opus.utils.helperObjects.JobClusterObject;
import playground.tnicolai.matsim4opus.utils.helperObjects.JobsObject;
import playground.tnicolai.matsim4opus.utils.io.ReadFromUrbansimParcelModel;
import playground.toronto.ttimematrix.LeastCostPathTree;

public class AccessibilityTest extends MatsimTestCase{
	
	private static final Logger log = Logger.getLogger(AccessibilityTest.class);
	
	ScenarioImpl scenario;
	double dummyCostFactor;
	double dummyTolerance = 0.;
	
	/**
	 * Testing workplace accessibility computation:
	 * The workplace accessibility at node 1 should be the lowest
	 * the highest should be at node 6
	 */
	@Test
	public void testAccessibilityComputation(){
		log.info("Testing computation of \"workplace\" accessibility ...");
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.dummyCostFactor = 1.;
		
		NetworkImpl network = createNetwork();
		JobClusterObject[] dummyJobClusterArray= createWorkplaces(network);
		
		Map<Id, Double> resultMap = travelTimeAccessibility(network, dummyJobClusterArray);
		
		Assert.assertTrue( evaluateResult(resultMap) );
		log.info("Done testing \"workplace\" accessibility computing.");
	}
	
	/**
	 * Testing derivation of ordinary workplace accessibility 
	 * computing compared with interpolated accessibility 
	 * measures via SpatialGrid's
	 */
	@Test
	public void testInterpolation(){
		log.info("Testing derivation of ordinary \"workplace\" accessibility computing compared with interpolated accessibility measures via SpatialGrid's...");
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.dummyCostFactor = 1.;
		this.dummyTolerance = 0.01;
		
		NetworkImpl network = createNetwork();
		JobClusterObject[] dummyJobClusterArray= createWorkplaces(network);
		
		SpatialGrid<InterpolationObjectV2> derivationGrid300 = constructDerivationGrid(network, dummyJobClusterArray, 300);
		SpatialGrid<InterpolationObjectV2> derivationGrid700 = constructDerivationGrid(network, dummyJobClusterArray, 700);
		
		Assert.assertTrue( evaluateSpatialGrid( derivationGrid300 ) && 
						   evaluateSpatialGrid(derivationGrid700) );
		log.info("Done testing derivation comparison via SpatialGrid's.");
	}

	/**
	 * @param network
	 * @param dummyJobClusterArray
	 */
	private SpatialGrid<InterpolationObjectV2> constructDerivationGrid(NetworkImpl network,
			JobClusterObject[] dummyJobClusterArray, double resolution) {
		Map<Id, Double> resultMap = travelTimeAccessibility(network, dummyJobClusterArray);
		
		NetworkBoundary nb = new NetworkBoundary(network);

		SpatialGrid<InterpolationObjectV2> grid = new SpatialGrid<AccessibilityTest.InterpolationObjectV2>(nb.getXMin(), nb.getYMin(), nb.getXMax(), nb.getYMax(), resolution);
		initSpatialGrid(grid, network, resultMap, nb, resolution);
		computeDerivation(grid, network, dummyJobClusterArray);

		return grid;
	} 
	
	private boolean initSpatialGrid(final SpatialGrid<InterpolationObjectV2> grid, final NetworkImpl network, 
									final Map<Id, Double> resultMap, final NetworkBoundary nb, final double resolution){
		
		List<Node> nodeList = new ArrayList<Node>(network.getNodes().values());
		
		int row = grid.getNumRows();
		int column = grid.getNumCols(0);
		
		double lowerLeftX = nb.getXMin();
		double lowerLeftY = nb.getYMin();
		
		for(int r = 0; r < row; r++){
			lowerLeftX = nb.getXMin();
			for(int c = 0; c < column; c++){
				
				if(grid.getValue(r, c) == null)
					grid.setValue(r, c, new InterpolationObjectV2(this.dummyTolerance));
				
				InterpolationObjectV2 iov2 = grid.getValue(r, c);
				// get square centroid for this part of grid
				iov2.setGridCentroid( new CoordImpl(lowerLeftX + (resolution / 2.) , lowerLeftY + (resolution / 2.)) );
				
				// init grid area for interpolation computation
				Coord lowerLeft = new CoordImpl(lowerLeftX, lowerLeftY);
				Coord upperRight = new CoordImpl(lowerLeftX + resolution, lowerLeftY + resolution);
				
				// check if a network node lies within this area, add its accessibility measure if so
				for(int i = 0; i < nodeList.size(); i++){
					Node node = nodeList.get(i);
					
					// node within this area
					if(node.getCoord().getX() < upperRight.getX() &&
							   node.getCoord().getX() >= lowerLeft.getX() &&
							   node.getCoord().getY() < upperRight.getY() &&
							   node.getCoord().getY() >= lowerLeft.getY()){
						
						// get accessibility measure
						double measure = resultMap.get( node.getId() );
						// add to interpolation accessibility sum ...
						iov2.addSubNodeAccessibility(measure);
						nodeList.remove( i );
						i--; // adjust index, since one node was removed
					}
				}
				lowerLeftX += resolution;
			}
			lowerLeftY += resolution;
		}
		return nodeList.size() == 0;
	}

	/**
	 * creating a test network
	 */
	private NetworkImpl createNetwork() {
		log.info("Creating road network ...");
		
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
		
		NetworkImpl network = this.scenario.getNetwork();
		
		// add nodes
		Node node1 = network.createAndAddNode(new IdImpl(1), this.scenario.createCoord(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl(2), this.scenario.createCoord(1000, 0));
		Node node3 = network.createAndAddNode(new IdImpl(3), this.scenario.createCoord(1100, 0));
		Node node4 = network.createAndAddNode(new IdImpl(4), this.scenario.createCoord(1000, 100));
		Node node5 = network.createAndAddNode(new IdImpl(5), this.scenario.createCoord(1200, 100));
		Node node6 = network.createAndAddNode(new IdImpl(6), this.scenario.createCoord(1200, -100));
		Node node7 = network.createAndAddNode(new IdImpl(7), this.scenario.createCoord(1000, 100));
		
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
		
		log.info("... done!");
		return network;
	}
	
	/**
	 * creating workplaces ...
	 */
	private JobClusterObject[] createWorkplaces(NetworkImpl network){
		
		log.info("Creating workplaces ...");
		
		ReadFromUrbansimParcelModel dummyUrbanSimPracelModel = new ReadFromUrbansimParcelModel(2000);
		
		// create dummy jobs
		Id zoneID = new IdImpl(1);
		List<JobsObject> dummyJobSampleList = new ArrayList<JobsObject>();
		// 1 job at node 1
		dummyJobSampleList.add( new JobsObject(new IdImpl(0), new IdImpl(0), zoneID, new CoordImpl(10, 10)));
		// 2 jobs at node 2
		dummyJobSampleList.add( new JobsObject(new IdImpl(1), new IdImpl(1), zoneID, new CoordImpl(990, 10)));
		// 1 job at node 4
		dummyJobSampleList.add( new JobsObject(new IdImpl(2), new IdImpl(2), zoneID, new CoordImpl(1000, 110)));
		// 1 job at node 5
		dummyJobSampleList.add( new JobsObject(new IdImpl(3), new IdImpl(3), zoneID, new CoordImpl(1200, 110)));
		// 4 jobs at node 6
		dummyJobSampleList.add( new JobsObject(new IdImpl(4), new IdImpl(4), zoneID, new CoordImpl(1190, -90)));
		dummyJobSampleList.add( new JobsObject(new IdImpl(5), new IdImpl(4), zoneID, new CoordImpl(1210, -90)));
		dummyJobSampleList.add( new JobsObject(new IdImpl(6), new IdImpl(4), zoneID, new CoordImpl(1190, -110)));
		dummyJobSampleList.add( new JobsObject(new IdImpl(7), new IdImpl(4), zoneID, new CoordImpl(1210, -110)));
		// 1 job at node 7
		dummyJobSampleList.add( new JobsObject(new IdImpl(8), new IdImpl(5), zoneID, new CoordImpl(1000, -110)));
		
		// aggregate jobs
		JobClusterObject[] dummyJobClusterArray = dummyUrbanSimPracelModel.aggregateJobsWithSameParcelID(dummyJobSampleList);
		
		// add nearest network node
		for(int i = 0; i < dummyJobClusterArray.length; i++){
			
			assert ( dummyJobClusterArray[ i ].getCoordinate() != null );
			Node nearestNode = network.getNearestNode( dummyJobClusterArray[ i ].getCoordinate() );
			assert ( nearestNode != null );
			// add nearest node to job object
			dummyJobClusterArray[ i ].addNearestNode( nearestNode );
		}
		
		log.info("... done!");
		return dummyJobClusterArray;
	}
	
	/**
	 * computing travel time accessibility
	 * @param network
	 * @param dummyJobClusterArray
	 * @return
	 */
	private Map<Id, Double> travelTimeAccessibility(final NetworkImpl network, 
													final JobClusterObject[] dummyJobClusterArray){
		
		log.info("Computing travel time accessibility ...");
		
		TravelTime ttc = new TravelTimeCalculator(this.scenario.getNetwork(),60,30*3600, scenario.getConfig().travelTimeCalculator());
		// init least cost path tree computing shortest paths (according to travel times)
		LeastCostPathTree lcptTime = new LeastCostPathTree(ttc, new TravelTimeCostCalculatorTest(ttc, scenario.getConfig().planCalcScore(), dummyCostFactor));
		
		// setting depature time not important here but necessary for LeastCostPathTree
		double depatureTime = 8.*3600;
		lcptTime.setDepartureTime(depatureTime);
		// get a beta for accessibility computation
		double beta_per_hr = this.scenario.getConfig().planCalcScore().getTraveling_utils_hr() - this.scenario.getConfig().planCalcScore().getPerforming_utils_hr();
		double beta_per_min = beta_per_hr / 60.; // get utility per minute
		
		Iterator<Node> nodeIterator = network.getNodes().values().iterator();
		Map<Id, Double> resultMap = new HashMap<Id, Double>();
		
		while(nodeIterator.hasNext()){
			// set origin
			Node originNode = nodeIterator.next();
			Coord coord = originNode.getCoord();
			assert( coord != null );
			
			lcptTime.setOrigin(originNode);
			lcptTime.run(network);
			
			double accessibilityTravelTimes = 0.;
			
			// go through all jobs (nearest network node) and calculate workplace accessibility
			for ( int i = 0; i < dummyJobClusterArray.length; i++ ) {
				
				Node destinationNode = dummyJobClusterArray[i].getNearestNode();
				Id nodeID = destinationNode.getId();
				int jobCounter = dummyJobClusterArray[i].getNumberOfJobs();
				
				double arrivalTime = lcptTime.getTree().get( nodeID ).getTime();
				// with "dummyCostFactor=1" this is the same as: lcptTime.getTree().get( nodeID ).getCost() / 60
				double travelTime_min = (arrivalTime - depatureTime) / 60.; 
				
				// sum travel times
				accessibilityTravelTimes += Math.exp( beta_per_min * travelTime_min ) * jobCounter;
			}
			
			resultMap.put( originNode.getId(), Math.log( accessibilityTravelTimes ) );
		}
		log.info("... done!");
		return resultMap;
	}
	
	/**
	 * Computing "workplace accessibility for each square centroid in spatial gird. 
	 * The spatial gird already contains accessibility measured for each network node.
	 * These measures are 
	 * @param grid
	 * @param network
	 * @param dummyJobClusterArray
	 */
	private void computeDerivation(final SpatialGrid<InterpolationObjectV2> grid,
									final NetworkImpl network, 
									final JobClusterObject[] dummyJobClusterArray){
		log.info("Computing travel time accessibility for each square ...");
		
		TravelTime ttc = new TravelTimeCalculator(this.scenario.getNetwork(),60,30*3600, scenario.getConfig().travelTimeCalculator());
		// init least cost path tree computing shortest paths (according to travel times)
		LeastCostPathTree lcptTime = new LeastCostPathTree(ttc, new TravelTimeCostCalculatorTest(ttc, scenario.getConfig().planCalcScore(), dummyCostFactor));
		
		// setting depature time not important here but necessary for LeastCostPathTree
		double depatureTime = 8.*3600;
		lcptTime.setDepartureTime(depatureTime);
		// get a beta for accessibility computation
		double beta_per_hr = this.scenario.getConfig().planCalcScore().getTraveling_utils_hr() - this.scenario.getConfig().planCalcScore().getPerforming_utils_hr();
		double beta_per_min = beta_per_hr / 60.; // get utility per minute
		
		int row = grid.getNumRows();
		int column = grid.getNumCols(0);
		
		// running though the spatial gird computing accessibility for each square centroid 
		// and determining the derivation with interpolated measures (from previous accessibility run)
		
		for(int r = 0; r < row; r++){
			for(int c = 0; c < column; c++){
				InterpolationObjectV2 iov2 = grid.getValue(r, c);
				
				Node originNode = network.getNearestNode( iov2.gridCentroid );
				assert( originNode != null );
				
				lcptTime.setOrigin(originNode);
				lcptTime.run(network);
				
				double ttAccessibility = 0.;
				
				// go through all jobs (nearest network node) and calculate workplace accessibility
				for ( int i = 0; i < dummyJobClusterArray.length; i++ ) {
					
					Node destinationNode = dummyJobClusterArray[i].getNearestNode();
					Id nodeID = destinationNode.getId();
					int jobCounter = dummyJobClusterArray[i].getNumberOfJobs();
					
					double arrivalTime = lcptTime.getTree().get( nodeID ).getTime();
					// with "dummyCostFactor=1" this is the same as: lcptTime.getTree().get( nodeID ).getCost() / 60
					double travelTime_min = (arrivalTime - depatureTime) / 60.; 
					
					// sum travel times
					ttAccessibility += Math.exp( beta_per_min * travelTime_min ) * jobCounter;
				}
				iov2.setGridCentroidAccessibility( Math.log(ttAccessibility) );
				iov2.computeAndGetDerivation();
			}
		}
		log.info("... done!");
	}
	
	/**
	 * The workplace accessibility at node 1 should be the lowest and 
	 * the highest at node 6
	 * @param resultMap
	 * @return
	 */
	private boolean evaluateResult(Map<Id, Double> resultMap){
		
		log.info("Evaluating accessibility results ...");
		
		Id nodeIDMinimumValue = null;
		double minimumValue = Double.MIN_VALUE;
		Id nodeIDMaximumValue = null;
		double maximumValue = Double.MAX_VALUE;
		
		Iterator<Id> keyIterator = resultMap.keySet().iterator();
		
		while(keyIterator.hasNext()){
			
			Id key = keyIterator.next();
			double value = resultMap.get( key );
			
			if(nodeIDMaximumValue == null && nodeIDMaximumValue == null){
				nodeIDMaximumValue = key;
				nodeIDMinimumValue = key;
				minimumValue = value;
				maximumValue = value;
			}
			else{
				if(value < minimumValue){
					nodeIDMinimumValue = key;
					minimumValue = value;
				}
				else if(value >= maximumValue){
					nodeIDMaximumValue = key;
					maximumValue = value;
				}
			}
		}
		
		log.info("... done!");
		
		return (nodeIDMinimumValue.compareTo(new IdImpl(1)) == 0 &&
				nodeIDMaximumValue.compareTo(new IdImpl(6)) == 0);
	}
	
	/**
	 * 
	 * @param grid
	 * @return
	 */
	private boolean evaluateSpatialGrid(SpatialGrid<InterpolationObjectV2> grid){
		boolean result = true;
		
		if(grid.getResolution() == 300){
			// just checking first row (row = 0) and 
			// ignoring other rows, since those contain no 
			// interpolation values at all
			InterpolationObjectV2 square1 = grid.getValue(0, 0);
			InterpolationObjectV2 square2 = grid.getValue(0, 1);
			InterpolationObjectV2 square3 = grid.getValue(0, 2);
			InterpolationObjectV2 square4 = grid.getValue(0, 3);
			InterpolationObjectV2 square5 = grid.getValue(0, 4);
			
			result &= square1.isInterpolationAvailable() && square1.isWithinTolerance();	// here the interpolation is equal with centroid measure (only one node there)
			result &= !square2.isInterpolationAvailable();									// no interpolation, since no nodes located in this area
			result &= !square3.isInterpolationAvailable();									// no interpolation, since no nodes located in this area
			result &= square4.isInterpolationAvailable() && !square4.isWithinTolerance(); 	// interpolation available but given tolerance exceeded
			result &= square5.isInterpolationAvailable() && square5.isWithinTolerance();	// interpolation available and given tolerance fits
			
			return result;
		}
		else if(grid.getResolution() == 700){
			// just checking first row (row = 0) and 
			// ignoring other rows, since those contain no 
			// interpolation values at all
			InterpolationObjectV2 square1 = grid.getValue(0, 0);
			InterpolationObjectV2 square2 = grid.getValue(0, 1);
			InterpolationObjectV2 square3 = grid.getValue(0, 2);
			
			result &= square1.isInterpolationAvailable() && square1.isWithinTolerance();
			result &= square2.isInterpolationAvailable() && square2.isWithinTolerance();
			result &= !square3.isInterpolationAvailable();
			
			return result;
		}
		return false;
	}
	
	/**
	 * helper class
	 * @author thomas
	 *
	 */
	class NetworkBoundary{
		private double xmin = Double.MAX_VALUE;
		private double xmax = Double.MIN_VALUE;
		private double ymin = Double.MAX_VALUE;
		private double ymax = Double.MIN_VALUE;
		
		public NetworkBoundary(final NetworkImpl network){
			if(network != null)
				init(network.getNodes().values().iterator());
		}
		
		private void init(Iterator<Node> nodeIterator){
			// get the network extend
			while(nodeIterator.hasNext()){
				Node node = nodeIterator.next();
				// check x coord
				if(node.getCoord().getX() > xmax)
					xmax = node.getCoord().getX();
				else if(node.getCoord().getX() < xmin)
					xmin = node.getCoord().getX();
				// check y coord
				if(node.getCoord().getY() > ymax)
					ymax = node.getCoord().getY();
				else if(node.getCoord().getY() < ymin)
					ymin = node.getCoord().getY();
			}
		}
		
		double getXMax(){
			return this.xmax;
		}
		double getXMin(){
			return this.xmin;
		}
		double getYMax(){
			return this.ymax;
		}
		double getYMin(){
			return this.ymin;
		}
	}
	
	/**
	 * helper class
	 * @author thomas
	 *
	 */
	class InterpolationObjectV2{

		private Coord gridCentroid = null;
		private double gridCentroidAccessibility = 0.;
		private boolean setGridCentroid = Boolean.FALSE;
		
		private double sumSubNodesAccessibilities = 0.;
		private long subNodeCounter = 0;
		private double interpolatedSubNodeAccessibility = 0.;
		
		private double derivationCentroidVsSubNodes = Double.MIN_VALUE;
		private double targetTolerance = 0.;
		private boolean withinTolerance = Boolean.FALSE;
		
		public InterpolationObjectV2(double tolerance){
			this.targetTolerance = tolerance;
		}
		
		void setGridCentroid(Coord centroid){
			this.gridCentroid = centroid;
		}
		
		Coord getGridCentroid(){
			return this.gridCentroid;
		}
		
		void setGridCentroidAccessibility(double measure){
			this.gridCentroidAccessibility = measure;
			this.setGridCentroid = true;
		}
		
		void addSubNodeAccessibility(double measure){
			this.sumSubNodesAccessibilities += measure;
			this.subNodeCounter++;
		}
		
		boolean computeAndGetDerivation(){
			if(setGridCentroid && isInterpolationAvailable()){
				// compute interpolation
				this.interpolatedSubNodeAccessibility = this.sumSubNodesAccessibilities / this.subNodeCounter;
				// compute derivation
				this.derivationCentroidVsSubNodes = this.interpolatedSubNodeAccessibility - this.gridCentroidAccessibility;
				if (Math.abs( this.derivationCentroidVsSubNodes ) <= targetTolerance )
					this.withinTolerance = Boolean.TRUE;
				return true;
			}
			return false;
		}
		
		boolean isInterpolationAvailable(){
			return (this.subNodeCounter > 0);
		}
		
		boolean isGridCentroidAccessibilityAvailable(){
			return this.setGridCentroid;
		}
		
		boolean isWithinTolerance(){
			return this.withinTolerance;
		}
		
		double getDerivation(){
			return this.derivationCentroidVsSubNodes;
		}
		
		double getGridCentroidAccessibility(){
			return this.gridCentroidAccessibility;
		}
	}
}
