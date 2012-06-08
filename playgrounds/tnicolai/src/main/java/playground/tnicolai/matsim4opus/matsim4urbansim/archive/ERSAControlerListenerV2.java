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

package playground.tnicolai.matsim4opus.matsim4urbansim.archive;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.utils.LeastCostPathTree;

import playground.tnicolai.matsim4opus.constants.InternalConstants;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelWalkTimeCostCalculator;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import playground.tnicolai.matsim4opus.utils.helperObjects.SquareLayer;
import playground.tnicolai.matsim4opus.utils.io.writer.AnalysisWorkplaceCSVWriter;
import playground.tnicolai.matsim4opus.utils.misc.ProgressBar;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class ERSAControlerListenerV2 implements ShutdownListener{

	private static final Logger log = Logger.getLogger(ERSAControlerListenerV2.class);

	private final AggregateObject2NearestNode[] aggregatedJobArray;
	private final int resolutionFeet;
	private final int resolutionMeter;

	private SpatialGridOld<SquareLayer> travelTimeAccessibilityGrid;
	private SpatialGridOld<SquareLayer> travelCostAccessibilityGrid;
	private SpatialGridOld<SquareLayer> travelDistanceAccessibilityGrid;

	private final Map<Id, Double>travelTimeAccessibilityMap;
	private final Map<Id, Double>travelCostAccessibilityMap;
	private final Map<Id, Double>travelDistanceAccessibilityMap;

	private final Benchmark benchmark;

	/**
	 * constructor
	 * @param aggregatedJobArray
	 */
	public ERSAControlerListenerV2(final AggregateObject2NearestNode[] aggregatedJobArray, final int resolutionFeet, final int resolutionMeter, final Benchmark benchmark){

		log.info("Initializing ERSAControlerListenerV2 ...");

		assert(aggregatedJobArray != null);
		this.aggregatedJobArray = aggregatedJobArray;
		assert(resolutionFeet > 0);
		this.resolutionFeet = resolutionFeet;
		assert(resolutionMeter > 0);
		this.resolutionMeter = resolutionMeter;
		assert(benchmark != null);
		this.benchmark = benchmark;

		this.travelCostAccessibilityMap = new HashMap<Id, Double>();
		this.travelTimeAccessibilityMap = new HashMap<Id, Double>();
		this.travelDistanceAccessibilityMap = new HashMap<Id, Double>();

//		CellBasedAccessibilityCSVWriter.initAccessiblityWriter( Constants.MATSIM_4_OPUS_TEMP +
//													   "accessibility_indicators_v2" +
//													   Constants.FILE_TYPE_CSV);

		log.info(".. done initializing ERSAControlerListenerV2!");
	}


	/**
	 * calculating accessibility indicators
	 */
	@Override
	public void notifyShutdown(final ShutdownEvent event) {
		log.info("Entering notifyShutdown ..." );

		// get the controller and scenario
		Controler controler = event.getControler();
		Scenario sc = controler.getScenario();
		NetworkImpl network = (NetworkImpl) controler.getNetwork();
		initSpatialGirds(network);

		int benchmarkID = this.benchmark.addMeasure("1-Point accessibility computation");

		// init LeastCostPathTree in order to calculate travel times and travel costs
		TravelTime ttc = controler.getTravelTimeCalculator();
		// this calculates the workplace accessibility travel times
		LeastCostPathTree lcptTravelTime = new LeastCostPathTree( ttc, new TravelTimeAndDistanceBasedTravelDisutility(ttc, controler.getConfig().planCalcScore()) );
		// this calculates the workplace accessibility distances
		LeastCostPathTree lcptTravelDistance = new LeastCostPathTree( ttc, new TravelWalkTimeCostCalculator(sc.getConfig().plansCalcRoute().getWalkSpeed()) ); // tnicolai: this is experimental, check with Kai, sep'2011

		double depatureTime = 8.*3600;	// tnicolai: make configurable
		double beta_per_hr = sc.getConfig().planCalcScore().getTraveling_utils_hr() - sc.getConfig().planCalcScore().getPerforming_utils_hr();
		double beta_per_min = beta_per_hr / 60.; // get utility per minute

		try{
			log.info("Computing and writing accessibility measures ..." );
			Iterator<Node> startNodeIterator = network.getNodes().values().iterator();
			int numberOfStartNodes = network.getNodes().values().size();
			log.info("Calculating " + numberOfStartNodes + " starting points ...");

			ProgressBar bar = new ProgressBar( numberOfStartNodes );

			// iterates through all starting points (fromZone) and calculates their workplace accessibility
			while( startNodeIterator.hasNext() ){

				bar.update();

				Node originNode = startNodeIterator.next();
				assert(originNode != null);
				// run dijkstra on network
				lcptTravelTime.calculate(network, originNode, depatureTime);
				lcptTravelDistance.calculate(network, originNode, depatureTime);

				// from here: accessibility computation for current starting point ("originNode")

				double accessibilityTravelTimes = 0.;
				double accessibilityTravelTimeCosts = 0.;
				double accessibilityTravelDistanceCosts = 0.;

				// go through all jobs (nearest network node) and calculate workplace accessibility
				for ( int i = 0; i < this.aggregatedJobArray.length; i++ ) {

					Node destinationNode = this.aggregatedJobArray[i].getNearestNode();
					Id nodeID = destinationNode.getId();
					int jobWeight = this.aggregatedJobArray[i].getNumberOfObjects();

					double arrivalTime = lcptTravelTime.getTree().get( nodeID ).getTime();

					// travel times in minutes
					double travelTime_min = (arrivalTime - depatureTime) / 60.;
					// travel costs in utils
					double travelCosts = lcptTravelTime.getTree().get( nodeID ).getCost();
					// travel distance by car in meter
					double travelDistance_meter = lcptTravelDistance.getTree().get( nodeID ).getCost();

					// sum travel times
					accessibilityTravelTimes += Math.exp( beta_per_min * travelTime_min ) * jobWeight;
					// sum travel costs  (mention the beta)
					accessibilityTravelTimeCosts += Math.exp( beta_per_min * travelCosts ) * jobWeight; // tnicolai: find another beta for travel costs
					// sum travel distances  (mention the beta)
					accessibilityTravelDistanceCosts += Math.exp( beta_per_min * travelDistance_meter ) * jobWeight; // tnicolai: find another beta for travel distance
				}

				double travelCostLogSum = Math.log( accessibilityTravelTimeCosts );
				double travelDistanceLogSum = Math.log( accessibilityTravelDistanceCosts );
				double travelTimeLogSum = Math.log( accessibilityTravelTimes );

				// assigning each accessibility value with current node id (as key) to corresponding hash map
				this.travelCostAccessibilityMap.put(originNode.getId(), travelCostLogSum );
				this.travelDistanceAccessibilityMap.put(originNode.getId(), travelDistanceLogSum );
				this.travelTimeAccessibilityMap.put(originNode.getId(), travelTimeLogSum );

				// using hash maps to dump out log sum of current node in csv format
//				CellBasedAccessibilityCSVWriter.write(originNode, travelTimeLogSum, travelCostLogSum, travelDistanceLogSum);
			}
			this.benchmark.stoppMeasurement(benchmarkID);
			log.info("Accessibility computation with " + numberOfStartNodes
					+ " starting points (origins) and "
					+ this.aggregatedJobArray.length
					+ " destinations (workplaces) took "
					+ this.benchmark.getDurationInSeconds(benchmarkID)
					+ " seconds ("
					+ this.benchmark.getDurationInSeconds(benchmarkID) / 60.
					+ " minutes).");
		}
		catch(Exception e){ e.printStackTrace(); }
		finally{
			dumpData();
		}
	}

	private void dumpData(){
//		writeSpatialGridTables();
		SpatialGridTableWriterERSA_V2.writeTableAndCSV(this.travelTimeAccessibilityGrid, this.travelCostAccessibilityGrid, this.travelDistanceAccessibilityGrid, this.travelTimeAccessibilityMap, this.travelCostAccessibilityMap, this.travelDistanceAccessibilityMap, this.resolutionMeter);
		SpatialGrid2KMZWriter.writeKMZFiles(this.travelTimeAccessibilityGrid, this.travelCostAccessibilityGrid, this.travelDistanceAccessibilityGrid);
		AnalysisWorkplaceCSVWriter.writeAggregatedWorkplaceData2CSV( this.aggregatedJobArray );
		// accessibility measure were written while computing, just closing file now .
//		CellBasedAccessibilityCSVWriter.close();
	}

	private void initSpatialGirds(final NetworkImpl network){

		log.info("Initializing Spatial Grids ...");
		// The bounding box of all the given nodes as double[] = {minX, minY, maxX, maxY}
		double networkBoundingBox[] = NetworkUtils.getBoundingBox(network.getNodes().values());

		double xmin = networkBoundingBox[0];
		double xmax = networkBoundingBox[1];
		double ymin = networkBoundingBox[2];
		double ymax = networkBoundingBox[3];

		log.info("Detected network size: MinX=" + xmin + " MinY=" + ymin + " MaxX=" + xmax + " MaxY=" + ymax );

		// creating spatial grids, one for each accessibility measure ...
		this.travelTimeAccessibilityGrid = new SpatialGridOld<SquareLayer>(xmin, ymin, xmax, ymax, this.resolutionFeet);
		this.travelCostAccessibilityGrid = new SpatialGridOld<SquareLayer>(xmin, ymin, xmax, ymax, this.resolutionFeet);
		this.travelDistanceAccessibilityGrid = new SpatialGridOld<SquareLayer>(xmin, ymin, xmax, ymax, this.resolutionFeet);

		GeometryFactory factory = new GeometryFactory();

		// create/init squares
		int rows = this.travelTimeAccessibilityGrid.getNumRows();
		int cols = this.travelTimeAccessibilityGrid.getNumCols(0);
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				this.travelTimeAccessibilityGrid.setValue(r, c, new SquareLayer());
				this.travelCostAccessibilityGrid.setValue(r, c, new SquareLayer());
				this.travelDistanceAccessibilityGrid.setValue(r, c, new SquareLayer());
			}
		}

		log.warn("Spatial Reference ID (SRID) is set for WASHINGTON_NORTH: " + InternalConstants.SRID_WASHINGTON_NORTH);
		// determine square + square centroid + nearest node
		for(double x = xmin; x <= xmax; x += this.resolutionFeet){
			for(double y = ymin; y <= ymax; y += this.resolutionFeet){

				// point points to lower left corner of a square
				Point point = factory.createPoint( new Coordinate(x, y));
				// create a square (for google maps kmz writer)
				Coordinate[] coords = new Coordinate[5];
				coords[0] = point.getCoordinate();
				coords[1] = new Coordinate(x, y + this.resolutionFeet);
				coords[2] = new Coordinate(x + this.resolutionFeet, y + this.resolutionFeet);
				coords[3] = new Coordinate(x + this.resolutionFeet, y);
				coords[4] = point.getCoordinate();
				// Linear Ring defines an artificial zone
				LinearRing linearRing = factory.createLinearRing(coords);
				Polygon polygon = factory.createPolygon(linearRing, null);
				polygon.setSRID( InternalConstants.SRID_WASHINGTON_NORTH );

				// centroid determines the center of a square
				Coord squareCentroid = new CoordImpl(x + (this.resolutionFeet/2), y + (this.resolutionFeet/2));
				// nearestNode lies next to the square centroid
				Node nearestNode = network.getNearestNode( squareCentroid );

				// set square centroid and the node id of its nearest node
				this.travelTimeAccessibilityGrid.getValue( point ).setSquareCentroid(nearestNode.getId(), polygon, squareCentroid);
				this.travelCostAccessibilityGrid.getValue( point ).setSquareCentroid(nearestNode.getId(), polygon, squareCentroid);
				this.travelDistanceAccessibilityGrid.getValue( point ).setSquareCentroid(nearestNode.getId(), polygon, squareCentroid);
			}
		}
		// assigns all nodes that are located within the according square boundary
		// this is only relevant for interpolation computations
		Iterator<Node> nodeIterator = network.getNodes().values().iterator();
		for(;nodeIterator.hasNext();){
			Node node = nodeIterator.next();
			Point point = factory.createPoint( new Coordinate(node.getCoord().getX(), node.getCoord().getY()));

			// add nodes to square that are located within square boundaries ...
			this.travelTimeAccessibilityGrid.getValue( point ).addNode( node );
			this.travelCostAccessibilityGrid.getValue( point ).addNode( node );
			this.travelDistanceAccessibilityGrid.getValue( point ).addNode( node );
		}
		log.info(".. done initializing Spatial Grids!");
	}

