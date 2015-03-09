package playground.andreas.aas.modules.cellBasedAccessibility;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;

import playground.andreas.aas.modules.cellBasedAccessibility.constants.InternalConstants;
import playground.andreas.aas.modules.cellBasedAccessibility.costcalculators.FreeSpeedTravelTimeCostCalculator;
import playground.andreas.aas.modules.cellBasedAccessibility.costcalculators.TravelDistanceCalculator;
import playground.andreas.aas.modules.cellBasedAccessibility.costcalculators.TravelTimeCostCalculator;
import playground.andreas.aas.modules.cellBasedAccessibility.gis.GridUtils;
import playground.andreas.aas.modules.cellBasedAccessibility.gis.SpatialGrid;
import playground.andreas.aas.modules.cellBasedAccessibility.gis.Zone;
import playground.andreas.aas.modules.cellBasedAccessibility.gis.ZoneLayer;
import playground.andreas.aas.modules.cellBasedAccessibility.interpolation.Interpolation;
import playground.andreas.aas.modules.cellBasedAccessibility.utils.helperObjects.AggregateObject2NearestNode;
import playground.andreas.aas.modules.cellBasedAccessibility.utils.helperObjects.Benchmark;
import playground.andreas.aas.modules.cellBasedAccessibility.utils.io.writer.AnalysisCellBasedAccessibilityCSVWriterV2;
import playground.andreas.aas.modules.cellBasedAccessibility.utils.io.writer.UrbanSimParcelCSVWriter;
import playground.andreas.aas.modules.cellBasedAccessibility.utils.misc.ProgressBar;
import playground.andreas.aas.modules.cellBasedAccessibility.utils.network.NetworkUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

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
 * @author thomas
 * 
 */
public class CellBasedAccessibilityControlerListenerV3 extends AccessibilityControlerListenerImpl implements ShutdownListener{ // implements ShutdownListener
	
	private static final Logger log = Logger.getLogger(CellBasedAccessibilityControlerListenerV3.class);
	
	// ////////////////////////////////////////////////////////////////////
	// constructors
	// ////////////////////////////////////////////////////////////////////
	
