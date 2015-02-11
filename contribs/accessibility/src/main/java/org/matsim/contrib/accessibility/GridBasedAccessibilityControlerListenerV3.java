package org.matsim.contrib.accessibility;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.utils.Benchmark;
import org.matsim.contrib.accessibility.utils.LeastCostPathTreeExtended;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.contrib.matrixbasedptrouter.utils.TempDirectoryUtil;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.roadpricing.RoadPricingScheme;
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
public final class GridBasedAccessibilityControlerListenerV3 extends AccessibilityControlerListenerImpl 
implements ShutdownListener, StartupListener { 
	private static final Logger log = Logger.getLogger(GridBasedAccessibilityControlerListenerV3.class);
	
	private UrbansimCellBasedAccessibilityCSVWriterV2 urbansimAccessibilityWriter;
	private Network network;
	private Config config;
	// for consideration of different activity types or different modes (or both) subdirectories are
	// required in order not to confuse the output
	private String outputSubdirectory;
	

	// ////////////////////////////////////////////////////////////////////
	// constructors
	// ////////////////////////////////////////////////////////////////////

	public GridBasedAccessibilityControlerListenerV3(ActivityFacilities opportunities, Config config, Network network){
		this(opportunities, null, config, network); // PtMatrix is optional and in a different contrib
	}
	
	
	/**
	 * constructor
	 * 
	 * @param opportunities represented by ActivityFacilitiesImpl 
	 * @param ptMatrix matrix with travel times and distances for any pair of pt stops
	 * @param config MATSim Config object
	 * @param network MATSim road network
	 */
	public GridBasedAccessibilityControlerListenerV3(ActivityFacilities opportunities, PtMatrix ptMatrix, Config config, Network network){
		// I thought about changing the type of opportunities to Map<Id,Facility> or even Collection<Facility>, but in the end
		// one can also use FacilitiesUtils.createActivitiesFacilities(), put everything in there, and give that to this constructor. kai, feb'14

		log.info("Initializing  ...");

		this.ptMatrix = ptMatrix;	// this could be zero if no input files for pseudo pt are given ...
		assert (config != null);
		this.config = config ;
		assert (network != null);

		this.benchmark = new Benchmark();

		initAccessibilityParameters(config);

		// aggregating facilities to their nearest node on the road network
		this.aggregatedOpportunities = aggregatedOpportunities(opportunities, network);
		// yyyy ignores the "capacities" of the facilities.  kai, mar'14
		
		
		// use network as global variable, otherwise another network might be used during 
		// notifyShutDown(...) (network may be preprocessed, e.g. only car-links.)
		this.network = network;
		log.info(".. done initializing CellBasedAccessibilityControlerListenerV3");
	}

	
	@Override
	public void notifyStartup(StartupEvent event) {
		// I moved this from the constructor since it did actually NOT work in situations where the output directory hierarchy was not there
		// from the beginning ... since the matsim Controler instantiates this not before the "run" statement ... which is the only way in which
		// setOverwriteDirectories can be honoured.  kai, feb'14

		// writing accessibility measures continuously into a csv file, which is not 
		// dedicated for as input for UrbanSim, but for analysis purposes
		
		// in case multiple AccessibilityControlerListeners are added to the controller, e.g. if various calculations are done for
		// different activity types or different modes (or both) subdirectories are required in order not to confuse the output
		if (outputSubdirectory == null) {
			urbansimAccessibilityWriter = new UrbansimCellBasedAccessibilityCSVWriterV2(config.controler().getOutputDirectory());
		} else {
			File file = new File(config.controler().getOutputDirectory() + "/" + outputSubdirectory);
			file.mkdirs();
			urbansimAccessibilityWriter = new UrbansimCellBasedAccessibilityCSVWriterV2(
					config.controler().getOutputDirectory() + "/" + outputSubdirectory);
		}
	}


	private boolean alreadyActive = false ;
	private List<ActivityFacilities> additionalFacilityData = new ArrayList<ActivityFacilities>() ; 
	private Map<String,Tuple<SpatialGrid,SpatialGrid>> additionalSpatialGrids = new TreeMap<String,Tuple<SpatialGrid,SpatialGrid>>() ;
	//(not sure if this is a bit odd ... but I always need TWO spatial grids. kai, mar'14)
	private boolean lockedForAdditionalFacilityData = false;

	
	@Override
	public void notifyShutdown(ShutdownEvent event){
		if ( alreadyActive ) {
			return ; // don't need this a second time, which can happen if the irregular shutdown is called within the regular shutdown
		}
		alreadyActive = true ;
		log.info("Entering notifyShutdown ..." );

		// make sure that measuring points are set.
		if(this.measuringPoints == null){
			// yy this test is really a bit late AFTER all the iterations. kai, mar'14
			
			log.error("No measuring points found! For this reason no accessibilities can be calculated!");
			log.info("Please use one of the following methods when initializing the accessibility listener to fix this problem:");
			log.info("1) generateGridsAndMeasuringPointsByShapeFile(String shapeFile, double cellSize)");
			log.info("2) ggenerateGridsAndMeasuringPointsByCustomBoundary(double minX, double minY, double maxX, double maxY, double cellSize)");
			log.info("3) generateGridsAndMeasuringPointsByNetwork(Network network, double cellSize)");
			return;
		}

		// prepare the additional columns:
		for ( ActivityFacilities facilities : this.additionalFacilityData ) {
			Tuple<SpatialGrid,SpatialGrid> spatialGrids = this.additionalSpatialGrids.get( facilities.getName() ) ;
			GridUtils.aggregateFacilitiesIntoSpatialGrid(facilities, spatialGrids.getFirst(), spatialGrids.getSecond());
		}

		// get the controller and scenario
		Controler controler = event.getControler();

		int benchmarkID = this.benchmark.addMeasure("cell-based accessibility computation");

		// get the free-speed car travel times (in seconds)
		TravelTime ttf = new FreeSpeedTravelTime() ;
		TravelDisutility tdFree = controler.getTravelDisutilityFactory().createTravelDisutility(ttf, controler.getConfig().planCalcScore() ) ;
		LeastCostPathTreeExtended lcptExtFreeSpeedCarTrvelTime = new LeastCostPathTreeExtended( ttf, tdFree, (RoadPricingScheme) controler.getScenario().getScenarioElement(RoadPricingScheme.ELEMENT_NAME) ) ;

		// get the congested car travel time (in seconds)
		TravelTime ttc = null ;
		if ( controler != null ) {
			ttc = controler.getLinkTravelTimes(); // congested
		} else {
			EventsManager events = EventsUtils.createEventsManager() ;
			
			TravelTimeCalculator ttcalc = new TravelTimeCalculator(network, this.config.travelTimeCalculator() ) ;
			events.addHandler( ttcalc );
			
			MatsimEventsReader reader = new MatsimEventsReader(events);
			reader.readFile("events.xml.gz");
			
			ttc = ttcalc.getLinkTravelTimes() ;
		}
		TravelDisutility tdCongested = controler.getTravelDisutilityFactory().createTravelDisutility(ttc, controler.getConfig().planCalcScore() ) ;
		LeastCostPathTreeExtended  lcptExtCongestedCarTravelTime = new LeastCostPathTreeExtended(ttc, tdCongested, (RoadPricingScheme) controler.getScenario().getScenarioElement(RoadPricingScheme.ELEMENT_NAME) ) ;

		// get travel distance (in meter)
		LeastCostPathTree lcptTravelDistance		 = new LeastCostPathTree( ttf, new LinkLengthTravelDisutility());

		this.scheme = (RoadPricingScheme) controler.getScenario().getScenarioElement(RoadPricingScheme.ELEMENT_NAME);

		log.info("Computing and writing cell based accessibility measures ...");
		// printParameterSettings(); // use only for debugging (settings are printed as part of config dump)
		log.info(measuringPoints.getFacilities().values().size() + " measurement points are now processing ...");

		accessibilityComputation(ttc, 
				lcptExtFreeSpeedCarTrvelTime,
				lcptExtCongestedCarTravelTime, 
				lcptTravelDistance, 
				(NetworkImpl) network,
				measuringPoints,
				PARCEL_BASED);
		System.out.println();

		if (this.benchmark != null && benchmarkID > 0) {
			this.benchmark.stoppMeasurement(benchmarkID);
			log.info("Accessibility computation with "
					+ measuringPoints.getFacilities().size()
					+ " starting points (origins) and "
					+ this.aggregatedOpportunities.length
					+ " destinations (opportunities) took "
					+ this.benchmark.getDurationInSeconds(benchmarkID)
					+ " seconds ("
					+ this.benchmark.getDurationInSeconds(benchmarkID)
					/ 60. + " minutes).");
		}
		
		
		String matsimOutputDirectory = event.getControler().getScenario().getConfig().controler().getOutputDirectory();
			

		urbansimAccessibilityWriter.close();
		
		
		// as for the other writer above: In case multiple AccessibilityControlerListeners are added to the controller, e.g. if 
		// various calculations are done for different activity types or different modes (or both) subdirectories are required
		// in order not to confuse the output
		if (outputSubdirectory == null) {
			writePlottingData(matsimOutputDirectory);
		} else {
			writePlottingData(matsimOutputDirectory + "/" + outputSubdirectory);
		}
		

		if(this.spatialGridDataExchangeListenerList != null){
			log.info("Triggering " + this.spatialGridDataExchangeListenerList.size() + " SpatialGridDataExchangeListener(s) ...");
			for(int i = 0; i < this.spatialGridDataExchangeListenerList.size(); i++)
				this.spatialGridDataExchangeListenerList.get(i).getAndProcessSpatialGrids( accessibilityGrids );
		}

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
	final void writeCSVData4Urbansim( ActivityFacility measurePoint, Node fromNode, Map<Modes4Accessibility,Double> accessibilities ) {
		// (this is what, I think, writes the urbansim data, and should thus better not be touched. kai, feb'14)

		// writing accessibility measures of current measurePoint in csv format
		urbansimAccessibilityWriter.writeRecord(measurePoint, fromNode, accessibilities ) ;
	}

	
	/**
	 * This writes the accessibility grid data into the MATSim output directory
	 * 
	 * @param adaptedOutputDirectory
	 * @throws IOException
	 */
	private final void writePlottingData(String adaptedOutputDirectory) {

		log.info("Writing plotting data for R analyis into " + adaptedOutputDirectory + " ...");
		for ( Modes4Accessibility mode : Modes4Accessibility.values()  ) {
			if ( this.isComputingMode.get(mode) ) {
				final SpatialGrid spatialGrid = this.accessibilityGrids.get(mode);

				// output for R:
				GridUtils.writeSpatialGridTable( spatialGrid, adaptedOutputDirectory
						+ "/" + mode.toString() + Labels.ACCESSIBILITY_CELLSIZE + spatialGrid.getResolution() + ".txt");

			}
		}
		log.info("Writing plotting data for R done!");

		
		// in the following, the data used for gnuplot or QGis is written. dz, feb'15
		// different separators have to be used to make this output useable by gnuplot or QGis, respectively
		log.info("Writing plotting data for other analyis into " + adaptedOutputDirectory + " ...");

		final CSVWriter writer = new CSVWriter(adaptedOutputDirectory + "/" + CSVWriter.FILE_NAME ) ;

		final SpatialGrid spatialGrid = this.accessibilityGrids.get( Modes4Accessibility.freeSpeed ) ;
		// yy for time being, have to assume that this is always there
		for(double y = spatialGrid.getYmin(); y <= spatialGrid.getYmax() ; y += spatialGrid.getResolution()) {
			for(double x = spatialGrid.getXmin(); x <= spatialGrid.getXmax(); x += spatialGrid.getResolution()) {
				writer.writeField( x ) ;
				writer.writeField( y ) ;
				for ( Modes4Accessibility mode : Modes4Accessibility.values()  ) {
					if ( this.isComputingMode.get(mode) ) {
						final SpatialGrid theSpatialGrid = this.accessibilityGrids.get(mode);
						final double value = theSpatialGrid.getValue(x, y);
						if ( !Double.isNaN(value ) ) { 
							writer.writeField( value ) ;
						} else {
							writer.writeField( Double.NaN ) ;
						}
					} else {
						writer.writeField( Double.NaN ) ;
					}
				}
				for ( Tuple<SpatialGrid,SpatialGrid> additionalGrids : this.additionalSpatialGrids.values() ) {
					writer.writeField( additionalGrids.getFirst().getValue(x, y) ) ;
					writer.writeField( additionalGrids.getSecond().getValue(x, y) ) ;
				}
				writer.writeNewLine(); 
			}
			writer.writeNewLine(); // gnuplot pm3d scanline 
		}
		writer.close() ;

		log.info("Writing plotting data for other analysis done!");
		
		
		// Write accessibility (so far only for mode freeSpeed) to a shapefile
		
		// this was used to create point objects with accessibility values in a shapefile and then load this
		// shapefile in QGis and draw theses points as tiles coloured with respect to their accessibility values
		// now that we know how handling csv files in QGis works, the conversion into points and storing them
		// in a shapefile does not seem to be needed anymore
//		PointFeatureFactory factory = new PointFeatureFactory.Builder()
//		.setCrs(MGC.getCRS(TransformationFactory.WGS84))
//		.setName("accessibility")
//		.addAttribute("access", Double.class)
//		.create();
//		
//		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
//		
//		for(double y = spatialGrid.getYmin(); y <= spatialGrid.getYmax() ; y += spatialGrid.getResolution()) {
//			for(double x = spatialGrid.getXmin(); x <= spatialGrid.getXmax(); x += spatialGrid.getResolution()) {
//				// so far only for mode freeSpeed
//				final SpatialGrid theSpatialGrid = this.accessibilityGrids.get(Modes4Accessibility.freeSpeed);
//				final double value = theSpatialGrid.getValue(x, y);
//				
//				CoordImpl coord = new CoordImpl(x, y);
//				factory.createPoint(MGC.coord2Coordinate(coord), new Object[] {value}, null);
//				SimpleFeature feature = factory.createPoint(MGC.coord2Coordinate(coord), new Object[] {value}, null);
//				features.add(feature);
//			}
//		}
//				
//		log.info("Writing accessibilities and densities to shapefile... ");
//		ShapeFileWriter.writeGeometries(features, adaptedOutputDirectory + "/accessibilities.shp");
//		log.info("Writing accessibilities and densities to shapefile... Done. ");
	}
	

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// GridBasedAccessibilityControlerListenerV3 specific methods that do not apply to zone-based accessibility measures
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * Generates the activated SpatialGrids and measuring points for accessibility calculation.
	 * The given ShapeFile determines the area for which to compute/measure accessibilities.
	 * 
	 * @param shapeFileName String giving the path to the shape file
	 * @param cellSize double value giving the the side length of the cell in meter
	 */
	public void generateGridsAndMeasuringPointsByShapeFile(String shapeFileName, double cellSize){

		if(TempDirectoryUtil.pathExists(shapeFileName))
			log.info("Using shape file to determine the area for accessibility computation.");
		else
			throw new RuntimeException("ShapeFile for accessibility computation not found: " + shapeFileName);

		Geometry boundary = GridUtils.getBoundary(shapeFileName);
		measuringPoints = GridUtils.createGridLayerByGridSizeByShapeFileV2(boundary, cellSize);
		for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
			if ( this.isComputingMode.get(mode) ) {
				this.accessibilityGrids.put( mode, GridUtils.createSpatialGridByShapeBoundary(boundary, cellSize ) ) ;
			}
		}
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
	 * @param cellSize double value giving the the side length of the cell in meter
	 */
	public void generateGridsAndMeasuringPointsByNetwork(double cellSize){
		log.info("Using the boundary of the network file to determine the area for accessibility computation.");
		log.warn("This could lead to memory issues when the network is large and/or the cell size is too fine!");
		if (cellSize <= 0) {
			new RuntimeException("Cell Size needs to be assigned a value greater than zero.");
		}
		BoundingBox bb = BoundingBox.createBoundingBox(network);
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
	private void generateGridsAndMeasuringPoints(double minX, double minY, double maxX, double maxY, double cellSize) {
		measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(minX, minY, maxX, maxY, cellSize);
		for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
			if ( this.isComputingMode.get(mode) ) {
				this.accessibilityGrids.put( mode, new SpatialGrid(minX, minY, maxX, maxY, cellSize, Double.NaN) ) ;
			}
		}
		lockedForAdditionalFacilityData  = true ;
		for ( ActivityFacilities facilities : this.additionalFacilityData ) {
			if ( this.additionalSpatialGrids.get( facilities.getName() ) != null ) {
				throw new RuntimeException("this should not yet exist ...") ;
			}
			Tuple<SpatialGrid,SpatialGrid> spatialGrids = new Tuple<SpatialGrid,SpatialGrid>(
					new SpatialGrid( minX, minY, maxX, maxY, cellSize, 0. ) , new SpatialGrid( minX, minY, maxX, maxY, cellSize, 0. ) ) ;
			this.additionalSpatialGrids.put( facilities.getName(), spatialGrids ) ;
		}
	}
	

	/**
	 * I wanted to plot something like (max(acc)-acc)*population.  For that, I needed "population" at the x/y coordinates.
	 * This is the mechanics via which I inserted that. (The computation is then done in postprocessing.)
	 * <p/>
	 * You can add arbitrary ActivityFacilities containers here.  They will be aggregated to the grid points, and then written to
	 * file as additional column.
	 */
	public void addAdditionalFacilityData(ActivityFacilities facilities ) {
		log.warn("changed this data flow (by adding the _cnt_ column) but did not test.  If it works, please remove this warning. kai, mar'14") ;
		
		if ( this.lockedForAdditionalFacilityData ) {
			throw new RuntimeException("too late for adding additional facility data; spatial grids have already been generated.  Needs"
					+ " to be called before generating the spatial grids.  (This design should be improved ..)") ;
		}
		if ( facilities.getName()==null || facilities.getName().equals("") ) {
			throw new RuntimeException("cannot add unnamed facility containers here since we need a key to find them again") ;
		}
		for ( ActivityFacilities existingFacilities : this.additionalFacilityData ) {
			if ( existingFacilities.getName().equals( facilities.getName() ) ) {
				throw new RuntimeException("additional facilities under the name of + " + facilities.getName() + 
						" already exist; cannot add additional facilities under the same name twice.") ;
			}
		}

		this.additionalFacilityData.add( facilities ) ;
	}
	

	// new .... testing
	public void writeToSubdirectoryWithName(String subdirectory) {
		this.outputSubdirectory = subdirectory;
	}
	// end new
}