//	private void writeSpatialGridTables() {
//
//		fillSpatialGrids();
//
//		log.info("Writing spatial grid tables ...");
//
//		assert (travelTimeAccessibilityGrid != null);
//		assert (travelDistanceAccessibilityGrid != null);
//		assert (travelCostAccessibilityGrid != null);
//
//		SpatialGridTableWriterERSA_V2 tableWriter = new SpatialGridTableWriterERSA_V2();
//
//		try {
//			// Travel Time Accessibility Table
//			log.info("Writing Travel Time Accessibility Measures ...");
//			tableWriter.write(this.travelTimeAccessibilityGrid,
//					Constants.MATSIM_4_OPUS_TEMP
//							+ Constants.ERSA_TRAVEL_TIME_ACCESSIBILITY
//							+ "_GridSize_" + resolutionMeter,
//					Constants.FILE_TYPE_TXT);
//			// Travel Distance Accessibility Table
//			log.info("Writing Travel Distance Accessibility Measures ...");
//			tableWriter.write(this.travelDistanceAccessibilityGrid,
//					Constants.MATSIM_4_OPUS_TEMP
//							+ Constants.ERSA_TRAVEL_DISTANCE_ACCESSIBILITY
//							+ "_GridSize_" + resolutionMeter,
//					Constants.FILE_TYPE_TXT);
//			// Travel Cost Accessibility Table
//			log.info("Writing Travel Cost Accessibility Measures ...");
//			tableWriter.write(this.travelCostAccessibilityGrid,
//					Constants.MATSIM_4_OPUS_TEMP
//							+ Constants.ERSA_TRAVEL_COST_ACCESSIBILITY
//							+ "_GridSize_" + resolutionMeter,
//					Constants.FILE_TYPE_TXT);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		log.info("... done writing spatial grid tables!");
//	}
//
//	private void fillSpatialGrids() {
//
//		log.info("Filling spatial grid tables ...");
//
//		assert (travelTimeAccessibilityGrid != null);
//		assert (travelDistanceAccessibilityGrid != null);
//		assert (travelCostAccessibilityGrid != null);
//
//		assert (travelTimeAccessibilityMap != null);
//		assert (travelDistanceAccessibilityMap != null);
//		assert (travelCostAccessibilityMap != null);
//
//		// compute derivation ...
//		fill(this.travelCostAccessibilityGrid, this.travelCostAccessibilityMap);
//		fill(this.travelDistanceAccessibilityGrid, this.travelDistanceAccessibilityMap);
//		fill(this.travelTimeAccessibilityGrid, this.travelTimeAccessibilityMap);
//
//		log.info("...done filling spatial grids!");
//	}
//
//	private void fill(SpatialGridOld<SquareLayer> grid, Map<Id, Double> map) {
//		int rows = grid.getNumRows();
//		int cols = grid.getNumCols(0);
//		log.info("Grid Rows: " + rows + " Grid Columns: " +cols);
//		for (int r = 0; r < rows; r++) {
//			for (int c = 0; c < cols; c++) {
//				SquareLayer layer = grid.getValue(r, c);
//				if(layer != null)
//					layer.computeDerivation(map);
//			}
//		}
//	}
}
