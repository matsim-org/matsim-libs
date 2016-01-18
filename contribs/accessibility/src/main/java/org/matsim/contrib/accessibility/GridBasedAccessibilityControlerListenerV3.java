package org.matsim.contrib.accessibility;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.interfaces.FacilityDataExchangeInterface;
import org.matsim.contrib.accessibility.interfaces.SpatialGridDataExchangeInterface;
import org.matsim.contrib.accessibility.utils.AccessibilityRunUtils;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.contrib.matrixbasedptrouter.utils.TempDirectoryUtil;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacilities;

import com.vividsolutions.jts.geom.Geometry;

import javax.inject.Inject;

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
public final class GridBasedAccessibilityControlerListenerV3 implements ShutdownListener {
	private static final Logger log = Logger.getLogger(GridBasedAccessibilityControlerListenerV3.class);
	private final AccessibilityCalculator delegate;
	private final List<SpatialGridDataExchangeInterface> spatialGridDataExchangeListener = new ArrayList<>();

	private Scenario scenario;
	private Config config;
	private double time;
	
	// for consideration of different activity types or different modes (or both) subdirectories are
	// required in order not to confuse the output
	private String outputSubdirectory;
	private boolean urbanSimMode;
	
	
	//
	private boolean	calculateAggregateValues;
	private Map<Modes4Accessibility, Double> accessibilitySums = new HashMap<Modes4Accessibility, Double>();
	private Map<Modes4Accessibility, Double> accessibilityGiniCoefficients = new HashMap<Modes4Accessibility, Double>();
	//
	
	private SpatialGridAggregator spatialGridAggregator;

	/**
	 * constructor
	 * @param opportunities represented by ActivityFacilitiesImpl
	 * @param ptMatrix matrix with travel times and distances for any pair of pt stops
	 * @param config MATSim Config object
	 * @param scenario MATSim scenario
	 */
	public GridBasedAccessibilityControlerListenerV3(ActivityFacilities opportunities, PtMatrix ptMatrix, Config config, Scenario scenario, Map<String, TravelTime> travelTimes, Map<String, TravelDisutilityFactory> travelDisutilityFactories) {
		// I thought about changing the type of opportunities to Map<Id,Facility> or even Collection<Facility>, but in the end
		// one can also use FacilitiesUtils.createActivitiesFacilities(), put everything in there, and give that to this constructor. kai, feb'14

		log.info("Initializing  ...");
		spatialGridAggregator = new SpatialGridAggregator();
		delegate = new AccessibilityCalculator(travelTimes, travelDisutilityFactories, scenario, ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class));
		delegate.addFacilityDataExchangeListener(spatialGridAggregator);

		delegate.setPtMatrix(ptMatrix);	// this could be zero if no input files for pseudo pt are given ...
		assert (config != null);
		this.config = config ;
		this.scenario = scenario;
		delegate.initAccessibilityParameters(config);

		// aggregating facilities to their nearest node on the road network
		delegate.aggregateOpportunities(opportunities, scenario.getNetwork());
		// yyyy ignores the "capacities" of the facilities.  kai, mar'14
		
