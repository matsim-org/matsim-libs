package playground.tnicolai.matsim4opus.matsim4urbansim;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.utils.LeastCostPathTree;

import playground.tnicolai.matsim4opus.constants.InternalConstants;
import playground.tnicolai.matsim4opus.gis.GridUtils;
import playground.tnicolai.matsim4opus.gis.SpatialGrid;
import playground.tnicolai.matsim4opus.gis.Zone;
import playground.tnicolai.matsim4opus.gis.ZoneLayer;
import playground.tnicolai.matsim4opus.interpolation.Interpolation;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.FreeSpeedTravelTimeCostCalculator;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelDistanceCalculator;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelTimeCostCalculator;
import playground.tnicolai.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.helperObjects.Distances;
import playground.tnicolai.matsim4opus.utils.io.writer.AnalysisCellBasedAccessibilityCSVWriterV2;
import playground.tnicolai.matsim4opus.utils.io.writer.UrbanSimParcelCSVWriter;
import playground.tnicolai.matsim4opus.utils.misc.ProgressBar;
import playground.tnicolai.matsim4opus.utils.network.NetworkUtil;

import com.vividsolutions.jts.geom.Point;

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
public class CellBasedAccessibilityControlerListenerV2 extends AccessibilityControlerListenerImpl implements ShutdownListener{ // implements ShutdownListener
	
	private static final Logger log = Logger.getLogger(CellBasedAccessibilityControlerListenerV2.class);
	
