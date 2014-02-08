package org.matsim.contrib.accessibility;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.costcalculator.TravelDistanceCalculator;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.utils.Benchmark;
import org.matsim.contrib.accessibility.utils.LeastCostPathTreeExtended;
import org.matsim.contrib.accessibility.utils.io.writer.AnalysisCellBasedAccessibilityCSVWriterV2;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.MyBoundingBox;
import org.matsim.contrib.matrixbasedptrouter.utils.TempDirectoryUtil;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.utils.LeastCostPathTree;

import com.vividsolutions.jts.geom.Geometry;

/**
 * improvements sep'11:
 * 
 * Code improvements since last version (deadline ersa paper): - Aggregated
 * Workplaces: Workplaces with same parcel_id are aggregated to a weighted job
 * (see JobClusterObject) This means much less iteration cycles - Less time
 * consuming look-ups: All workplaces are assigned to their nearest node in an
 * pre-proscess step (see addNearestNodeToJobClusterArray) instead to do nearest
 * node look-ups in each iteration cycle - Distance based accessibility added:
 * like the travel time accessibility computation now also distances are
 * computed with LeastCostPathTree (tnicolai feb'12 distances are replaced by
 * walking times which is also linear and corresponds to distances)
 * 
 * improvements jan'12:
 * 
 * - Better readability: Removed unused methods such as
 * "addNearestNodeToJobClusterArray" (this is done while gathering/processing
 * workplaces). Also all results are now dumped directly from this class.
 * Before, the SpatialGrid tables were transfered to another class to dump out
 * the results. This also improves readability - Workplace data dump: Dumping
 * out the used workplace data was simplified, since the simulation now already
 * uses aggregated data. Corresponding subroutines aggregating the data are not
 * needed any more (see dumpWorkplaceData()). But coordinates of the origin
 * workplaces could not dumped out, this is now done in
 * ReadFromUrbansimParcelModel during processing the UrbnAism job data
 * 
 * improvements feb'12 - distance between square centroid and nearest node on
 * road network is considered in the accessibility computation as walk time of
 * the euclidian distance between both (centroid and nearest node). This walk
 * time is added as an offset to each measured travel times - using walk travel
 * times instead of travel distances. This is because of the betas that are
 * utils/time unit. The walk time corresponds to distances since this is also
 * linear.
 * 
 * improvements march'12 - revised distance measure from centroid to network:
 * using orthogonal distance from centroid to nearest network link! - merged
 * CellBasedAccessibilityNetworkControlerListener and
 * CellBasedAccessibilityShapefileControlerListener
 * 
 * improvements april'12 - accessibility calculation uses configurable betas
 * (coming from UrbanSim) for car/walk travel times, -distances and -costs -
 * replaced "SpatialGrid<Double>" by "SpatialGrid" using double instead of
 * Double-objects
 * 
 * improvements may'12 - including interpolated (spatial grid) feedback for each
 * parcel
 * 
 * improvements / changes june'12 
 * - the walk distance (measuring point -> nearest node) for accessibilities by 
 * car has changed: Now only the orthoganal distance (measuring point -> nearest 
 * link) is measured.
 * - re-added free-speed car travel time calculation
 * - added accessibility calculation for bike
 * - using network.getNearestLinkExactly instead of network.getNearestLink. 
 *   the new entry does not use nearest nodes to determine the link it directly detects the nearest link. 
 *   This avoids some artifacts in accessibility computation (like selective fluctuation in accessibility )
 *   
 * improvements / changes july'12 
 * - fixed error: used pre-factor (1/beta scale) in deterrence function instead of beta scale (fixed now!) 
 * 
 * improvements aug'12
 * - the aggregated opportunities now contain the euclidian distance to the nereast node on the network. This
 *   is used to determine the total costs cij in the accessibility measure
 *   
 * changes sep'12
 * - renaming from CellBasedAccessibilityControlerListenerV3 into ParcelBasedAccessibilityControlerListenerV3
 * 
 * improvements jan'13
 * - added pt for accessibility calculation
 * 
 * improvements april'13
 * - congested car modes uses TravelDisutility from MATSim
 * - taking disutilites directly from MATSim (controler.createTravelCostCalculator()), this 
 * also activates road pricing ...
 * 
 * improvements may'13
 * - grid based accessibility data is written into matsim output folder
 * 
 * changes june'13
 * - changed class name from ParcelBasedAccessibilityControlerListenerV3 into GridBasedAccessibilityControlerListenerV3
 * - providing opportunity facilities (e.g. workplaces)
 * - reduced dependencies to MATSim4UrbanSim contrib: replaced ZoneLayer<Id> and Zone by standard MATSim ActivityFacilities
 * - relevant transport modes for which to calculate the accessibility are now configurable. with this accessibilities
 *   are not calculated for unselected transport modes to improve computing times
 * 
 * @author thomas
 * 
 */
