package org.matsim.contrib.accessibility;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.interfaces.SpatialGridDataExchangeInterface;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacilities;

import com.vividsolutions.jts.geom.Envelope;

/**
 * @author thomas, dziemke
 */
public final class GridBasedAccessibilityShutdownListenerV3 implements ShutdownListener {
	private static final Logger log = Logger.getLogger(GridBasedAccessibilityShutdownListenerV3.class);
		

	private final AccessibilityCalculator accessibilityCalculator;
	private final List<SpatialGridDataExchangeInterface> spatialGridDataExchangeListener = new ArrayList<>();

	private final Scenario scenario;
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
	
	final double xMin, yMin, xMax, yMax, cellSize;
	
	private SpatialGridAggregator spatialGridAggregator;


	private PtMatrix ptMatrix;


	private ActivityFacilities opportunities;

	/**
	 * constructor
	 * @param opportunities represented by ActivityFacilitiesImpl
	 * @param ptMatrix matrix with travel times and distances for any pair of pt stops
	 * @param config MATSim Config object
	 * @param scenario MATSim scenario
	 */
	public GridBasedAccessibilityShutdownListenerV3(AccessibilityCalculator accessibilityCalculator, ActivityFacilities opportunities, PtMatrix ptMatrix, Config config, Scenario scenario, Map<String, TravelTime> travelTimes, Map<String, TravelDisutilityFactory> travelDisutilityFactories, double xMin, double yMin, double xMax, double yMax, double cellSize) {
		this.xMin = xMin;
		this.yMin = yMin;
		this.xMax = xMax;
		this.yMax = yMax;
		this.cellSize = cellSize;
		this.ptMatrix = ptMatrix;
		this.opportunities = opportunities;
		this.accessibilityCalculator = accessibilityCalculator;
		this.scenario = scenario;
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
		
		// I thought about changing the type of opportunities to Map<Id,Facility> or even Collection<Facility>, but in the end
		// one can also use FacilitiesUtils.createActivitiesFacilities(), put everything in there, and give that to this constructor. kai, feb'14

		log.info("Initializing  ...");
		spatialGridAggregator = new SpatialGridAggregator();
		this.accessibilityCalculator.addFacilityDataExchangeListener(spatialGridAggregator);

		if (ptMatrix != null) {
			this.accessibilityCalculator.setPtMatrix(ptMatrix);	// this could be zero if no input files for pseudo pt are given ...
		}
		
		log.info(".. done initializing CellBasedAccessibilityControlerListenerV3");
		for ( Modes4Accessibility mode : accessibilityCalculator.getIsComputingMode()) {
			spatialGridAggregator.getAccessibilityGrids().put(mode, new SpatialGrid(xMin, yMin, xMax, yMax, cellSize, Double.NaN)) ;
		}
		
		lockedForAdditionalFacilityData  = true ;
		for ( ActivityFacilities facilities : this.additionalFacilityData ) {
			if ( this.additionalSpatialGrids.get( facilities.getName() ) != null ) {
				throw new RuntimeException("this should not yet exist ...") ;
			}
			Tuple<SpatialGrid,SpatialGrid> spatialGrids = new Tuple<>(
					new SpatialGrid(xMin, yMin, xMax, yMax, cellSize, 0.), new SpatialGrid(xMin, yMin, xMax, yMax, cellSize, 0.)) ;
			this.additionalSpatialGrids.put( facilities.getName(), spatialGrids ) ;
		}
		
		if (outputSubdirectory != null) {
			File file = new File(scenario.getConfig().controler().getOutputDirectory() + "/" + outputSubdirectory);
			file.mkdirs();
		}
		
		UrbansimCellBasedAccessibilityCSVWriterV2 urbansimAccessibilityWriter = null;
		if (urbanSimMode) {
			if ( outputSubdirectory != null ) {
				throw new RuntimeException("output subdirectory not null stems from separate accessibility computation per activity type.  "
						+ "This is, however, not supported on the urbansim side, so using it in the urbansim mode does not make sense.  "
						+ "Thus aborting ..." ) ;
			}
			log.warn("here0");
			urbansimAccessibilityWriter = new UrbansimCellBasedAccessibilityCSVWriterV2(scenario.getConfig().controler().getOutputDirectory());
			accessibilityCalculator.addFacilityDataExchangeListener(urbansimAccessibilityWriter);
		}

		// prepare the additional columns:
		for ( ActivityFacilities facilities : this.additionalFacilityData ) {
			Tuple<SpatialGrid,SpatialGrid> spatialGrids = this.additionalSpatialGrids.get( facilities.getName() ) ;
			GridUtils.aggregateFacilitiesIntoSpatialGrid(facilities, spatialGrids.getFirst(), spatialGrids.getSecond());
		}

		log.info("Computing and writing cell based accessibility measures ...");
		// printParameterSettings(); // use only for debugging (settings are printed as part of config dump)

		AccessibilityConfigGroup moduleAPCM =
		ConfigUtils.addOrGetModule(
				scenario.getConfig(),
				AccessibilityConfigGroup.GROUP_NAME,
				AccessibilityConfigGroup.class);
		accessibilityCalculator.computeAccessibilities(moduleAPCM.getTimeOfDay(), opportunities);
		
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
			writePlottingData(scenario.getConfig().controler().getOutputDirectory());
		} else {
			writePlottingData(scenario.getConfig().controler().getOutputDirectory() + "/" + outputSubdirectory);
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
//		writer.writeField(Labels.TIME); // TODO
		for (Modes4Accessibility mode : Modes4Accessibility.values()) {
			writer.writeField(mode.toString() + "_accessibility");
		}
		writer.writeField(Labels.POPULATION_DENSITIY); // TODO I think this has to be made adjustable
		writer.writeField(Labels.POPULATION_DENSITIY);
		writer.writeNewLine();

		final SpatialGrid spatialGrid = spatialGridAggregator.getAccessibilityGrids().get(Modes4Accessibility.freeSpeed) ;
		// yy for time being, have to assume that this is always there
		for(double y = spatialGrid.getYmin(); y <= spatialGrid.getYmax(); y += spatialGrid.getResolution()) {
			for(double x = spatialGrid.getXmin(); x <= spatialGrid.getXmax(); x += spatialGrid.getResolution()) {
				
				writer.writeField( x + 0.5*spatialGrid.getResolution());
				writer.writeField( y + 0.5*spatialGrid.getResolution());
				
//				writer.writeField(time); // TODO
				
				for (Modes4Accessibility mode : Modes4Accessibility.values()) {
					if ( accessibilityCalculator.getIsComputingMode().contains(mode) ) {
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
	
	
	// new idea
	/**
	 * perform aggregate value calculations
	 */
	private void performAggregateValueCalculations() {
		log.info("Starting to caluclating aggregate values!");
		final SpatialGrid spatialGrid = spatialGridAggregator.getAccessibilityGrids().get(Modes4Accessibility.freeSpeed) ;
		// yy for time being, have to assume that this is always there

		for (Modes4Accessibility mode : accessibilityCalculator.getIsComputingMode()) {
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

			double accessibilityValueSum = AccessibilityUtils.calculateSum(valueList);
			double giniCoefficient = AccessibilityUtils.calculateGiniCoefficient(valueList);

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

	
	public void addSpatialGridDataExchangeListener(SpatialGridDataExchangeInterface l) {
		this.spatialGridDataExchangeListener.add(l);
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