		log.info(".. done initializing CellBasedAccessibilityControlerListenerV3");
	}

	private List<ActivityFacilities> additionalFacilityData = new ArrayList<>() ;
	private Map<String,Tuple<SpatialGrid,SpatialGrid>> additionalSpatialGrids = new TreeMap<>() ;
	//(not sure if this is a bit odd ... but I always need TWO spatial grids. kai, mar'14)
	private boolean lockedForAdditionalFacilityData = false;
	
	
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		if (event.isUnexpected()) {
			return;
		}
		
		if (outputSubdirectory != null) {
			File file = new File(config.controler().getOutputDirectory() + "/" + outputSubdirectory);
			file.mkdirs();
		}
		
		UrbansimCellBasedAccessibilityCSVWriterV2 urbansimAccessibilityWriter = null;
		log.warn("here-1") ;
		if (urbanSimMode) {
			if ( outputSubdirectory != null ) {
				throw new RuntimeException("output subdirectory not null stems from separate accessibility computation per activity type.  "
						+ "This is, however, not supported on the urbansim side, so using it in the urbansim mode does not make sense.  "
						+ "Thus aborting ..." ) ;
			}
			log.warn("here0");
			urbansimAccessibilityWriter = new UrbansimCellBasedAccessibilityCSVWriterV2(config.controler().getOutputDirectory());
			delegate.addFacilityDataExchangeListener(urbansimAccessibilityWriter);
		}
		
		delegate.initDefaultContributionCalculators();

		// make sure that measuring points are set.
		if(delegate.getMeasuringPoints() == null){
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

		log.info("Computing and writing cell based accessibility measures ...");
		// printParameterSettings(); // use only for debugging (settings are printed as part of config dump)
		log.info(delegate.getMeasuringPoints().getFacilities().values().size() + " measurement points are now processing ...");

		AccessibilityConfigGroup moduleAPCM =
		ConfigUtils.addOrGetModule(
				scenario.getConfig(),
				AccessibilityConfigGroup.GROUP_NAME,
				AccessibilityConfigGroup.class);
		delegate.computeAccessibilities(scenario, moduleAPCM.getTimeOfDay());
		
		//
		// do calculation of aggregate index values, e.g. gini coefficient
		if (calculateAggregateValues) {
			performAggregateValueCalculations();
		}
		//
		
		
		// as for the other writer above: In case multiple AccessibilityControlerListeners are added to the controller, e.g. if 
		// various calculations are done for different activity types or different modes (or both) subdirectories are required
		// in order not to confuse the output
		if (outputSubdirectory == null) {
			writePlottingData(config.controler().getOutputDirectory());
		} else {
			writePlottingData(config.controler().getOutputDirectory() + "/" + outputSubdirectory);
		}

		log.info("Triggering " + spatialGridDataExchangeListener.size() + " SpatialGridDataExchangeListener(s) ...");
		for (SpatialGridDataExchangeInterface spatialGridDataExchangeInterface : spatialGridDataExchangeListener) {
			try {
				spatialGridDataExchangeInterface.setAndProcessSpatialGrids(spatialGridAggregator.getAccessibilityGrids());
			} catch ( Exception ee ) {
				log.warn("Had a problem here; printing stack trace but then continuing anyways") ;
				ee.printStackTrace(); 
			}
		}

	}

	/**
	 * This writes the accessibility grid data into the MATSim output directory
	 */
	private void writePlottingData(String adaptedOutputDirectory) {

		// in the following, the data used for gnuplot or QGis is written. dz, feb'15
		// different separators have to be used to make this output useable by gnuplot or QGis, respectively
		log.info("Writing plotting data for other analyis into " + adaptedOutputDirectory + " ...");
		

		final CSVWriter writer = new CSVWriter(adaptedOutputDirectory + "/" + CSVWriter.FILE_NAME ) ;
		
		// write header
		writer.writeField(Labels.X_COORDINATE);
		writer.writeField(Labels.Y_COORDINATE);
		writer.writeField(Labels.TIME);
		
		writer.writeField(Labels.ACCESSIBILITY_BY_FREESPEED);
		writer.writeField(Labels.ACCESSIBILITY_BY_CAR);
		writer.writeField(Labels.ACCESSIBILITY_BY_BIKE);
		writer.writeField(Labels.ACCESSIBILITY_BY_WALK);
		writer.writeField(Labels.ACCESSIBILITY_BY_PT);
		// yyyyyy the above needs to be replaced by a loop over Modes4Accessibility.values() . kai/mz, jul'15
		
		writer.writeField(Labels.POPULATION_DENSITIY);
		writer.writeField(Labels.POPULATION_DENSITIY);
		writer.writeNewLine();

		final SpatialGrid spatialGrid = spatialGridAggregator.getAccessibilityGrids().get(Modes4Accessibility.freeSpeed) ;
		// yy for time being, have to assume that this is always there
		for(double y = spatialGrid.getYmin(); y <= spatialGrid.getYmax(); y += spatialGrid.getResolution()) {
			for(double x = spatialGrid.getXmin(); x <= spatialGrid.getXmax(); x += spatialGrid.getResolution()) {
				
				writer.writeField( x + 0.5*spatialGrid.getResolution());
				writer.writeField( y + 0.5*spatialGrid.getResolution());
				
				writer.writeField(time);
				
				for (Modes4Accessibility mode : Modes4Accessibility.values()) {
					if ( delegate.getIsComputingMode().contains(mode) ) {
						final SpatialGrid spatialGridOfMode = spatialGridAggregator.getAccessibilityGrids().get(mode);
						final double value = spatialGridOfMode.getValue(x, y);
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
			// writer.writeNewLine(); // gnuplot pm3d scanline
		}
		writer.close() ;

		log.info("Writing plotting data for other analysis done!");
	}
	
	
	//
	/**
	 * perform aggregate value calculations
	 */
	private void performAggregateValueCalculations() {
		log.info("Starting to caluclating aggregate values!");
		final SpatialGrid spatialGrid = spatialGridAggregator.getAccessibilityGrids().get(Modes4Accessibility.freeSpeed) ;
		// yy for time being, have to assume that this is always there

		for (Modes4Accessibility mode : delegate.getIsComputingMode()) {
			List<Double> valueList = new ArrayList<Double>();

			for(double y = spatialGrid.getYmin(); y <= spatialGrid.getYmax() ; y += spatialGrid.getResolution()) {
				for(double x = spatialGrid.getXmin(); x <= spatialGrid.getXmax(); x += spatialGrid.getResolution()) {
					final SpatialGrid spatialGridOfMode = spatialGridAggregator.getAccessibilityGrids().get(mode);
					final double value = spatialGridOfMode.getValue(x, y);
					if ( !Double.isNaN(value ) ) {
						valueList.add(value);
					} else {
						new RuntimeException("Don't know how to calculate aggregate values properly if some are missing!");
					}
				}
			}

			double accessibilityValueSum = AccessibilityRunUtils.calculateSum(valueList);
			double giniCoefficient = AccessibilityRunUtils.calculateGiniCoefficient(valueList);

			log.warn("mode = " + mode  + " -- accessibilityValueSum = " + accessibilityValueSum);
			accessibilitySums.put(mode, accessibilityValueSum);
			log.warn("accessibilitySum = " + accessibilitySums);
			accessibilityGiniCoefficients.put(mode, giniCoefficient);
		}
		log.info("Done with caluclating aggregate values!");
	}
	//

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
		delegate.setMeasuringPoints(GridUtils.createGridLayerByGridSizeByShapeFileV2(boundary, cellSize));
		for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
			if ( delegate.getIsComputingMode().contains(mode) ) {
				spatialGridAggregator.getAccessibilityGrids().put(mode, GridUtils.createSpatialGridByShapeBoundary(boundary, cellSize)) ;
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
			throw new RuntimeException("Cell Size needs to be assigned a value greater than zero.");
		}
		BoundingBox bb = BoundingBox.createBoundingBox(scenario.getNetwork());
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
		delegate.setMeasuringPoints(GridUtils.createGridLayerByGridSizeByBoundingBoxV2(minX, minY, maxX, maxY, cellSize));
		for ( Modes4Accessibility mode : delegate.getIsComputingMode()) {
			spatialGridAggregator.getAccessibilityGrids().put(mode, new SpatialGrid(minX, minY, maxX, maxY, cellSize, Double.NaN)) ;
		}
		lockedForAdditionalFacilityData  = true ;
		for ( ActivityFacilities facilities : this.additionalFacilityData ) {
			if ( this.additionalSpatialGrids.get( facilities.getName() ) != null ) {
				throw new RuntimeException("this should not yet exist ...") ;
			}
			Tuple<SpatialGrid,SpatialGrid> spatialGrids = new Tuple<>(
					new SpatialGrid(minX, minY, maxX, maxY, cellSize, 0.), new SpatialGrid(minX, minY, maxX, maxY, cellSize, 0.)) ;
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
	
	
	/**
	 * Using this method changes the folder structure of the output. The output of the calculation will be written into the
	 * subfolder. This is needed if for than one ContolerListener is added since otherwise the output would be overwritten
	 * and not be available for analyses anymore.
	 * 
	 * @param subdirectory the name of the subdirectory
	 */
	public void writeToSubdirectoryWithName(String subdirectory) {
		this.outputSubdirectory = subdirectory;
	}

	
	public void setComputingAccessibilityForMode(Modes4Accessibility mode, boolean val) {
		delegate.setComputingAccessibilityForMode(mode, val);
	}

	
	public void addSpatialGridDataExchangeListener(SpatialGridDataExchangeInterface l) {
		this.spatialGridDataExchangeListener.add(l);
	}
	
	
	public final void addFacilityDataExchangeListener(FacilityDataExchangeInterface listener) {
		this.delegate.addFacilityDataExchangeListener(listener);
	}

	
	public void setUrbansimMode(boolean urbansimMode) {
		this.urbanSimMode = urbansimMode;
	}
	
	
	public void setTime(double time) {
		this.time = time;
	}
	
	
	public void setCalculateAggregateValues(boolean calculateAggregateValues) {
		this.calculateAggregateValues = calculateAggregateValues;
	}
	
	
	public Map<Modes4Accessibility, Double> getAccessibilitySums() {
		return this.accessibilitySums;
	}
	
	
	public Map<Modes4Accessibility, Double> getAccessibilityGiniCoefficients() {
		return this.accessibilityGiniCoefficients;
	}
}