package playground.tnicolai.matsim4opus.matsim4urbansim;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.matsim4opus.gis.SpatialGrid;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.utils.LeastCostPathTree;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.gis.SpatialGridTableWriterERSA_V2;
import playground.tnicolai.matsim4opus.matsim4urbansim.ERSAControlerListener.TravelDistanceCostCalculator;
import playground.tnicolai.matsim4opus.utils.ProgressBar;
import playground.tnicolai.matsim4opus.utils.UtilityCollection;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.helperObjects.JobClusterObject;
import playground.tnicolai.matsim4opus.utils.helperObjects.NetworkBoundary;
import playground.tnicolai.matsim4opus.utils.helperObjects.SquareLayer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class ERSAControlerListenerV2 implements ShutdownListener{
	
	private static final Logger log = Logger.getLogger(ERSAControlerListenerV2.class);

	private JobClusterObject[] aggregatedJobArray;
	private int resolutionMeter;
	
	private SpatialGrid<SquareLayer> travelTimeAccessibilityGrid;
	private SpatialGrid<SquareLayer> travelCostAccessibilityGrid;
	private SpatialGrid<SquareLayer> travelDistanceAccessibilityGrid;
	
	private Map<Id, Double>travelTimeAccessibilityMap;
	private Map<Id, Double>travelCostAccessibilityMap;
	private Map<Id, Double>travelDistanceAccessibilityMap;
	
	private Benchmark benchmark;
	
	/**
	 * constructor
	 * @param aggregatedJobArray
	 */
	public ERSAControlerListenerV2(JobClusterObject[] aggregatedJobArray, int resolution, Benchmark benchmark){
		
		log.info("Initializing ERSAControlerListenerV2 ...");
		
		assert(aggregatedJobArray != null);
		this.aggregatedJobArray = aggregatedJobArray;
		assert(resolution > 0);
		this.resolutionMeter = resolution;
		assert(benchmark != null);
		this.benchmark = benchmark;
		
		this.travelCostAccessibilityMap = new HashMap<Id, Double>();
		this.travelTimeAccessibilityMap = new HashMap<Id, Double>();
		this.travelDistanceAccessibilityMap = new HashMap<Id, Double>();
		
		log.info(".. done initializing ERSAControlerListenerV2!");
	}
	
	
	/**
	 * calculating accessibility indicators
	 */
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		log.info("Entering notifyShutdown ..." );
		
		int benchmarkID = this.benchmark.addMeasure("1-Point accessibility computation");
		
		// get the controller and scenario
		Controler controler = event.getControler();
		Scenario sc = controler.getScenario();
		NetworkImpl network = controler.getNetwork();
		initSpatialGirds(network);
		
		// init LeastCostPathTree in order to calculate travel times and travel costs
		TravelTime ttc = controler.getTravelTimeCalculator();
		// this calculates the workplace accessibility travel times
		LeastCostPathTree lcptTravelTime = new LeastCostPathTree( ttc, new TravelTimeDistanceCostCalculator(ttc, controler.getConfig().planCalcScore()) );
		// this calculates the workplace accessibility distances
		LeastCostPathTree lcptTravelDistance = new LeastCostPathTree( ttc, new TravelDistanceCostCalculator() ); // tnicolai: this is experimental, check with Kai, sep'2011
		
		double depatureTime = 8.*3600;	// tnicolai: make configurable
		double beta_per_hr = sc.getConfig().planCalcScore().getTraveling_utils_hr() - sc.getConfig().planCalcScore().getPerforming_utils_hr();
		double beta_per_min = beta_per_hr / 60.; // get utility per minute
		
		try{
			log.info("Computing and writing accessibility measures ..." );
			Iterator<Node> startNodeIterator = network.getNodes().values().iterator();
			int numberOfStartNodes = network.getNodes().values().size();
			log.info("Caculating " + numberOfStartNodes + " starting points ...");
			
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
					int jobWeight = this.aggregatedJobArray[i].getNumberOfJobs();

					double arrivalTime = lcptTravelTime.getTree().get( nodeID ).getTime();
					
					// travel times in minutes
					double travelTime_min = (arrivalTime - depatureTime) / 60.;
					// travel costs in utils
					double travelCosts = lcptTravelTime.getTree().get( nodeID ).getCost();
					// travel distance by car in meter
					double travelDistance_meter = lcptTravelDistance.getTree().get( nodeID ).getCost();
					
					// sum travel times
					accessibilityTravelTimes += Math.exp( beta_per_min * travelTime_min ) * jobWeight;
					// sum travel costs
					accessibilityTravelTimeCosts += Math.exp( beta_per_min * travelCosts ) * jobWeight; // tnicolai: find another beta for travel costs
					// sum travel distances
					accessibilityTravelDistanceCosts += Math.exp( beta_per_min * travelDistance_meter ) * jobWeight; // tnicolai: find another beta for travel distance
				}
				
				// assign accessibility 
				this.travelCostAccessibilityMap.put(originNode.getId(), Math.log( accessibilityTravelTimeCosts ));
				this.travelDistanceAccessibilityMap.put(originNode.getId(), Math.log( accessibilityTravelDistanceCosts ));
				this.travelTimeAccessibilityMap.put(originNode.getId(), Math.log( accessibilityTravelTimes ));
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
			writeSpatialGridTables();
		}
	}
	
	private void initSpatialGirds(NetworkImpl network){
		
		log.info("Initializing Spatial Grids ...");
		NetworkBoundary nb = UtilityCollection.getNetworkBoundary(network);
		
		double xmin = nb.getMinX();
		double xmax = nb.getMaxX();
		double ymin = nb.getMinY();
		double ymax = nb.getMaxY();
		
		log.info("Detected network size: MinX=" + xmin + " MinY=" + ymin + " MaxX=" + xmax + " MaxY=" + ymax );
		
		// creating spatial grids, one for each accessibility measure ...
		this.travelTimeAccessibilityGrid = new SpatialGrid<SquareLayer>(xmin, ymin, xmax, ymax, this.resolutionMeter);
		this.travelCostAccessibilityGrid = new SpatialGrid<SquareLayer>(xmin, ymin, xmax, ymax, this.resolutionMeter);
		this.travelDistanceAccessibilityGrid = new SpatialGrid<SquareLayer>(xmin, ymin, xmax, ymax, this.resolutionMeter);
		
		GeometryFactory factory = new GeometryFactory();
		
		// create a square
		int rows = this.travelTimeAccessibilityGrid.getNumRows();
		int cols = this.travelTimeAccessibilityGrid.getNumCols(0);
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				this.travelTimeAccessibilityGrid.setValue(r, c, new SquareLayer());
				this.travelCostAccessibilityGrid.setValue(r, c, new SquareLayer());
				this.travelDistanceAccessibilityGrid.setValue(r, c, new SquareLayer());
			}
		}
		
		// init all squares in spatial grid and determine square centroid + nearest node
		for(double x = xmin; x <= xmax; x += this.resolutionMeter){
			for(double y = ymin; y <= ymax; y += this.resolutionMeter){
				
				// point points to lower left corner of a square
				Point point = factory.createPoint( new Coordinate(x, y));
				// centroid determines the center of a square
				Coord squareCentroid = new CoordImpl(x + (this.resolutionMeter/2), y + (this.resolutionMeter/2));
				// nearestNode lies next to the square centroid 
				Node nearestNode = network.getNearestNode( squareCentroid );
				
				// set square centroid and the node id of its nearest node
				this.travelTimeAccessibilityGrid.getValue( point ).setSquareCentroid(nearestNode.getId());
				this.travelCostAccessibilityGrid.getValue( point ).setSquareCentroid(nearestNode.getId());
				this.travelDistanceAccessibilityGrid.getValue( point ).setSquareCentroid(nearestNode.getId());
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
	
	private void writeSpatialGridTables() {
		
		fillSpatialGrids();
		
		log.info("Writing spatial grid tables ...");

		assert (travelTimeAccessibilityGrid != null);
		assert (travelDistanceAccessibilityGrid != null);
		assert (travelCostAccessibilityGrid != null);

		SpatialGridTableWriterERSA_V2 tableWriter = new SpatialGridTableWriterERSA_V2();

		try {
			// Travel Time Accessibility Table
			log.info("Writing Travel Time Accessibility Measures ...");
			tableWriter.write(this.travelTimeAccessibilityGrid,
					Constants.MATSIM_4_OPUS_TEMP
							+ Constants.ERSA_TRAVEL_TIME_ACCESSIBILITY
							+ "_GridSize_" + resolutionMeter,
					Constants.FILE_TYPE_TXT);
			// Travel Distance Accessibility Table
			log.info("Writing Travel Distance Accessibility Measures ...");
			tableWriter.write(this.travelDistanceAccessibilityGrid,
					Constants.MATSIM_4_OPUS_TEMP
							+ Constants.ERSA_TRAVEL_DISTANCE_ACCESSIBILITY
							+ "_GridSize_" + resolutionMeter,
					Constants.FILE_TYPE_TXT);
			// Travel Cost Accessibility Table
			log.info("Writing Travel Cost Accessibility Measures ...");
			tableWriter.write(this.travelCostAccessibilityGrid,
					Constants.MATSIM_4_OPUS_TEMP
							+ Constants.ERSA_TRAVEL_COST_ACCESSIBILITY
							+ "_GridSize_" + resolutionMeter,
					Constants.FILE_TYPE_TXT);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("... done writing spatial grid tables!");
	}
	
	private void fillSpatialGrids() {

		log.info("Filling spatial grid tables ...");

		assert (travelTimeAccessibilityGrid != null);
		assert (travelDistanceAccessibilityGrid != null);
		assert (travelCostAccessibilityGrid != null);

		assert (travelTimeAccessibilityMap != null);
		assert (travelDistanceAccessibilityMap != null);
		assert (travelCostAccessibilityMap != null);

		// compute derivation ...
		fill(this.travelCostAccessibilityGrid, this.travelCostAccessibilityMap);
		fill(this.travelDistanceAccessibilityGrid, this.travelDistanceAccessibilityMap);
		fill(this.travelTimeAccessibilityGrid, this.travelTimeAccessibilityMap);

		log.info("...done filling spatial grids!");
	}
	
	private void fill(SpatialGrid<SquareLayer> grid, Map<Id, Double> map) {
		int rows = grid.getNumRows();
		int cols = grid.getNumCols(0);
		log.info("Grid Rows: " + rows + " Grid Columns: " +cols);
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				SquareLayer layer = grid.getValue(r, c);
				if(layer != null)
					layer.computeDerivation(map);
			}
		}
	}
}