public class GridBasedAccessibilityControlerListenerV3 extends AccessibilityControlerListenerImpl 
implements ShutdownListener{ 
	
	private static final Logger log = Logger.getLogger(GridBasedAccessibilityControlerListenerV3.class);
	private AnalysisCellBasedAccessibilityCSVWriterV2 accessibilityWriter;
	private Network network;
	
	// ////////////////////////////////////////////////////////////////////
	// constructors
	// ////////////////////////////////////////////////////////////////////
	
	/**
	 * constructor
	 * 
	 * @param opportunities represented by ActivityFacilitiesImpl 
	 * @param ptMatrix matrix with travel times and distances for any pair of pt stops
	 * @param config MATSim Config object
	 * @param network MATSim road network
	 */
	public GridBasedAccessibilityControlerListenerV3(ActivityFacilities opportunities,
													 PtMatrix ptMatrix,
													 Config config, 
													 Network network){
		// I thought about chaning the type of opportunities to Map<Id,Facility> or even Collection<Facility>, but in the end
		// one can also use FacilitiesUtils.createActivitiesFacilities(), put everything in there, and give that to this constructor. kai, feb'14

		log.info("Initializing  ...");

		this.ptMatrix = ptMatrix;	// this could be zero if no input files for pseudo pt are given ...
		assert (config != null);
		assert (network != null);

		this.benchmark = new Benchmark();
		
		// writing accessibility measures continuously into a csv file, which is not 
		// dedicated for as input for UrbanSim, but for analysis purposes
		String matsimOutputDirectory = config.controler().getOutputDirectory();
		try {
			accessibilityWriter = new AnalysisCellBasedAccessibilityCSVWriterV2(matsimOutputDirectory);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("The output directory hierarchy needs to be in place when this constructor is called") ;
		}
		initAccessibilityParameters(config);
		// aggregating facilities to their nearest node on the road network
		this.aggregatedFacilities = aggregatedOpportunities(opportunities, network);
		// use network as global variable, otherwise another network might be used during 
		// notifyShutDown(...) (network may be preprocessed, e.g. only car-links.)
		this.network = network;
		log.info(".. done initializing CellBasedAccessibilityControlerListenerV3");
	}
	
	private boolean alreadyActive = false ;
	
	@Override
	public void notifyShutdown(ShutdownEvent event){
		if ( alreadyActive ) {
			return ; // don't need this a second time
		}
		alreadyActive = true ;
		log.info("Entering notifyShutdown ..." );
		
		// make sure that measuring points are set.
		if(this.measuringPoints == null){
			log.error("No measuring points found! For this reason no accessibilities can be calculated!");
			log.info("Please use one of the following methods when initializing the accessibility listener to fix this problem:");
			log.info("1) generateGridsAndMeasuringPointsByShapeFile(String shapeFile, double cellSize)");
			log.info("2) ggenerateGridsAndMeasuringPointsByCustomBoundary(double minX, double minY, double maxX, double maxY, double cellSize)");
			log.info("3) generateGridsAndMeasuringPointsByNetwork(Network network, double cellSize)");
			return;
		}
			
		
		// get the controller and scenario
		Controler controler = event.getControler();
		NetworkImpl network = (NetworkImpl) this.network; //(NetworkImpl) controler.getNetwork();
		
		int benchmarkID = this.benchmark.addMeasure("cell-based accessibility computation");

		// get the free-speed car travel times (in seconds)
		TravelTime ttf = new FreeSpeedTravelTime() ;
		TravelDisutility tdFree = controler.getTravelDisutilityFactory().createTravelDisutility(ttf, controler.getConfig().planCalcScore() ) ;
		LeastCostPathTreeExtended lcptExtFreeSpeedCarTrvelTime = new LeastCostPathTreeExtended( ttf, tdFree, (RoadPricingSchemeImpl) controler.getScenario().getScenarioElement(RoadPricingScheme.ELEMENT_NAME) ) ;

		// get the congested car travel time (in seconds)
		TravelTime ttc = controler.getLinkTravelTimes(); // congested
		TravelDisutility tdCongested = controler.getTravelDisutilityFactory().createTravelDisutility(ttc, controler.getConfig().planCalcScore() ) ;
		LeastCostPathTreeExtended  lcptExtCongestedCarTravelTime = new LeastCostPathTreeExtended(ttc, tdCongested, (RoadPricingSchemeImpl) controler.getScenario().getScenarioElement(RoadPricingScheme.ELEMENT_NAME) ) ;

		// get travel distance (in meter)
		LeastCostPathTree lcptTravelDistance		 = new LeastCostPathTree( ttf, new TravelDistanceCalculator());
		
		this.scheme = (RoadPricingSchemeImpl) controler.getScenario().getScenarioElement(RoadPricingScheme.ELEMENT_NAME);

//		try{
			log.info("Computing and writing cell based accessibility measures ...");
			// printParameterSettings(); // use only for debugging (settings are printed as part of config dump)
			log.info(measuringPoints.getFacilities().values().size() + " measurement points are now processing ...");
			
			accessibilityComputation(ttc, 
									 lcptExtFreeSpeedCarTrvelTime,
									 lcptExtCongestedCarTravelTime, 
									 lcptTravelDistance, 
									 network,
									 measuringPoints,
									 PARCEL_BASED);
			System.out.println();

			if (this.benchmark != null && benchmarkID > 0) {
				this.benchmark.stoppMeasurement(benchmarkID);
				log.info("Accessibility computation with "
						+ measuringPoints.getFacilities().size()
						+ " starting points (origins) and "
						+ this.aggregatedFacilities.length
						+ " destinations (opportunities) took "
						+ this.benchmark.getDurationInSeconds(benchmarkID)
						+ " seconds ("
						+ this.benchmark.getDurationInSeconds(benchmarkID)
						/ 60. + " minutes).");
			}
			
			String matsimOutputDirectory = event.getControler().getScenario().getConfig().controler().getOutputDirectory();
			
			accessibilityWriter.close(); 
			writePlottingData(matsimOutputDirectory);			
			
			if(this.spatialGridDataExchangeListenerList != null){
				log.info("Triggering " + this.spatialGridDataExchangeListenerList.size() + " SpatialGridDataExchangeListener(s) ...");
				for(int i = 0; i < this.spatialGridDataExchangeListenerList.size(); i++)
					this.spatialGridDataExchangeListenerList.get(i).getAndProcessSpatialGrids(freeSpeedGrid, carGrid, bikeGrid, walkGrid, ptGrid);
			}
			
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	/**
	 * Writes the measured accessibility for the current measurePoint instantly
	 * to disc in csv format.
	 * 
	 * @param measurePoint
	 * @param fromNode
	 * @param freeSpeedAccessibility
	 * @param carAccessibility
	 * @param bikeAccessibility
	 * @param walkAccessibility
	 * @param accCsvWriter
	 */
	@Override
	protected void writeCSVData(
			ActivityFacility measurePoint, Node fromNode,
			double freeSpeedAccessibility, double carAccessibility,
			double bikeAccessibility, double walkAccessibility,
			double ptAccessibility) {
		
		// writing accessibility measures of current measurePoint in csv format
		accessibilityWriter.writeRecord(measurePoint, fromNode, freeSpeedAccessibility,
														carAccessibility, bikeAccessibility, walkAccessibility, ptAccessibility);
	}
	
	/**
	 * This writes the accessibility grid data into the MATSim output directory
	 * 
	 * @param matsimOutputDirectory
	 * @throws IOException
	 */
	private void writePlottingData(String matsimOutputDirectory) {
		
		final String FILE_TYPE_TXT = ".txt";

		log.info("Writing plotting data for R analyis into " + matsimOutputDirectory + " ...");
		if(freeSpeedGrid != null) {
			GridUtils.writeSpatialGridTable(freeSpeedGrid, matsimOutputDirectory
				+ "/" + FREESEED_FILENAME + freeSpeedGrid.getResolution()
				+ FILE_TYPE_TXT);
			AnalysisCellBasedAccessibilityCSVWriterV2 writer = new AnalysisCellBasedAccessibilityCSVWriterV2(matsimOutputDirectory,"freeSpeed") ;
			for(double y = freeSpeedGrid.getYmin(); y <= freeSpeedGrid.getYmax() ; y += freeSpeedGrid.getResolution()) {
				for(double x = freeSpeedGrid.getXmin(); x <= freeSpeedGrid.getXmax(); x += freeSpeedGrid.getResolution()) {
					final double value = freeSpeedGrid.getValue(x, y);
					if ( !Double.isNaN(value ) ) { 
						writer.writeRecord( new CoordImpl(x,y), value) ;
					}
				}
				writer.writeNewLine() ;
			}
			writer.close() ;
		}
		if(carGrid != null) {
			GridUtils.writeSpatialGridTable(carGrid, matsimOutputDirectory
				+ "/" + CAR_FILENAME + carGrid.getResolution()
				+ FILE_TYPE_TXT);
			AnalysisCellBasedAccessibilityCSVWriterV2 writer = new AnalysisCellBasedAccessibilityCSVWriterV2(matsimOutputDirectory,"car") ;
			for(double y = carGrid.getYmin(); y <= carGrid.getYmax() ; y += carGrid.getResolution()) {
				for(double x = carGrid.getXmin(); x <= carGrid.getXmax(); x += carGrid.getResolution()) {
					writer.writeRecord( new CoordImpl(x,y), carGrid.getValue(x, y)) ;
				}
				writer.writeNewLine() ;
			}
			writer.close() ;
		}
		if(bikeGrid != null) {
			GridUtils.writeSpatialGridTable(bikeGrid, matsimOutputDirectory
				+ "/" + BIKE_FILENAME + bikeGrid.getResolution()
				+ FILE_TYPE_TXT);
		}
		if(walkGrid != null) {
			GridUtils.writeSpatialGridTable(walkGrid, matsimOutputDirectory
				+ "/" + WALK_FILENAME + walkGrid.getResolution()
				+ FILE_TYPE_TXT);
		}
		if(ptGrid != null) {
			GridUtils.writeSpatialGridTable(ptGrid, matsimOutputDirectory
				+ "/" + PT_FILENAME + ptGrid.getResolution()
				+ FILE_TYPE_TXT);
		}
		log.info("Writing plotting data for R done!");
	}
	
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// GridBasedAccessibilityControlerListenerV3 specific methods that do not apply to zone-based accessibility measures
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * Generates the activated SpatialGrids and measuring points for accessibility calculation.
	 * The given ShapeFile determines the area for which to compute/measure accessibilities.
	 * 
	 * @param shapeFile String giving the path to the shape file
	 * @param cellSize double value giving the the side length of the cell in meter
	 */
	public void generateGridsAndMeasuringPointsByShapeFile(String shapeFile, double cellSize){
		
		if(TempDirectoryUtil.pathExists(shapeFile))
			log.info("Using shape file to determine the area for accessibility computation.");
		else
			throw new RuntimeException("ShapeFile for accessibility computation not found: " + shapeFile);
		
		Geometry boundary = GridUtils.getBoundary(shapeFile);
		
		measuringPoints = GridUtils.createGridLayerByGridSizeByShapeFileV2(boundary, cellSize);
		if(useFreeSpeedGrid)
			freeSpeedGrid = GridUtils.createSpatialGridByShapeBoundary(boundary, cellSize);
		if(useCarGrid)
			carGrid	= GridUtils.createSpatialGridByShapeBoundary(boundary, cellSize);
		if(useBikeGrid)
			bikeGrid = GridUtils.createSpatialGridByShapeBoundary(boundary, cellSize);
		if(useWalkGrid)
			walkGrid = GridUtils.createSpatialGridByShapeBoundary(boundary, cellSize);
		if(usePtGrid)
			ptGrid = GridUtils.createSpatialGridByShapeBoundary(boundary, cellSize);
	}
	
	/**
	 * Generates the activated SpatialGrids and measuring points for accessibility calculation.
	 * The given custom boundary determines the area for which to compute/measure accessibilities.
	 * 
	 * @param minX double value giving the left x coordinate of the area
	 * @param minY double value giving the bottom y coordinate of the area
	 * @param maxX double value giving the right x coordinate of the area
	 * @param maxY double value giving the top y coordinate of the area
	 * @param cellSize double value giving the the side length of the cell in meter
	 */
	public void generateGridsAndMeasuringPointsByCustomBoundary(double minX, double minY, double maxX, double maxY, double cellSize){
		
		log.info("Using custom bounding box to determine the area for accessibility computation.");
		
		generateGridsAndMeasuringPoints(minX, minY, maxX, maxY, cellSize);
	}
	
	/**
	 * Generates the activated SpatialGrids and measuring points for accessibility calculation.
	 * The given road network determines the area for which to compute/measure accessibilities.
	 * 
	 * @param network MATSim road network
	 * @param cellSize double value giving the the side length of the cell in meter
	 */
	public void generateGridsAndMeasuringPointsByNetwork(Network network, double cellSize){
		
		log.info("Using the boundary of the network file to determine the area for accessibility computation.");
		log.warn("This could lead to memory issues when the network is large and/or the cell size is too fine!");
		
		MyBoundingBox bb = new MyBoundingBox();
		bb.setDefaultBoundaryBox(network);
		
		generateGridsAndMeasuringPoints(bb.getXMin(), bb.getYMin(), bb.getXMax(), bb.getYMax(), cellSize);
	}
	
	/**
	 * Common method for generateGridsAndMeasuringPointsByCustomBoundary and 
	 * generateGridsAndMeasuringPointsByNetwork to generate SpatialGrids and 
	 * measuring points for accessibility calculation.
	 * 
	 * @param minX double value giving the left x coordinate of the area
	 * @param minY double value giving the bottom y coordinate of the area
	 * @param maxX double value giving the right x coordinate of the area
	 * @param maxY double value giving the top y coordinate of the area
	 * @param cellSize double value giving the the side length of the cell in meter
	 */
	private void generateGridsAndMeasuringPoints(double minX, double minY,
			double maxX, double maxY, double cellSize) {
		measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(minX, minY, maxX, maxY, cellSize);
		if(useFreeSpeedGrid)
			freeSpeedGrid = new SpatialGrid(minX, minY, maxX, maxY, cellSize);
		if(useCarGrid)
			carGrid = new SpatialGrid(minX, minY, maxX, maxY, cellSize);
		if(useBikeGrid)
			bikeGrid = new SpatialGrid(minX, minY, maxX, maxY, cellSize);
		if(useWalkGrid)
			walkGrid = new SpatialGrid(minX, minY, maxX, maxY, cellSize);
		if(usePtGrid)
			ptGrid = new SpatialGrid(minX, minY, maxX, maxY, cellSize);
	}
}