	public CellBasedAccessibilityControlerListenerV3(ZoneLayer<Id> startZones, 						// needed for google earth plots (not supported by now tnicolai feb'12)
													 AggregateObject2NearestNode[] aggregatedOpportunities, 	// destinations (like workplaces)
													 ActivityFacilitiesImpl parcels,							// parcel coordinates for accessibility feedback
													 SpatialGrid freeSpeedGrid,
													 SpatialGrid carGrid, 										// table for congested car travel times in accessibility computation
													 SpatialGrid bikeGrid,										// table for bike travel times in accessibility computation
													 SpatialGrid walkGrid, 										// table for walk travel times in accessibility computation
													 String fileExtension,										// adds an extension to output files whether a shape-file or network boundaries are used for calculation
													 Benchmark benchmark,										// Benchmark tool
													 ScenarioImpl scenario){	
		log.info("Initializing CellBasedAccessibilityControlerListenerV3 ...");
		
		assert (startZones != null);
		this.measuringPointsCell = startZones;
		assert (aggregatedOpportunities != null);
		this.aggregatedOpportunities = aggregatedOpportunities;
		assert (parcels != null);
		this.parcels = parcels;
		assert (freeSpeedGrid != null);
		this.freeSpeedGrid = freeSpeedGrid;
		assert (carGrid != null);
		this.carGrid = carGrid;
		assert (bikeGrid != null);
		this.bikeGrid = bikeGrid;
		assert (walkGrid != null);
		this.walkGrid = walkGrid;
		assert (fileExtension != null);
		CellBasedAccessibilityControlerListenerV3.fileExtension = fileExtension;
		assert (benchmark != null);
		this.benchmark = benchmark;

		// writing accessibility measures continuously into a csv file, which is not 
		// dedicated for as input for UrbanSim, but for analysis purposes
		AnalysisCellBasedAccessibilityCSVWriterV2.initAnalysisCellBasedAccessibilityCSVWriterV2(fileExtension);
		
		initAccessibilityParameter(scenario);
		log.info(".. done initializing CellBasedAccessibilityControlerListenerV3");
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event){
		log.info("Entering notifyShutdown ..." );
		
		int benchmarkID = this.benchmark.addMeasure("cell-based accessibility computation");
		
		// get the controller and scenario
		Controler controler = event.getControler();
		
		TravelTime ttc = controler.getLinkTravelTimes();
		// get the free-speed car travel times (in seconds)
		LeastCostPathTree lcptFreeSpeedCarTravelTime = new LeastCostPathTree( ttc, new FreeSpeedTravelTimeCostCalculator() );
		// get the congested car travel time (in seconds)
		LeastCostPathTree lcptCongestedCarTravelTime = new LeastCostPathTree( ttc, new TravelTimeCostCalculator(ttc) );
		// get travel distance (in meter)
		LeastCostPathTree lcptTravelDistance		 = new LeastCostPathTree( ttc, new TravelDistanceCalculator());

        NetworkImpl network = (NetworkImpl) controler.getScenario().getNetwork();

		try{
			log.info("Computing and writing cell based accessibility measures ...");
			printParameterSettings();
			
			Iterator<Zone<Id>> measuringPointIterator = measuringPointsCell.getZones().iterator();
			log.info(measuringPointsCell.getZones().size() + " measurement points are now processing ...");
			
			accessibilityComputation(ttc, lcptFreeSpeedCarTravelTime,
					lcptCongestedCarTravelTime, lcptTravelDistance, network,
					measuringPointIterator, measuringPointsCell.getZones().size(),
					CELL_BASED);
			
			System.out.println();

			if (this.benchmark != null && benchmarkID > 0) {
				this.benchmark.stoppMeasurement(benchmarkID);
				log.info("Accessibility computation with "
						+ measuringPointsCell.getZones().size()
						+ " starting points (origins) and "
						+ this.aggregatedOpportunities.length
						+ " destinations (opportunities) took "
						+ this.benchmark.getDurationInSeconds(benchmarkID)
						+ " seconds ("
						+ this.benchmark.getDurationInSeconds(benchmarkID)
						/ 60. + " minutes).");
			}
			// tnicolai: for debugging (remove for release)
			log.info("Euclidian vs Othogonal Distance:");
			log.info("Total Counter:" + NetworkUtil.totalCounter);
			log.info("Euclidian Counter:" + NetworkUtil.euclidianCounter);
			log.info("Othogonal Counter:" + NetworkUtil.othogonalCounter);
			
			AnalysisCellBasedAccessibilityCSVWriterV2.close(); 
			writePlottingData();						// plotting data for visual analysis via R
			writeInterpolatedParcelAccessibilities();	// UrbanSim input file with interpolated accessibilities on parcel level
		
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
			Zone<Id> measurePoint, Coord coordFromZone,
			Node fromNode, double freeSpeedAccessibility,
			double carAccessibility, double bikeAccessibility,
			double walkAccessibility) {
		// writing accessibility values (stored in startZone object) in csv format ...
		AnalysisCellBasedAccessibilityCSVWriterV2.write(measurePoint,
				coordFromZone, fromNode, freeSpeedAccessibility,
				carAccessibility, bikeAccessibility, walkAccessibility);
	}

	// This needs to be executed only once at the end of the accessibility computation
	// A synchronization is may be not needed
	private void writePlottingData() throws IOException{
		
		log.info("Writing plotting files ...");
		// tnicolai: can be disabled for final release
		GridUtils.writeSpatialGridTable(freeSpeedGrid, InternalConstants.MATSIM_4_OPUS_TEMP	// freespeed results for plotting in R
				+ "freeSpeedAccessibility_cellsize_" + freeSpeedGrid.getResolution()
				+ CellBasedAccessibilityControlerListenerV3.fileExtension
				+ InternalConstants.FILE_TYPE_TXT);
		// tnicolai: can be disabled for final release
		GridUtils.writeSpatialGridTable(carGrid, InternalConstants.MATSIM_4_OPUS_TEMP	// car results for plotting in R
				+ "carAccessibility_cellsize_" + carGrid.getResolution()
				+ CellBasedAccessibilityControlerListenerV3.fileExtension
				+ InternalConstants.FILE_TYPE_TXT);
		// tnicolai: can be disabled for final release
		GridUtils.writeSpatialGridTable(bikeGrid, InternalConstants.MATSIM_4_OPUS_TEMP	// car results for plotting in R
				+ "bikeAccessibility_cellsize_" + bikeGrid.getResolution()
				+ CellBasedAccessibilityControlerListenerV3.fileExtension
				+ InternalConstants.FILE_TYPE_TXT);
		// tnicolai: can be disabled for final release
		GridUtils.writeSpatialGridTable(walkGrid, InternalConstants.MATSIM_4_OPUS_TEMP	// walk results for plotting in R
				+ "walkAccessibility_cellsize_" + walkGrid.getResolution()
				+ CellBasedAccessibilityControlerListenerV3.fileExtension
				+ InternalConstants.FILE_TYPE_TXT);

		// tnicolai: google earth outputs can be left in final release since
		// this can be used for a quick analysis without any further scripts or
		// so...
		GridUtils.writeKMZFiles(measuringPointsCell,								// car results for google earth
							freeSpeedGrid,
							InternalConstants.MATSIM_4_OPUS_TEMP
										+ "freeSpeedAccessibility_cellsize_"
										+ freeSpeedGrid.getResolution()
										+ CellBasedAccessibilityControlerListenerV3.fileExtension
										+ InternalConstants.FILE_TYPE_KMZ);
		GridUtils.writeKMZFiles(measuringPointsCell,								// car results for google earth
								carGrid,
								InternalConstants.MATSIM_4_OPUS_TEMP
										+ "carAccessibility_cellsize_"
										+ carGrid.getResolution()
										+ CellBasedAccessibilityControlerListenerV3.fileExtension
										+ InternalConstants.FILE_TYPE_KMZ);
		GridUtils.writeKMZFiles(measuringPointsCell,								// bike results for google earth
								bikeGrid,
								InternalConstants.MATSIM_4_OPUS_TEMP
										+ "bikeAccessibility_cellsize_"
										+ bikeGrid.getResolution()
										+ CellBasedAccessibilityControlerListenerV3.fileExtension
										+ InternalConstants.FILE_TYPE_KMZ);
		GridUtils.writeKMZFiles(measuringPointsCell,								// walk results for google earth
								walkGrid,
								InternalConstants.MATSIM_4_OPUS_TEMP
										+ "walkAccessibility_cellsize_"
										+ walkGrid.getResolution()
										+ CellBasedAccessibilityControlerListenerV3.fileExtension
										+ InternalConstants.FILE_TYPE_KMZ);
		log.info("Writing plotting files done!");
	}
	
	/**
	 * 
	 */
	private void writeInterpolatedParcelAccessibilities() {
		// from here accessibility feedback for each parcel
		UrbanSimParcelCSVWriter.initUrbanSimZoneWriter();
		
		Interpolation freeSpeedGridInterpolation = new Interpolation(freeSpeedGrid, Interpolation.BILINEAR);
		Interpolation carGridInterpolation = new Interpolation(carGrid, Interpolation.BILINEAR);
		Interpolation bikeGridInterpolation= new Interpolation(bikeGrid, Interpolation.BILINEAR);
		Interpolation walkGridInterpolation= new Interpolation(walkGrid, Interpolation.BILINEAR);
		
		if(this.parcels != null){
			
			int numberOfParcels = this.parcels.getFacilities().size();
			double freeSpeedAccessibility = Double.NaN;
			double carAccessibility = Double.NaN;
			double bikeAccessibility= Double.NaN;
			double walkAccessibility= Double.NaN;
			
			log.info(numberOfParcels + " parcels are now processing ...");
			
			Iterator<? extends ActivityFacility> parcelIterator = this.parcels.getFacilities().values().iterator();
			ProgressBar bar = new ProgressBar( numberOfParcels );
			
			while(parcelIterator.hasNext()){
				
				bar.update();
				
				ActivityFacility parcel = parcelIterator.next();
				
				// for testing
				// double car = carGrid.getValue(parcel.getCoord().getX(), parcel.getCoord().getY());
				// double walk= walkGrid.getValue(parcel.getCoord().getX(), parcel.getCoord().getY());
				
				freeSpeedAccessibility = freeSpeedGridInterpolation.interpolate( parcel.getCoord() );
				carAccessibility = carGridInterpolation.interpolate( parcel.getCoord() );
				bikeAccessibility = bikeGridInterpolation.interpolate( parcel.getCoord() );
				walkAccessibility= walkGridInterpolation.interpolate( parcel.getCoord() );
				
				UrbanSimParcelCSVWriter.write(parcel.getId(), freeSpeedAccessibility, carAccessibility, bikeAccessibility, walkAccessibility);
			}
			log.info("... done!");
			UrbanSimParcelCSVWriter.close();
		}
	}
}
