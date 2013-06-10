package org.matsim.contrib.accessibility;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.costcalculator.TravelDistanceCalculator;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.utils.Benchmark;
import org.matsim.contrib.accessibility.utils.LeastCostPathTreeExtended;
import org.matsim.contrib.accessibility.utils.io.writer.AnalysisCellBasedAccessibilityCSVWriterV2;
import org.matsim.contrib.improvedPseudoPt.PtMatrix;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.utils.LeastCostPathTree;

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
 * 
 * @author thomas
 * 
 */
public class GridBasedAccessibilityControlerListenerV3 extends AccessibilityControlerListenerImpl implements ShutdownListener{ // implements ShutdownListener
	
	private static final Logger log = Logger.getLogger(GridBasedAccessibilityControlerListenerV3.class);
	
	// ////////////////////////////////////////////////////////////////////
	// constructors
	// ////////////////////////////////////////////////////////////////////
	
	public GridBasedAccessibilityControlerListenerV3(ActivityFacilitiesImpl measuringPoints,
													 ActivityFacilitiesImpl opportunities,
													 SpatialGrid freeSpeedGrid,									// table for free speed car travel times in accessibility computation
													 SpatialGrid carGrid, 										// table for congested car travel times in accessibility computation
													 SpatialGrid bikeGrid,										// table for bike travel times in accessibility computation
													 SpatialGrid walkGrid, 										// table for walk travel times in accessibility computation
													 SpatialGrid ptGrid,
													 PtMatrix ptMatrix,
													 Config config, 
													 Network network){
								
		log.info("Initializing ParcelBasedAccessibilityControlerListenerV3 ...");

		assert (measuringPoints != null);
		this.measuringPoints = measuringPoints;
		assert (freeSpeedGrid != null);
		this.freeSpeedGrid = freeSpeedGrid;
		assert (carGrid != null);
		this.carGrid = carGrid;
		assert (bikeGrid != null);
		this.bikeGrid = bikeGrid;
		assert (walkGrid != null);
		this.walkGrid = walkGrid;
		assert (ptGrid != null);
		this.ptGrid = ptGrid;
		this.ptMatrix = ptMatrix;	// this could be zero of no input files for pseudo pt are given ...
		assert (config != null);
		assert (network != null);

		this.benchmark = new Benchmark();
		
		// writing accessibility measures continuously into a csv file, which is not 
		// dedicated for as input for UrbanSim, but for analysis purposes
		String matsimOutputDirectory = config.controler().getOutputDirectory();
		AnalysisCellBasedAccessibilityCSVWriterV2.initAnalysisCellBasedAccessibilityCSVWriterV2(matsimOutputDirectory);
		initAccessibilityParameters(config);
		// aggregating facilities to their nearest node on the road network
		this.aggregatedFacilities = aggregatedOpportunities(opportunities, network);
		
		log.info(".. done initializing CellBasedAccessibilityControlerListenerV3");
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event){
		log.info("Entering notifyShutdown ..." );
		
		// get the controller and scenario
		Controler controler = event.getControler();
		NetworkImpl network = (NetworkImpl) controler.getNetwork();
		
		int benchmarkID = this.benchmark.addMeasure("cell-based accessibility computation");

		// get the free-speed car travel times (in seconds)
		TravelTime ttf = new FreeSpeedTravelTime() ;
		TravelDisutility tdFree = controler.getTravelDisutilityFactory().createTravelDisutility(ttf, controler.getConfig().planCalcScore() ) ;
		LeastCostPathTreeExtended lcptExtFreeSpeedCarTrvelTime = new LeastCostPathTreeExtended( ttf, tdFree, controler.getScenario().getScenarioElement(RoadPricingSchemeImpl.class) ) ;

		// get the congested car travel time (in seconds)
		TravelTime ttc = controler.getLinkTravelTimes(); // congested
		TravelDisutility tdCongested = controler.getTravelDisutilityFactory().createTravelDisutility(ttc, controler.getConfig().planCalcScore() ) ;
		LeastCostPathTreeExtended  lcptExtCongestedCarTravelTime = new LeastCostPathTreeExtended(ttc, tdCongested, controler.getScenario().getScenarioElement(RoadPricingSchemeImpl.class) ) ;

		// get travel distance (in meter)
		LeastCostPathTree lcptTravelDistance		 = new LeastCostPathTree( ttf, new TravelDistanceCalculator());
		
		this.scheme = controler.getScenario().getScenarioElement(RoadPricingSchemeImpl.class);

		try{
			log.info("Computing and writing cell based accessibility measures ...");
			// printParameterSettings(); // use only for debugging (settings are printed as part of config dump)
			log.info(measuringPoints.getFacilities().values().size() + " measurement points are now processing ...");
			
			accessibilityComputation(ttc, 
									 lcptExtFreeSpeedCarTrvelTime,
									 lcptExtCongestedCarTravelTime, 
									 lcptTravelDistance, 
									 ptMatrix,
									 network,
									 measuringPoints,
									 PARCEL_BASED,
									 controler);
			
			
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
			
			AnalysisCellBasedAccessibilityCSVWriterV2.close(); 
			writePlottingData(matsimOutputDirectory);			
			
			if(this.spatialGridDataExchangeListenerList != null){
				log.info("Triggering " + this.spatialGridDataExchangeListenerList.size() + " SpatialGridDataExchangeListener(s) ...");
				for(int i = 0; i < this.spatialGridDataExchangeListenerList.size(); i++)
					this.spatialGridDataExchangeListenerList.get(i).getAndProcessSpatialGrids(freeSpeedGrid, carGrid, bikeGrid, walkGrid, ptGrid);
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param accCsvWriter
	 * @param measurePoint
	 * @param coordFromZone
	 * @param fromNode
	 * @param freeSpeedAccessibility
	 * @param carAccessibility
	 * @param bikeAccessibility
	 * @param walkAccessibility
	 */
	@Override
	protected void writeCSVData(
			ActivityFacility measurePoint, Coord coordFromZone,
			Node fromNode, double freeSpeedAccessibility,
			double carAccessibility, double bikeAccessibility,
			double walkAccessibility, double ptAccessibility) {
		// writing accessibility values (stored in startZone object) in csv format ...
		AnalysisCellBasedAccessibilityCSVWriterV2.write(measurePoint, coordFromZone, fromNode, freeSpeedAccessibility,
														carAccessibility, bikeAccessibility, walkAccessibility, ptAccessibility);
//		log.info(measurePoint + " " + coordFromZone + " " + fromNode);
	}

	/**
	 * This writes the accessibility grid data into the MATSim output directory
	 * 
	 * @param matsimOutputDirectory
	 * @throws IOException
	 */
	private void writePlottingData(String matsimOutputDirectory) throws IOException{
		
		final String FILE_TYPE_TXT = ".txt";

		log.info("Writing plotting data for R analyis into " + matsimOutputDirectory + " ...");
		// tnicolai: can be disabled for final release
		GridUtils.writeSpatialGridTable(freeSpeedGrid, matsimOutputDirectory
				+ FREESEED_FILENAME + freeSpeedGrid.getResolution()
				+ FILE_TYPE_TXT);
		// tnicolai: can be disabled for final release
		GridUtils.writeSpatialGridTable(carGrid, matsimOutputDirectory
				+ CAR_FILENAME + carGrid.getResolution()
				+ FILE_TYPE_TXT);
		// tnicolai: can be disabled for final release
		GridUtils.writeSpatialGridTable(bikeGrid, matsimOutputDirectory
				+ BIKE_FILENAME + bikeGrid.getResolution()
				+ FILE_TYPE_TXT);
		// tnicolai: can be disabled for final release
		GridUtils.writeSpatialGridTable(walkGrid, matsimOutputDirectory
				+ WALK_FILENAME + walkGrid.getResolution()
				+ FILE_TYPE_TXT);
		// tnicolai: can be disabled for final release
		GridUtils.writeSpatialGridTable(ptGrid, matsimOutputDirectory
				+ PT_FILENAME + ptGrid.getResolution()
				+ FILE_TYPE_TXT);

		log.info("Writing plotting data for R done!");
	}
}