	/**
	 * constructor
	 */
	public CellBasedAccessibilityControlerListenerV2(ZoneLayer<Id> startZones, 						// needed for google earth plots (not supported by now tnicolai feb'12)
													 AggregateObject2NearestNode[] aggregatedOpportunities, 	// destinations (like workplaces)
													 ActivityFacilitiesImpl parcels,							// parcel coordinates for accessibility feedback
													 SpatialGrid freeSpeedGrid,
													 SpatialGrid carGrid, 										// table for congested car travel times in accessibility computation
													 SpatialGrid bikeGrid,										// table for bike travel times in accessibility computation
													 SpatialGrid walkGrid, 										// table for walk travel times in accessibility computation
													 String fileExtension,										// adds an extension to output files whether a shape-file or network boundaries are used for calculation
													 Benchmark benchmark,										// Benchmark tool
													 ScenarioImpl scenario){	
		log.info("Initializing CellBasedAccessibilityControlerListenerV2 ...");
		
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
		assert (benchmark != null);
		this.benchmark = benchmark;

		// writing accessibility measures continuously into a csv file, which is not 
		// dedicated for as input for UrbanSim, but for analysis purposes
		AnalysisCellBasedAccessibilityCSVWriterV2.initAnalysisCellBasedAccessibilityCSVWriterV2();
		
		initAccessibilityParameter(scenario);
		log.info(".. done initializing CellBasedAccessibilityControlerListenerV2!");
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event){
		log.info("Entering notifyShutdown ..." );
		
		int benchmarkID = this.benchmark.addMeasure("1-Point accessibility computation");
		
		// get the controller and scenario
		Controler controler = event.getControler();
		
		TravelTime ttc = controler.getLinkTravelTimes();
		// get the free-speed car travel times (in seconds)
		LeastCostPathTree lcptFreeSpeedCarTravelTime = new LeastCostPathTree( ttc, new FreeSpeedTravelTimeCostCalculator() );
		// get the congested car travel time (in seconds)
		LeastCostPathTree lcptCongestedCarTravelTime = new LeastCostPathTree( ttc, new TravelTimeCostCalculator(ttc) );
		// get travel distance (in meter)
		LeastCostPathTree lcptTravelDistance		 = new LeastCostPathTree( ttc, new TravelDistanceCalculator());
		
		NetworkImpl network = (NetworkImpl) controler.getNetwork();
		double inverseOfLogitScaleParameter = 1/(logitScaleParameter); // logitScaleParameter = same as brainExpBeta on 2-aug-12. kai

		try{
			log.info("Computing and writing cell based accessibility measures ...");
			printParameterSettings();
			
			Iterator<Zone<Id>> measuringPointIterator = measuringPointsCell.getZones().iterator();
			log.info(measuringPointsCell.getZones().size() + " measurement points are now processing ...");
			
			ProgressBar bar = new ProgressBar( measuringPointsCell.getZones().size() );
			
			GeneralizedCostSum gcs = new GeneralizedCostSum();
			
//			// tnicolai: only for testing, disable afterwards
//			ZoneLayer<Id> testSet = createTestPoints();
//			measuringPointIterator = testSet.getZones().iterator();
		
			// iterates through all starting points (fromZone) and calculates their accessibility, e.g. to jobs
			while( measuringPointIterator.hasNext() ){
				
				bar.update();
				
				Zone<Id> measurePoint = measuringPointIterator.next();
				
				Point point = measurePoint.getGeometry().getCentroid();
				// get coordinate from origin (start point)
				Coord coordFromZone = new CoordImpl( point.getX(), point.getY());
				assert( coordFromZone!=null );
				
				// from here: accessibility computation for current starting point ("fromNode")
				
				// captures the distance (as walk time) between a cell centroid and the road network
				Link nearestLink = network.getNearestLinkExactly(coordFromZone);

				// determine nearest network node (from- or toNode) based on the link 
				Node fromNode = NetworkUtil.getNearestNode(coordFromZone, nearestLink);
				assert( fromNode != null );
				// run dijkstra on network
				lcptFreeSpeedCarTravelTime.calculate(network, fromNode, depatureTime);
				lcptCongestedCarTravelTime.calculate(network, fromNode, depatureTime);		
				lcptTravelDistance.calculate(network, fromNode, depatureTime);
				
				// captures the distance (as walk time) between a zone centroid and its nearest node
				
				Distances distance = NetworkUtil.getDistance2NodeV2(nearestLink, point, fromNode);
				
				double distanceMeasuringPoint2Road_meter 	= distance.getDisatancePoint2Road(); // distance measuring point 2 road (link or node)
				double distanceRoad2Node_meter 				= distance.getDistanceRoad2Node();	 // distance intersection 2 node (only for orthogonal distance)
				
				double offsetWalkTime2Node_h 				= distanceMeasuringPoint2Road_meter / this.walkSpeedMeterPerHour;
				double carTravelTime_meterpersec			= nearestLink.getLength() / ttc.getLinkTravelTime(nearestLink, depatureTime, null, null);
				double freeSpeedTravelTime_meterpersec 		= nearestLink.getFreespeed();
				
				double offsetFreeSpeedTime_h				= distanceRoad2Node_meter / (freeSpeedTravelTime_meterpersec * 3600);
				double offsetCongestedCarTime_h 			= distanceRoad2Node_meter / (carTravelTime_meterpersec * 3600.);
				double offsetBikeTime_h						= distanceRoad2Node_meter / this.bikeSpeedMeterPerHour;
				

				// Possible offsets to calculate the gap between measuring (start) point and start node (fromNode)
				// Euclidean Distance (measuring point 2 nearest node):
				// double walkTimeOffset_min = NetworkUtil.getEuclideanDistanceAsWalkTimeInSeconds(coordFromZone, fromNode.getCoord()) / 60.;
				// Orthogonal Distance (measuring point 2 nearest link, does not include remaining distance between link intersection and nearest node)
				// LinkImpl nearestLink = network.getNearestLink( coordFromZone );
				// double walkTimeOffset_min = (nearestLink.calcDistance( coordFromZone ) / this.walkSpeedMeterPerMin); 
				// or use NetworkUtil.getOrthogonalDistance(link, point) instead!
				
				gcs.reset();

				// goes through all opportunities, e.g. jobs, (nearest network node) and calculate the accessibility
				for ( int i = 0; i < this.aggregatedOpportunities.length; i++ ) {
					
					// add the avg. distance of all aggregated opportunities (euclidiean distance from nearest node to opportunity)
					double averageDistanceRoad2Opportunitiy_meter = this.aggregatedOpportunities[i].getAverageDistance();
					double offsetWalkTime2Opportunity_h = averageDistanceRoad2Opportunitiy_meter / this.walkSpeedMeterPerHour;
					
					// get stored network node (this is the nearest node next to an aggregated work place)
					Node destinationNode = this.aggregatedOpportunities[i].getNearestNode();
					Id nodeID = destinationNode.getId();
					
					// using number of aggregated opportunities as weight for log sum measure
					// int opportunityWeight = this.aggregatedOpportunities[i].getNumberOfObjects();

					// free speed car travel times in hours
					double freeSpeedTravelTime_h = (lcptFreeSpeedCarTravelTime.getTree().get( nodeID ).getCost() / 3600.) + offsetFreeSpeedTime_h;
					// travel distance in meter
					double travelDistance_meter = lcptTravelDistance.getTree().get( nodeID ).getCost();
					// bike travel times in hours
					double bikeTravelTime_h 	= (travelDistance_meter / this.bikeSpeedMeterPerHour) + offsetBikeTime_h; // using a constant speed of 15km/h
					// walk travel times in hours
					double walkTravelTime_h		= travelDistance_meter / this.walkSpeedMeterPerHour;
					// congested car travel times in hours
					double arrivalTime = lcptCongestedCarTravelTime.getTree().get( nodeID ).getTime(); // may also use .getCost() !!!
					double congestedCarTravelTime_h = ((arrivalTime - depatureTime) / 3600.) + offsetCongestedCarTime_h;
					
					sumDisutilityOfTravel(gcs, 
										this.aggregatedOpportunities[i],
										distanceMeasuringPoint2Road_meter + averageDistanceRoad2Opportunitiy_meter,
										distanceRoad2Node_meter, 
										travelDistance_meter,
										offsetWalkTime2Node_h + offsetWalkTime2Opportunity_h,
										freeSpeedTravelTime_h,
										bikeTravelTime_h,
										walkTravelTime_h, 
										congestedCarTravelTime_h);
				}
				
				// aggregated value
				double freeSpeedAccessibility, carAccessibility, bikeAccessibility, walkAccessibility;
				if(!useRawSum){ 	// get log sum
					freeSpeedAccessibility = inverseOfLogitScaleParameter * Math.log( gcs.getFreeSpeedSum() );
					carAccessibility = inverseOfLogitScaleParameter * Math.log( gcs.getCarSum() );
					bikeAccessibility= inverseOfLogitScaleParameter * Math.log( gcs.getBikeSum() );
					walkAccessibility= inverseOfLogitScaleParameter * Math.log( gcs.getWalkSum() );
				}
				else{ 				// get raw sum
					freeSpeedAccessibility = inverseOfLogitScaleParameter * gcs.getFreeSpeedSum();
					carAccessibility = inverseOfLogitScaleParameter * gcs.getCarSum();
					bikeAccessibility= inverseOfLogitScaleParameter * gcs.getBikeSum();
					walkAccessibility= inverseOfLogitScaleParameter * gcs.getWalkSum();
				}
				
				// assign log sums to current starZone object and spatial grid
				freeSpeedGrid.setValue(freeSpeedAccessibility, measurePoint.getGeometry().getCentroid());
				carGrid.setValue(carAccessibility , measurePoint.getGeometry().getCentroid());
				bikeGrid.setValue(bikeAccessibility , measurePoint.getGeometry().getCentroid());
				walkGrid.setValue(walkAccessibility , measurePoint.getGeometry().getCentroid());
				
				// writing accessibility values (stored in startZone object) in csv format ...
				AnalysisCellBasedAccessibilityCSVWriterV2.write(measurePoint,
						coordFromZone, fromNode, freeSpeedAccessibility,
						carAccessibility, bikeAccessibility, walkAccessibility);
			}
			System.out.println();

			if (this.benchmark != null && benchmarkID > 0) {
				this.benchmark.stoppMeasurement(benchmarkID);
				log.info("Accessibility computation with "
						+ measuringPointsCell.getZones().size()
						+ " starting points (origins) and "
						+ this.aggregatedOpportunities.length
						+ " destinations (workplaces) took "
						+ this.benchmark.getDurationInSeconds(benchmarkID)
						+ " seconds ("
						+ this.benchmark.getDurationInSeconds(benchmarkID)
						/ 60. + " minutes).");
			}
			// tnicolai: for debugging (remove for relaease)
			log.info("Euclidian vs Othogonal Distance:");
			log.info("Total Counter:" + NetworkUtil.totalCounter);
			log.info("Euclidian Counter:" + NetworkUtil.euclidianCounter);
			log.info("Othogonal Counter:" + NetworkUtil.othogonalCounter);
			
			AnalysisCellBasedAccessibilityCSVWriterV2.close();
			writePlottingData();
			writeInterpolatedParcelAccessibilities();
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// This needs to be executed only once at the end of the accessibility computation
	// A synchronization is may be not needed
	private void writePlottingData() throws IOException{
		
		log.info("Writing plotting files ...");
		// tnicolai: can be disabled for final release
		GridUtils.writeSpatialGridTable(freeSpeedGrid, InternalConstants.MATSIM_4_OPUS_TEMP	// freespeed results for plotting in R
				+ "freeSpeedAccessibility_cellsize_" + freeSpeedGrid.getResolution()
				+ InternalConstants.FILE_TYPE_TXT);
		// tnicolai: can be disabled for final release
		GridUtils.writeSpatialGridTable(carGrid, InternalConstants.MATSIM_4_OPUS_TEMP	// car results for plotting in R
				+ "carAccessibility_cellsize_" + carGrid.getResolution()
				+ InternalConstants.FILE_TYPE_TXT);
		// tnicolai: can be disabled for final release
		GridUtils.writeSpatialGridTable(bikeGrid, InternalConstants.MATSIM_4_OPUS_TEMP	// car results for plotting in R
				+ "bikeAccessibility_cellsize_" + bikeGrid.getResolution()
				+ InternalConstants.FILE_TYPE_TXT);
		// tnicolai: can be disabled for final release
		GridUtils.writeSpatialGridTable(walkGrid, InternalConstants.MATSIM_4_OPUS_TEMP	// walk results for plotting in R
				+ "walkAccessibility_cellsize_" + walkGrid.getResolution()
				+ InternalConstants.FILE_TYPE_TXT);

		// tnicolai: google earth outputs can be left in final release since
		// this can be used for a quick analysis without any further scripts or
		// so...
		GridUtils.writeKMZFiles(measuringPointsCell,								// car results for google earth
							freeSpeedGrid,
							InternalConstants.MATSIM_4_OPUS_TEMP
										+ "freeSpeedAccessibility_cellsize_"
										+ freeSpeedGrid.getResolution()
										+ InternalConstants.FILE_TYPE_KMZ);
		GridUtils.writeKMZFiles(measuringPointsCell,								// car results for google earth
								carGrid,
								InternalConstants.MATSIM_4_OPUS_TEMP
										+ "carAccessibility_cellsize_"
										+ carGrid.getResolution()
										+ InternalConstants.FILE_TYPE_KMZ);
		GridUtils.writeKMZFiles(measuringPointsCell,								// bike results for google earth
								bikeGrid,
								InternalConstants.MATSIM_4_OPUS_TEMP
										+ "bikeAccessibility_cellsize_"
										+ bikeGrid.getResolution()
										+ InternalConstants.FILE_TYPE_KMZ);
		GridUtils.writeKMZFiles(measuringPointsCell,								// walk results for google earth
								walkGrid,
								InternalConstants.MATSIM_4_OPUS_TEMP
										+ "walkAccessibility_cellsize_"
										+ walkGrid.getResolution()
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
